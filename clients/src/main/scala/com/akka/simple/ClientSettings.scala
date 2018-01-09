package com.akka.simple

import akka.japi.Util.immutableSeq

import com.typesafe.config.{Config, ConfigFactory}

final class ClientSettings(conf: Option[Config] = None) extends Serializable {
  val localAddress: String = "localhost"
  val BasePort = 2551
  val ActorSystemName = "AkkaSeed"

  val rootConfig: Config = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load())
    case _ => ConfigFactory.load
  }

  protected val mySetting: Config = rootConfig.getConfig("AkkaSeed")

  // Application settings
  val AppName: String = mySetting.getString("app-name")

  val HttpHostName: String = mySetting.getString("http.host")
  val HttpListenPort: Int = mySetting.getInt("http.port")

}
