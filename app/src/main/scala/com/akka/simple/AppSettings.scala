package com.akka.simple

import com.typesafe.config.{Config, ConfigFactory}

final class AppSettings(conf: Option[Config] = None) extends Serializable {
  val ActorSystemName = "AkkaSeed"
  val localAddress: String = "localhost"

  val rootConfig: Config = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load())
    case _ => ConfigFactory.load
  }

  protected val appConfig: Config = rootConfig.getConfig("AkkaSeed")

  // Application settings
  val AppName: String = appConfig.getString("app-name")
}
