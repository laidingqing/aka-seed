package com.akka.simple

object SimpleEvent {

  @SerialVersionUID(1L)
  sealed trait SimpleEvent extends Serializable

  sealed trait LifeCycleEvent extends SimpleEvent
  case object OutputStreamInitialized extends LifeCycleEvent
  case object NodeInitialized extends LifeCycleEvent
  case object Start extends LifeCycleEvent
  case object DataFeedStarted extends LifeCycleEvent
  case object Shutdown extends LifeCycleEvent
  case object TaskCompleted extends LifeCycleEvent

  sealed trait Task extends Serializable
  case object QueryTask extends Task
  case object GracefulShutdown extends LifeCycleEvent


  sealed trait WebIngestRequest extends SimpleEvent
  case class InquiryRequest(name: String) extends WebIngestRequest

}
