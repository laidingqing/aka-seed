package com.akka.simple

import java.util.UUID

import scala.concurrent.duration._
import akka.cluster.Cluster
import akka.actor._
import akka.event.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.routing.BalancingPool
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.akka.simple.SimpleEvent.InquiryRequest
import com.akka.simple.cluster.ClusterAwareNodeGuardian
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContextExecutor, Future}

object DataClientApp extends App {
  val settings = new ClientSettings
  import settings._
  val system = ActorSystem(ActorSystemName)

  val guardian = system.actorOf(Props[ApiNodeGuardian], "node-guardian")

  system.registerOnTermination {
    guardian ! PoisonPill
  }
}

final class ApiNodeGuardian extends ClusterAwareNodeGuardian {
  cluster.joinSeedNodes(Vector(cluster.selfAddress))
  cluster registerOnMemberUp {
    context.actorOf(BalancingPool(1).props(Props(new DataApiActor())), "dynamic-data-feed")
    log.info("Started data ingestion on {}.", cluster.selfAddress)
  }
  def initialized: Actor.Receive = {
    case SimpleEvent.TaskCompleted => // ignore for now
  }
}

class DataApiActor extends Actor with ActorLogging {
  val settings = new ClientSettings

  import settings._

  implicit val system: ActorSystem = context.system
  implicit val askTimeout: Timeout = 500.millis
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val guardian = context.actorSelection(Cluster(context.system).selfAddress.copy(port = Some(BasePort)) + "/user/node-guardian")
  val service = new DataApiService(guardian)

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

class DataApiService(guardian: ActorSelection) extends Directives with DefaultJsonProtocol {
  val settings = new ClientSettings
  val log = Logger(this.getClass.getName)
  import scala.concurrent.ExecutionContext.Implicits.global

  def formatResponse(identifier: UUID): String = {
    val requestId = identifier.toString
    s"""{
       |"id":"$requestId"
       |}
          """.stripMargin
  }

  val route =
    get {
      path("") {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><head><title>HTTP data feed</title></head<body><h1>HTTP data feed</h1></body></html>"))
      }
    }~
      get{
        path("test"){
          val identifier = java.util.UUID.randomUUID
          guardian ! InquiryRequest("test")
          complete(HttpEntity(ContentTypes.`application/json`, formatResponse(identifier)))
        }
      }

}