package com.pixipanda.clickstream

import java.io.File


import com.pixipanda.clickstream.cassandra.CassandraConfig
import com.pixipanda.clickstream.kafka.KafkaConfig
import com.pixipanda.clickstream.spark.SparkConfig
import com.typesafe.config.{ConfigFactory, Config}
import org.apache.log4j.Logger

/**
 * Created by kafka on 15/11/18.
 */
object Config {

  val logger = Logger.getLogger(getClass.getName)

  var applicationConf: Config = _

  def parseArgs(args: Array[String]) = {

    if(args.size == 0) {
      defaultSettiing
    } else {
      applicationConf = ConfigFactory.parseFile(new File(args(0))).getConfig("config").resolve()
      logger.info(applicationConf)
      loadConfig()
    }
  }


  def debugSetting() = {


  }

  def loadConfig() = {

    CassandraConfig.load
    KafkaConfig.load
    SparkConfig.load
  }

  def defaultSettiing() = {

    CassandraConfig.defaultSettng()
    KafkaConfig.defaultSetting()
    SparkConfig.defaultSetting()
  }
}
