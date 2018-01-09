package com.akka.simple

import akka.actor.{Actor, ActorRef, Props}
import com.akka.simple.SimpleEvent.{GracefulShutdown, InquiryRequest, WebIngestRequest}
import com.akka.simple.actor.{ActorBase, WebIngestActor}
import com.akka.simple.cluster.ClusterAwareNodeGuardian

class NodeGuardian(settings: AppSettings) extends ClusterAwareNodeGuardian with ActorBase {

  val webIngestActor: ActorRef = context.actorOf(Props(new WebIngestActor()), "web-ingest")
  override def preStart(): Unit = {
    super.preStart()
    cluster.joinSeedNodes(Vector(cluster.selfAddress))
  }

  override def initialize(): Unit = {
    super.initialize()
    context become initialized
  }

  def initialized: Actor.Receive = {
    case e: WebIngestRequest => webIngestActor forward e
    case GracefulShutdown => {
      log.info("Perform graceful shutdown")
      gracefulShutdown(sender())
    }
  }
}
