package metrics

import entities.DataObject
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.BooleanType
import org.apache.spark.streaming.Time

import scala.util.matching.Regex


object CalculateMetrics {

  val pattern = new Regex(".*::(Unknown|Stolen)(\\|{2}|$)")


  //  val pathToSchema: String = getClass.getResource("/database/schema.json").getPath
  //  val file: String = FileUtils.readFileToString(new File(pathToSchema), StandardCharsets.UTF_8)
  //  val schemaFromJson: StructType = DataType.fromJson(file).asInstanceOf[StructType]


  //  private[metrics] def calculateDataset(rdd: RDD[DataObject], context: SparkSession) : Unit = {
  //    val dataset = context.createDataset(rdd)(Encoders.product[DataObject])
  //  }
  //
  //  private[metrics] def calculateDataframeFromSchema(rdd: RDD[String], context: SparkSession): Unit = {
  //    val dataframe = context.read.schema(schemaFromJson).json(rdd)
  //    dataframe.show()
  //  }


  private[metrics] def countDataFrame(dataframe: DataFrame, field: String): DataFrame = {
    dataframe
      .groupBy(field)
      .count()
      .sort(desc("count"), asc(field))
  }

  private[metrics] def countIfContains(dataframe: DataFrame, field: String): DataFrame = {
    val matcher = udf((s: String) => pattern.findAllMatchIn(s).nonEmpty, BooleanType)

    dataframe
      .where(col(field).isNotNull)
      .filter(matcher(col(field)))
      .agg(count("incident_id")
        .alias("count"))
  }


  private[metrics] def saveTopN(dataframe: DataFrame, time: Time, n: Int,
                                fieldName: String,
                                saveMetrics: (DataFrame, String, Time) => Unit): Unit = {
    val topOnBatch = dataframe.limit(n)
    saveMetrics(topOnBatch, fieldName, time)
  }


  private[metrics] def calculateDataFrame(time: Time,
                                          rdd: RDD[DataObject],
                                          context: SparkSession,
                                          saveOnDrive: (Time, DataFrame) => Unit,
                                          saveMetrics: (DataFrame, String, Time) => Unit): Unit = {

    val dataFrame = context.createDataFrame(rdd)
    dataFrame.show()

    saveOnDrive(time, dataFrame)

    val states = countDataFrame(dataFrame, "state")
    saveTopN(states, time, 10, "state", saveMetrics)

    val cityOrCounty = countDataFrame(dataFrame, "city_or_county")
    saveTopN(cityOrCounty, time, 10, "city_or_county", saveMetrics)

    val gun_stolen = countIfContains(dataFrame, "gun_stolen")
    saveTopN(gun_stolen, time, 1, "gun_stolen", saveMetrics)
  }


  def calculateAllMetrics(input: DStream[DataObject],
                          context: SparkSession,
                          n: Int,
                          saveOnDrive: (Time, DataFrame) => Unit,
                          saveMetrics: (DataFrame, String, Time) => Unit): Unit = {

    input.foreachRDD((rdd: RDD[DataObject], time: Time) =>
      calculateDataFrame(time, rdd, context, saveOnDrive, saveMetrics))
  }
}
