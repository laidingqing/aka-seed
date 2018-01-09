package com.akka.simple

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.actor.{ActorSystem, Address, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.cluster.Cluster
import com.akka.simple.SimpleEvent.GracefulShutdown

import scala.concurrent.{Await, Future}

object SeedApp extends App {

  val settings = new AppSettings
  import settings._

  val system = ActorSystem(ActorSystemName)

  val mySeedApp = MySeedApp(system)
}

object MySeedApp extends ExtensionId[MySeedApp] with ExtensionIdProvider {
  override def lookup: ExtensionId[_ <: Extension] = MySeedApp
  override def createExtension(system: ExtendedActorSystem) = new MySeedApp(system)
}

class MySeedApp(system: ExtendedActorSystem) extends Extension {
  import system.dispatcher
  val settings = new AppSettings
  val nodeGuardianActorName = "node-guardian"
  system.registerOnTermination(shutdown())

  protected val log = akka.event.Logging(system, system.name)
  protected val running = new AtomicBoolean(false)
  protected val terminated = new AtomicBoolean(false)
  implicit private val timeout = system.settings.CreationTimeout

  private val guardian = system.actorOf(Props(new NodeGuardian(settings)), nodeGuardianActorName)

  private val cluster = Cluster(system)

  val selfAddress: Address = cluster.selfAddress

  cluster.joinSeedNodes(Vector(selfAddress))

  def isRunning: Boolean = running.get

  def isTerminated: Boolean = terminated.get

  private def shutdown(): Unit = if (!isTerminated) {
    import akka.pattern.ask

    if (terminated.compareAndSet(false, true)) {
      log.info("Node {} shutting down", selfAddress)
      cluster leave selfAddress
      (guardian ? GracefulShutdown).mapTo[Future[Boolean]]
        .onComplete { _ =>
          system.terminate()
          Await.ready(system.whenTerminated, timeout.duration)
        }
    }
  }
}