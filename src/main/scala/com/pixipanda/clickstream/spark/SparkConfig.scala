package com.pixipanda.clickstream.spark


import com.pixipanda.clickstream.Config
import com.pixipanda.clickstream.cassandra.CassandraConfig
import org.apache.log4j.Logger
import org.apache.spark.SparkConf

/**
 * Created by kafka on 7/12/18.
 */
object SparkConfig {

  val logger = Logger.getLogger(getClass.getName)

  val sparkConf = new SparkConf

  var batchInterval:Int = _
  var ipLookupFile:String = _

  def load() = {
    logger.info("Loading Spark Setttings")
    sparkConf.set("spark.cassandra.connection.host", Config.applicationConf.getString("cassandra.hostname"))
      .set("spark.streaming.kafka.maxRatePerPartition", Config.applicationConf.getString("spark.max.rate.per.partition"))
    batchInterval = Config.applicationConf.getString("spark.batch.interval").toInt
    ipLookupFile = Config.applicationConf.getString("spark.ip.lookup.file")
  }

  def defaultSetting() = {
    sparkConf.setMaster("local[*]")
      .setAppName("Real-time EcommerceLogProcessing")
      .set("spark.cassandra.connection.host", CassandraConfig.cassandrHost)
    batchInterval = 5000
    ipLookupFile = "src/main/resources/all_classbs.txt"

  }
}
