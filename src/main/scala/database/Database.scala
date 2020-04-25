package database

import com.google.cloud.bigquery.{BigQueryOptions, JobId, JobInfo, QueryJobConfiguration}
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.functions.{desc, lit, sum}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.streaming.Time
import utils.Utils._

object Database {

  val datasetName: String = ConfigFactory.load().getString("database.datasetName")

  def saveMetricsInDatabase(dataFrame: DataFrame,
                            tableName: String,
                            n: Int,
                            time: Time,
                            sparkSession: SparkSession,
                            argument: String): Unit = {
    if (argument == "batch") {
      saveMetricsInDatabaseBatch(dataFrame.limit(n), tableName, time)
    } else if (argument == "all") {
      saveMetricsInDatabaseGlobal(dataFrame, tableName, time, sparkSession)
    }
  }


  def saveMetricsInDatabaseBatch(dataFrame: DataFrame,
                                 tableName: String,
                                 time: Time): Unit = {

    val allTime = fullDateFromMilliseconds(time.milliseconds)
    val resultDataFrame = dataFrame.withColumn("time", lit(allTime))
    //resultDataFrame.show()
    resultDataFrame.write.format("bigquery")
      .option("table", s"$datasetName.${tableName}_batch")
      .mode(SaveMode.Append)
      .save()
  }

  //
  //  def selectMinOrMax(param: String, n: Int): String = {
  //    s"SELECT c.table_id " +
  //      " FROM tables c " +
  //      " where c.creation_time in " +
  //      " (select creation_time from tables " +
  //      s" order by creation_time $param limit $n) "
  //  }

  def executeQuery(query: String): Unit = {
    val bigquery = BigQueryOptions.getDefaultInstance.getService
    val queryConfig = QueryJobConfiguration
      .newBuilder(query)
      .build
    bigquery.query(queryConfig)
  }


  def executeQueryBatch(query: String): Unit = {
    val bigquery = BigQueryOptions.getDefaultInstance.getService
    val queryConfig = QueryJobConfiguration
      .newBuilder(query)
      .setPriority(QueryJobConfiguration.Priority.BATCH)
      .build

    val jobId = JobId.newBuilder.setRandomJob().setLocation("US").build
    val jobIdString = jobId.getJob

    bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build)

    val queryJob = bigquery.getJob(JobId.newBuilder.setJob(jobIdString).setLocation("US").build)
    printf("Job %s in location %s currently in state: %s%n",
      queryJob.getJobId.getJob, queryJob.getJobId.getLocation, queryJob.getStatus.getState.toString)
  }


  def initDatabase(): Unit = {

    val table2 = "create table if not exists gunDataset.state ( " +
      " state STRING NOT NULL, " +
      " count INT64 NOT NULL, " +
      " time STRING NOT NULL );"

    val table3 = "create table if not exists gunDataset.city_or_county ( " +
      " city_or_county STRING NOT NULL, " +
      " count INT64 NOT NULL, " +
      " time STRING NOT NULL );"

    executeQuery(table2)
    executeQuery(table3)

  }

  def mergeQuery(datasetName: String, tableName: String, eqField: String): String = {
    s"MERGE $datasetName.$tableName AS sorc " +
      s" USING $datasetName.${tableName}_temporary AS targ" +
      s" ON (sorc.$eqField = targ.$eqField) " +
      " WHEN MATCHED THEN UPDATE " +
      "      SET sorc.count = targ.count + sorc.count, sorc.time = targ.time " +
      " WHEN NOT MATCHED BY TARGET " +
      s" THEN INSERT ( $tableName, count, time ) " +
      s" VALUES ( targ.$tableName, targ.count, targ.time); " +
      s" DROP TABLE $datasetName.${tableName}_temporary; "
  }


  def saveMetricsInDatabaseGlobal(dataFrame: DataFrame,
                                  tableName: String,
                                  time: Time,
                                  sparkSession: SparkSession): Unit = {

    val allTime = fullDateFromMilliseconds(time.milliseconds)
    val resultDataFrame = dataFrame.withColumn("time", lit(allTime))
    //resultDataFrame.show()
    resultDataFrame.write.format("bigquery")
      .option("table", s"$datasetName.${tableName}_temporary")
      .mode(SaveMode.Append)
      .save()

    executeQuery(mergeQuery("gunDataset", tableName, tableName))
  }


  private def loadTable(sparkSession: SparkSession, tableName: String): DataFrame = {
    sparkSession.read.format("bigquery")
      .option("table", s"$datasetName.$tableName")
      .load()
  }

  def calcResultMetricsBatch(sparkSession: SparkSession): Unit = {
    loadTable(sparkSession, "state_batch")
      .groupBy("state")
      .agg(sum("count").alias("count"))
      .orderBy(desc("count"))
      .withColumnRenamed("state", "Top states: ")
      .show(10)

    loadTable(sparkSession, "city_or_county_batch")
      .groupBy("city_or_county")
      .agg(sum("count").alias("count"))
      .orderBy(desc("count"))
      .withColumnRenamed("city_or_county", "Top city or county: ")
      .show(10)

    loadTable(sparkSession, "gun_stolen_batch")
      .groupBy()
      .agg(sum("count")
        .alias("The number of cases when the weapon was stolen or the status is unknown: "))
      .show()
  }

  def calcResultMetricsAll(sparkSession: SparkSession): Unit = {

    loadTable(sparkSession, "state")
      .orderBy(desc("count"))
      .withColumnRenamed("state", "Top states: ")
      .show(10)

    loadTable(sparkSession, "city_or_county")
      .orderBy(desc("count"))
      .withColumnRenamed("city_or_county", "Top city or county: ")
      .show(10)

    loadTable(sparkSession, "gun_stolen_batch")
      .groupBy()
      .agg(sum("count")
        .alias("The number of cases when the weapon was stolen or the status is unknown: "))
      .show()
  }
}
