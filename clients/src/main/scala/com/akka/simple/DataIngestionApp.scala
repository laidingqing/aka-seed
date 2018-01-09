package com.akka.simple

import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.event.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.remote.serialization.StringSerializer
import akka.routing.BalancingPool
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.akka.simple._
import com.akka.simple.cluster.ClusterAwareNodeGuardian
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

object DataIngestionApp extends App {
  val settings = new ClientSettings
  import settings._
  val system = ActorSystem(AppName)

  val guardian = system.actorOf(Props[HttpNodeGuardian], "node-guardian")

  system.registerOnTermination {
    guardian ! PoisonPill
  }
}


final class HttpNodeGuardian extends ClusterAwareNodeGuardian {
  val actorName = "kafka-ingestion-router"
  val settings = new ClientSettings

  import settings._

  cluster.joinSeedNodes(Vector(cluster.selfAddress))

//  val router: ActorRef = context.actorOf(BalancingPool(5).props(Props(new KafkaPublisherActor(KafkaHosts, KafkaBatchSendSize))), actorName)

  cluster registerOnMemberUp {
    context.actorOf(BalancingPool(1).props(Props(new HttpDataFeedActor())), "dynamic-data-feed")
    log.info("Started data ingestion on {}.", cluster.selfAddress)
  }

  def initialized: Actor.Receive = {
    case SimpleEvent.TaskCompleted => // ignore for now
  }

}

class KafkaPublisherActor(val config: Properties) extends KafkaDataProducerActor[String, String] {
  def this(hosts: Set[String], batchSize: Int) = this(KafkaDataProducer.createConfig(hosts, batchSize, classOf[StringSerializer].getName))
}

class HttpDataFeedActor(/**kafka: ActorRef**/) extends Actor with ActorLogging {
  val settings = new ClientSettings

  import settings._

  implicit val system: ActorSystem = context.system
  implicit val askTimeout: Timeout = 500.millis
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val service = new HttpDataFeedService()

  val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(service.route, HttpHostName, HttpListenPort)

  override def postStop: Unit = {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def receive: Actor.Receive = {
    case e =>
  }
}

class HttpDataFeedService() extends Directives with DefaultJsonProtocol {
  val settings = new ClientSettings
  val log = Logger(this.getClass.getName)
  import scala.concurrent.ExecutionContext.Implicits.global



  val route =
    get {
      path("") {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><head><title>HTTP data feed</title></head<body><h1>HTTP data feed</h1></body></html>"))
      }
    }

}