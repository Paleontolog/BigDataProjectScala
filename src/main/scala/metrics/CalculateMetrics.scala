package metrics

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream



object CalculateMetrics {
  case class Popularity(tag: String, amount: Int)



  private[metrics] def extractTrendingTags(input: RDD[String]): RDD[Popularity] =
      input.flatMap(_.split("\\s+"))
        .filter(_.startsWith("#"))

      .map(_.replaceAll("[,.!?:;]", "").toLowerCase)

      .map(_.replaceFirst("^#", ""))
      .filter(!_.isEmpty)
      .map((_, 1))
      .reduceByKey(_ + _)
      .map(r => Popularity(r._1, r._2))
      // Sort hashtags by descending number of occurrences

  // [END extract]


  def processTrendingHashTags(input: DStream[String],
                              windowLength: Int,
                              slidingInterval: Int,
                              n: Int,
                              handler: Array[Popularity] => Unit): Unit = {
    val sortedHashtags: DStream[Popularity] = input
      .window(Seconds(windowLength), Seconds(slidingInterval)) //create a window
      .transform(extractTrendingTags(_)) //apply transformation

    sortedHashtags.foreachRDD(rdd => {
      handler(rdd.take(n)) //take top N hashtags and save to external source
    })
  }

}
