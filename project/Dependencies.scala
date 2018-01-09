import sbt._
import sbt.Keys._

object Dependencies {

  import Versions._

  implicit class Exclude(module: ModuleID) {
    def sparkExclusions: ModuleID =
      module
        .exclude("com.google.guava", "guava")
  }

  object Compile {
    val akkaActor           = "com.typesafe.akka" %% "akka-actor"           % AkkaVersion
    val akkaHttp            = "com.typesafe.akka" %% "akka-http"            % AkkaVersion
    val akkaHttpJson        = "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
    val akkaSlf4j           = "com.typesafe.akka" %% "akka-slf4j"           % AkkaVersion
    val akkaCluster         = "com.typesafe.akka" %% "akka-cluster"         % AkkaVersion
    val akkaClusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics" % AkkaVersion
    val akkaRemote          = "com.typesafe.akka" %% "akka-remote"          % AkkaVersion
    val logback             = "ch.qos.logback"    %  "logback-classic"      % "1.2.3"
    val jodaTime            = "joda-time" % "joda-time" % JodaTimeVersion   % "compile;runtime"


    val sparkCassandraConnector = "com.datastax.spark" %% "spark-cassandra-connector" % SparkCassandra
    val sparkCore = "org.apache.spark" %% "spark-core" % Spark sparkExclusions
    val sparkSql = "org.apache.spark" %% "spark-sql" % Spark sparkExclusions
    val sparkStreaming = "org.apache.spark" %% "spark-streaming" % Spark sparkExclusions
    val sparkStreamingKafka = "org.apache.spark" %% "spark-streaming-kafka-0-10" % Spark sparkExclusions
    val sparkGraphx = "org.apache.spark" %% "spark-graphx" % Spark sparkExclusions


  }

  object Test {
    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test"
    val scalatest = "org.scalatest" %% "scalatest" % ScalaTestVersion % "test"
    val scalactic = "org.scalactic" %% "scalactic" % ScalaTestVersion % "test"
    val supersafe = "com.artima.supersafe" % "supersafe_2.11.8" % SuperSafe % "test"
    val log4j = "log4j" % "log4j" % Log4j % "test"
  }

  import Compile._

  val spark = Seq(sparkCassandraConnector, sparkCore, sparkSql, sparkStreaming, sparkStreamingKafka, sparkGraphx)
  val akka = Seq(akkaActor, akkaActor, akkaRemote, akkaHttpJson, akkaCluster, akkaClusterMetrics)
  val log = Seq(akkaSlf4j, logback)
  val time = Seq(jodaTime)

  val test = Seq(Test.akkaTestKit, Test.scalatest, Test.scalactic, Test.supersafe, Test.log4j)

  val coreDeps = spark ++ akka ++ log ++ time
  val clientDeps = Seq()
  val appDeps = Seq()
}
