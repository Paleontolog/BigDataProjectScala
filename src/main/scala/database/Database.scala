package database

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode, SparkSession}
import org.apache.spark.streaming.Time
import org.apache.spark.sql.functions.lit
import utils.Utils._



object Database {

  val datasetName: String = ConfigFactory.load().getString("database.datasetName")

  def saveMetricsInDatabase(dataFrame: DataFrame,
                            tableName: String,
                            time: Time): Unit = {

    val allTime = fullDateFromMilliseconds(time.milliseconds)
    val resultDataFrame = dataFrame.withColumn("time", lit(allTime))

    //    resultDataFrame.write.format("bigquery")
    //      .option("table", s"$datasetName.$tableName")
    //      .mode(SaveMode.Append)
    //      .save()
  }

  def saveMetricsInDatabaseGlobal(dataFrame: DataFrame,
                                  tableName: String,
                                  time: Time,
                                  sqlContext: SQLContext): Unit = {



    //makebigQueryContext(sql = sqlContext).runDMLQuery("")
    // Load results from a SQL query
  }
}
