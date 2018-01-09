package com.akka.simple.cluster

import java.util.concurrent.TimeoutException

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import akka.actor._
import akka.util.Timeout
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import akka.cluster.metrics.{ClusterMetricsChanged, Metric}
import com.akka.simple.SimpleEvent.{NodeInitialized, OutputStreamInitialized}

abstract class ClusterAware extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart.
  override def preStart(): Unit = cluster.subscribe(self, classOf[ClusterDomainEvent])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Actor.Receive = {
    case MemberUp(member) => watch(member)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case ClusterMetricsChanged(forNode) =>
      forNode collectFirst { case m if m.address == cluster.selfAddress =>
        log.debug("{}", filter(m.metrics))
      }
    case _: MemberEvent =>
  }

  // Initiated when node receives a [[ClusterEvent.MemberUp]].
  private def watch(member: Member): Unit = {
    log.debug("Member [{}] joined cluster.", member.address)
  }

  def filter(nodeMetrics: Set[Metric]): String = {
    val filtered = nodeMetrics collect { case v if v.name != "processors" => s"${v.name}:${v.value}" }
    s"NodeMetrics[${filtered.mkString(",")}]"
  }
}

abstract class ClusterAwareNodeGuardian extends ClusterAware {

  import SupervisorStrategy._
  import com.akka.simple.SimpleEvent.NodeInitialized
  import akka.pattern.gracefulStop
  import context.dispatcher

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ActorInitializationException => Stop
      case _: IllegalArgumentException => Stop
      case _: IllegalStateException => Restart
      case _: TimeoutException => Escalate
      case _: Exception => Escalate
    }

  override def preStart(): Unit = {
    super.preStart()
    log.info("Starting at {}", cluster.selfAddress)
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("Node {} shutting down.", cluster.selfAddress)
  }

  override def receive = uninitialized orElse initialized orElse super.receive

  def uninitialized: Actor.Receive = {
    case OutputStreamInitialized => initialize()
  }

  def initialize(): Unit = {
    log.info(s"Node is transitioning from 'uninitialized' to 'initialized'")
    context.system.eventStream.publish(NodeInitialized)
  }

  def initialized: Actor.Receive

  protected def gracefulShutdown(listener: ActorRef): Unit = {
    implicit val timeout = Timeout(5.seconds)
    val status = Future.sequence(context.children.map(shutdown))
    listener ! status
    log.info(s"Graceful shutdown completed.")
  }

  private def shutdown(child: ActorRef)(implicit t: Timeout): Future[Boolean] =
    try gracefulStop(child, t.duration + 1.seconds) catch {
      case NonFatal(e) =>
        log.error("Error shutting down {}, cause {}", child.path, e.toString)
        Future(false)
    }
}