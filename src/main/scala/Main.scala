
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.json4s.jackson.JsonMethods._
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s._
import org.json4s.jackson.Json4sScalaModule

object Main {


  def createContext(projectID: String, windowLength: String, slidingInterval: String, checkpointDirectory: String)
  : StreamingContext = {

    val conf = new SparkConf().setAppName("sparktest").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(slidingInterval.toInt))

    val lines: DStream[Map[String, Any]] =
         ssc.textFileStream("D:\\PycharmProjects\\BigDataProject\\emulation")
            .map(_.replaceAll("NaN", "null"))
            .map(parse(_).values.asInstanceOf[Map[String, Any]])


    lines.print()
    //
    //    val messagesStream: DStream[String] = PubsubUtils
    //      .createStream(
    //        ssc,
    //        projectID,
    //        None,
    //        "tweets-subscription",
    //        SparkGCPCredentials.builder.build(), StorageLevel.MEMORY_AND_DISK_SER_2)
    //      .map(message => new String(message.getData(), StandardCharsets.UTF_8))

    //    processTrendingHashTags(messagesStream,
    //      windowLength.toInt,
    //      slidingInterval.toInt,
    //      10,
    //      //decoupled handler that saves each separate result for processed to datastore
    //      saveRDDtoDataStore(_, windowLength.toInt)
    //    )

    ssc
  }

  def main(args: Array[String]): Unit = {

    val Seq(projectID, windowLength, slidingInterval, totalRunningTime, checkpointDirectory) =
      Seq("test", "10", "10", "100", "D:\\JAVA\\BigData\\src\\main\\resources\\checkpoint")


    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => createContext(projectID, windowLength, slidingInterval, checkpointDirectory))

    ssc.start()
    ssc.awaitTermination()
  }
}