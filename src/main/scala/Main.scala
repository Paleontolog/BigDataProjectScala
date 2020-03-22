
import com.evolutiongaming.cluster.pubsub.LocalPubSub
import com.typesafe.config.ConfigFactory
import entities.DataObject
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.json4s._
import org.json4s.native.JsonMethods._


//Для Goolge Cloud: Pub/Sub - queue, Dataproc для запуска Spark приложения,
// база BigQuery, хранилище Google Storage.


object Main {


  def parseJson(jsonString: String) = {
    implicit val formats: DefaultFormats.type = DefaultFormats
    parse(jsonString, useBigIntForLong = true,
      useBigDecimalForDouble = true).extract[DataObject]
  }

  def createContext(): StreamingContext = {

    val appName = ConfigFactory.load().getString("application.name")
    val batchDuration = ConfigFactory.load().getString("application.batchDuration")
    val windowLength = ConfigFactory.load().getString("application.windowLength").toInt
    val slidingInterval = ConfigFactory.load().getString("application.slidingInterval").toInt
    val tempBucket = ConfigFactory.load().getString("database.tempBucket")
    val projectID = ConfigFactory.load().getString("project.id")


    val conf = new SparkConf().setAppName(appName).setMaster("local[*]")
    val sc = new SparkContext(conf)
    val ssc: StreamingContext =
      new StreamingContext(sc, Seconds(batchDuration.toInt))
    val spark = SparkSession.builder
      .config(conf)
      .getOrCreate()


    //spark.conf.set("credentialsFile", getClass.getResource("/keys/keyJson.json").getPath)
    //spark.conf.set("temporaryGcsBucket", tempBucket)

    //
    val lines =
      ssc.textFileStream("D:\\PycharmProjects\\BigDataProject\\emulation")
        //.window(Seconds(windowLength), Seconds(slidingInterval))
        .map(_.replaceAll("NaN", "null"))
        .map(parseJson(_))

    lines.print()


//
//    val lines: DStream[String] = PubsubUtils
//      .createStream(
//        ssc,
//        projectID,
//        Option("testTopic"),
//        "tweets-subscription",
//        SparkGCPCredentials.builder.build(), StorageLevel.MEMORY_AND_DISK_SER_2)
//      .map(message => new String(message.getData(), StandardCharsets.UTF_8))

//    lines.print()

    //    CalculateMetrics.calculateAllMetrics(lines,
    //      spark,
    //      10,
    //      saveInDataStore,
    //      saveMetricsInDatabase
    //    )

    ssc
  }


  def main(args: Array[String]): Unit = {

    val checkpointDirectory = "D:\\JAVA\\BigData\\src\\main\\resources\\checkpoint"

    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => createContext())


    ssc.start()
    ssc.awaitTermination()
  }
}