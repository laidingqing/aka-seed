package com.akka.simple.actor

import akka.actor.{Actor, ActorLogging}
import com.akka.simple.SimpleEvent.{InquiryRequest, WebIngestRequest}

class WebIngestActor extends ActorBase with ActorLogging{
  def receive: Actor.Receive = {
    case e: InquiryRequest => log.info("receive web ingest request {}", e.name)
  }
}
