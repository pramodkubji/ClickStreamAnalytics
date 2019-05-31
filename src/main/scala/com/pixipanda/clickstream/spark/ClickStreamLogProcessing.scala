package com.pixipanda.clickstream.spark


import com.datastax.spark.connector.cql.CassandraConnector
import com.pixipanda.avro.ClickStream
import com.pixipanda.clickstream.Config
import com.pixipanda.clickstream.cassandra.{CassandraUtils, CassandraConfig}
import com.pixipanda.clickstream.kafka.KafkaConfig


import org.apache.log4j.Logger


import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, CanCommitOffsets, KafkaUtils}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies._



/**
 * Created by kafka on 16/11/18.
 */
object ClickStreamLogProcessing {


  val logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]) {

    Config.parseArgs(args)

    val sparkSession =  SparkSession.builder
      .config(SparkConfig.sparkConf)
      .getOrCreate()

    val ssc = new StreamingContext(sparkSession.sparkContext, Duration(SparkConfig.batchInterval))


    val topics = Set(KafkaConfig.kafkaParams("topic"))

    /* Here we are connecting to Schema Registry through REST and getting latest schema for the given topic */
/*
    val ecommerceSchemaString = SparkUtils.getSchema(KafkaConfig.kafkaParams("schema.registry"), KafkaConfig.kafkaParams("topic"))
*/


    val countryCodeBroadcast = sparkSession.sparkContext.broadcast(SparkUtils.createCountryCodeMap(SparkConfig.ipLookupFile))

    /*
       Connector Object is created in driver. It is serializable.
       So once the executor get it, they establish the real connection
    */

    val connector = CassandraConnector(sparkSession.sparkContext.getConf)

    val kafkaParams = Map[String, String](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> KafkaConfig.kafkaParams("bootstrap.servers"),
      ConsumerConfig.GROUP_ID_CONFIG -> KafkaConfig.kafkaParams("group.id"),
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> KafkaConfig.kafkaParams("key.deserializer"),
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->KafkaConfig.kafkaParams("value.deserializer"),
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> KafkaConfig.kafkaParams("enable.auto.commit"),
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> KafkaConfig.kafkaParams("auto.offset.reset"),
      "schema.registry.url" -> "http://localhost:8081",
      "specific.avro.reader" -> "true")

    logger.info("Connecting to Kafka")
    val kafkaDstream = KafkaUtils.createDirectStream[String, ClickStream](ssc,
                       PreferConsistent,
                       Subscribe[String, ClickStream](topics, kafkaParams)
                       )


  /*  kafkaDstream.foreachRDD(rdd => {

      if(rdd.isEmpty()) {
        logger.info("Did not receive any data")
      }

      /* Extract Offset */
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      offsetRanges.foreach(offRange => {
        logger.info("fromOffset: " + offRange.fromOffset + "untill Offset: " + offRange.untilOffset)
      })

      rdd.map(cr => cr.value).foreach(record => {
        println("ip: " + record.getIpv4 + " requestUri: " + record.getRequestUri)
      })

    })*/

    kafkaDstream.foreachRDD(rdd => {

      if(rdd.isEmpty()) {
        logger.info("Did not receive any data")
      }

      /* Extract Offset */
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      offsetRanges.foreach(offRange => {
        logger.info("fromOffset: " + offRange.fromOffset + "untill Offset: " + offRange.untilOffset)
      })

      val clickStreamRdd = rdd.map(_.value())

      //Kafka sends Array of bytes to Spark Streaming consumer. Here we are converting Array of bytes to Avro Generic Record
      //val genericAvroRdd = SparkUtils.bytesToAvroGenriceRecord(ecommerceValueRdd, ecommerceSchemaString)

      //Page views analytics
      val pageViewsRdd = SparkUtils.getPageViews(clickStreamRdd)
      CassandraUtils.updateToCassandra(pageViewsRdd, connector, CassandraConfig.keyspace, CassandraConfig.pageViewsTable, "page")

      //Response Code Analytics
      val statusCodeCountRdd = SparkUtils.getStatusCount(clickStreamRdd)
      CassandraUtils.updateToCassandra(statusCodeCountRdd, connector, CassandraConfig.keyspace, CassandraConfig.statusCounterTable, "status_code")

       //Referrer Analytics
      val referrerRdd = SparkUtils.getReferrer(clickStreamRdd)
      CassandraUtils.updateToCassandra(referrerRdd, connector, CassandraConfig.keyspace, CassandraConfig.referrerCounterTable, "referrer")

       //Country Visit Analytics
      val countryVisitRdd = SparkUtils.getVisitsByCounrty(clickStreamRdd, countryCodeBroadcast)
      CassandraUtils.updateToCassandra(countryVisitRdd, connector, CassandraConfig.keyspace, CassandraConfig.visitsByCountryTable, "country")

       //After all processing is done, offset is committed to Kafka
      kafkaDstream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)

    })

    ssc.start()
    ssc.awaitTermination()

  }

}
