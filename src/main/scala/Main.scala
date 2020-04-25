
import java.nio.charset.StandardCharsets

import com.typesafe.config.ConfigFactory
import entities.DataObject
import metrics.CalculateMetrics
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.pubsub.{PubsubUtils, SparkGCPCredentials}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.json4s._
import org.json4s.native.JsonMethods._
import database.DataStore._
import database.Database
import database.Database._
import org.apache.spark.internal.Logging
import utils.PubSubReceiver


object Main {

  val tempBucket: String = ConfigFactory.load().getString("database.tempBucket")
  val appName: String = ConfigFactory.load().getString("application.name")
  val batchDuration: String = ConfigFactory.load().getString("application.batchDuration")
  val windowLength: Int = ConfigFactory.load().getString("application.windowLength").toInt
  val slidingInterval: Int = ConfigFactory.load().getString("application.slidingInterval").toInt
  val projectID: String = ConfigFactory.load().getString("project.id")
  val subscription: String = ConfigFactory.load().getString("pubsub.subscription")
  val topic: String = ConfigFactory.load().getString("pubsub.topic")
  val checkpointDirectory: String = ConfigFactory.load().getString("project.checkpointDir")


  def parseJson(jsonString: String): DataObject = {
    implicit val formats: DefaultFormats.type = DefaultFormats
    parse(jsonString, useBigIntForLong = true,
      useBigDecimalForDouble = true).extract[DataObject]
  }

  def fileStream(ssc: StreamingContext, folderName: String): DStream[DataObject] = {
    val lines: DStream[DataObject] =
      ssc.textFileStream(folderName)
        //.window(Seconds(windowLength), Seconds(slidingInterval))
        .map(_.replaceAll("NaN", "null"))
        .map(parseJson)
    lines
  }


  def customReceiver(ssc: StreamingContext, host: String, port: Int): DStream[DataObject] = {
    //"localhost", 8085
    val pubSubTest = new PubSubReceiver(host, port)

    val lines: DStream[DataObject] =
      ssc.receiverStream(pubSubTest)
        .map(_.replaceAll("NaN", "null"))
        .map(parseJson)

    lines
  }

  def createContext(conf: SparkConf): Seq[Logging] = {

    val sc = new SparkContext(conf)
    val spark = SparkSession.builder
      .config(conf)
      .getOrCreate()

    spark.conf.set("temporaryGcsBucket", tempBucket)
    spark.conf.set("configuration.query.defaultDataset.projectId", projectID)
    spark.conf.set("configuration.query.defaultDataset.datasetId", "gunDataset")

    Seq(sc, spark)
  }


  def createStreamingContext(sparkConf: SparkConf, argument: String): StreamingContext = {

    val Seq(sc: SparkContext, spark: SparkSession) = createContext(sparkConf)
    val ssc: StreamingContext = new StreamingContext(sc, Seconds(batchDuration.toInt))

    //val lines: DStream[DataObject] = customReceiver(ssc, "localhost", 8085)

    val lines: DStream[DataObject] = PubsubUtils
      .createStream(
        ssc,
        projectID,
        Option(topic),
        subscription,
        SparkGCPCredentials.builder.build(), StorageLevel.MEMORY_AND_DISK_SER_2)
      .map(message => new String(message.getData(), StandardCharsets.UTF_8))
      .map(_.replaceAll("NaN", "null"))
      .map(parseJson)
    // lines.print()

    CalculateMetrics.calculateAllMetrics(lines,
      spark,
      argument,
      10,
      saveInDataStore,
      saveMetricsInDatabase
    )

    ssc
  }

  def main(args: Array[String]): Unit = {

    val conf: SparkConf = new SparkConf().setAppName(appName)
   // .setMaster("yarn")
    //.set("spark.cores.max", "4")
    .setMaster("local[*]")



    val Seq(argument, mode) = Seq(args(0), args(1))

    if (argument == "calculate") {

      if (mode == "all") {
        Database.initDatabase()
      }

      val ssc = StreamingContext.getOrCreate(checkpointDirectory,
        () => createStreamingContext(conf, mode))

      ssc.start()
      ssc.awaitTermination()

    } else if (argument == "show") {
      val Seq(_, spark: SparkSession) = createContext(conf)

      if (mode == "all") {
        Database.calcResultMetricsAll(spark)
      } else if (mode == "batch") {
        Database.calcResultMetricsBatch(spark)
      }
    }
  }
}
