package com.pixipanda.clickstream.kafka

import com.pixipanda.clickstream.Config
import org.apache.log4j.Logger

import scala.collection.mutable.Map

/**
 * Created by kafka on 7/12/18.
 */
object KafkaConfig {

  val logger = Logger.getLogger(getClass.getName)

  val kafkaParams: Map[String, String] = Map.empty

  /*Configuration setting are loaded from application.conf when you run Spark Standalone cluster*/
  def load() = {
    logger.info("Loading Kafka Setttings")
    kafkaParams.put("bootstrap.servers", Config.applicationConf.getString("kafka.bootstrap.servers"))
    kafkaParams.put("topic", Config.applicationConf.getString("kafka.topic"))
    kafkaParams.put("group.id", Config.applicationConf.getString("kafka.group.id"))
    kafkaParams.put("key.deserializer", Config.applicationConf.getString("kafka.key.deserializer"))
    kafkaParams.put("value.deserializer", Config.applicationConf.getString("kafka.value.deserializer"))
    kafkaParams.put("schema.registry", Config.applicationConf.getString("kafka.schema.registry"))
    kafkaParams.put("enable.auto.commit", Config.applicationConf.getString("kafka.enable.auto.commit"))
    kafkaParams.put("auto.offset.reset", Config.applicationConf.getString("kafka.auto.offset.reset"))
    kafkaParams.put("specific.avro.reader", Config.applicationConf.getString("specific.avro.reader"))

  }

  /* Default Settings will be used when you run the project from Intellij */
  def defaultSetting() = {

    kafkaParams.put("bootstrap.servers", "localhost:9092")
    kafkaParams.put("topic", "clickStream")
    kafkaParams.put("group.id", "Real-time EcommerceLogProcessing")
    kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaParams.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer")
    kafkaParams.put("schema.registry", "http://localhost:8081")
    kafkaParams.put("enable.auto.commit", "false")
    kafkaParams.put("auto.offset.reset", "earliest")
    kafkaParams.put("specific.avro.reader", "true")
  }
}
