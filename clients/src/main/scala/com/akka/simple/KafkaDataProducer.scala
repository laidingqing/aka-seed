package com.akka.simple

import java.util.Properties

import akka.actor.{Actor, ActorLogging}
import akka.event.slf4j.Logger
import kafka.server.KafkaConfig
import org.apache.kafka.clients.producer._
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer


abstract class KafkaDataProducerActor[K, V] extends Actor with ActorLogging {

  import KafkaEvent._

  def config: Properties

  private val producer = new KafkaDataProducer[K, V](config)

  override def postStop(): Unit = {
    log.info("Shutting down producer.")
    producer.close()
  }

  def receive: Actor.Receive = {
    case e: KafkaMessageEnvelope[K, V] => producer.send(e)
  }
}

class KafkaDataProducer[K, V](config: Properties) {

  val log = Logger(this.getClass.getName)

  def this(brokers: Set[String], batchSize: Int, serializerFqcn: String) =
    this(KafkaDataProducer.createConfig(brokers, batchSize, serializerFqcn))

  def this(config: KafkaConfig) =
    this(KafkaDataProducer.defaultConfig(config))

  import KafkaEvent._

  private val producer = new KafkaProducer[K, V](config)

  // Sends the data, partitioned by key to the topic.
  def send(e: KafkaMessageEnvelope[K, V]): Unit =
    send(e.identifier, e.topic, e.key, e.message)

  // Sends a single message.
  def send(identifier: String, topic: String, key: K, message: V): Unit = {
    producer.send(new ProducerRecord[K, V](topic, key, message), new Callback() {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {

      }
    }
    )
  }

  def close(): Unit = producer.close()

}

object KafkaEvent {
  case class KafkaMessageEnvelope[K, V](identifier: String, topic: String, key: K, message: V)
}

object KafkaDataProducer {

  def createConfig(brokers: Set[String], batchSize: Int, serializerFqcn: String): Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers.mkString(","))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializerFqcn)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializerFqcn)
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize.toString)
    props
  }

  def defaultConfig(config: KafkaConfig): Properties =
    createConfig(Set(s"${config.hostName}:${config.port}"), 100, classOf[StringSerializer].getName)
}