package database
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.streaming.Time
import org.apache.spark.sql.functions.lit
import utils.Utils._
import com.samelamin.spark.bigquery._


object Database {

  val datasetName: String = ConfigFactory.load().getString("database.datasetName")

  def saveMetricsInDatabase(dataFrame: DataFrame,
                            tableName: String,
                            time: Time): Unit = {

    val allTime = fullDateFromMilliseconds(time.milliseconds)
    val resultDataFrame = dataFrame.withColumn("time", lit(allTime))

    resultDataFrame.write.format("bigquery")
      .option("table", s"$datasetName.$tableName")
      .mode(SaveMode.Append)
      .save()
  }

  def saveMetricsInDatabaseGlobal(): Unit = {

    // Load results from a SQL query
    sqlContext.runDMLQuery("UPDATE dataset-id.table-name SET test_col = new_value WHERE test_col = old_value")
  }

}
