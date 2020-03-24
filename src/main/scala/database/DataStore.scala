package database

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.streaming.Time
import utils.Utils._


object DataStore {

  val filepath: String = ConfigFactory.load().getString("dataStore.path")


  def saveInDataStore(time: Time, dataFrame: DataFrame): Unit = {
    if (dataFrame.head(1).isEmpty) {
      return
    }

    val time_date = dateFromMilliseconds(time.milliseconds)
    val time_hours = hoursFromMilliseconds(time.milliseconds)
    val fullName = fullDateFromMilliseconds(time.milliseconds)

    dataFrame.write.mode(SaveMode.Overwrite)
      //.partitionBy("Time")
      .parquet(s"$filepath/$time_date/$time_hours/$fullName.parquet")

  }


  // Very bad way when we has many data, because we collect all data on one node
//
//  private def convertToDatastore(keyFactory: KeyFactory,
//                                 record: DataObject): FullEntity[IncompleteKey] = {
//
//    FullEntity.newBuilder(keyFactory.newKey())
//      .set("incident_id", record.incident_id.get)
//      .set("date", record.date)
//      .set("state", record.state)
//      .set("city_or_county", record.city_or_county)
//      .set("address", record.address)
//      .set("n_killed", record.n_killed.get)
//      .set("n_injured", record.n_injured.get)
//      .set("incident_url", record.incident_url)
//      .set("incident_url_fields_missing", record.incident_url_fields_missing.get)
//      .set("congressional_district", record.congressional_district.get)
//      .set("gun_stolen", record.gun_stolen)
//      .set("gun_type", record.gun_type)
//      .set("incident_characteristics", record.incident_characteristics)
//      .set("latitude", record.latitude.get)
//      .set("location_description", record.location_description)
//      .set("longitude", record.longitude.get)
//      .set("n_guns_involved", record.n_guns_involved)
//      .set("notes", record.notes)
//      .set("participant_age", record.participant_age)
//      .set("participant_age_group", record.participant_age_group)
//      .set("participant_gender", record.participant_gender)
//      .set("participant_name", record.participant_name)
//      .set("participant_relationship", record.participant_relationship)
//      .set("participant_status", record.participant_status)
//      .set("participant_type", record.participant_type)
//      .set("state_house_district", record.state_house_district.get)
//      .set("state_senate_district", record.state_senate_district.get)
//      .build()
//  }
//
//
//  private[database] def convertToEntity(hashtags: Array[DataObject],
//                                    keyFactory: String => KeyFactory,
//                                    time: Time): FullEntity[IncompleteKey] = {
//
//    val hashtagKeyFactory: KeyFactory = keyFactory("DataObjects")
//
//    val listValue = hashtags.foldLeft[ListValue.Builder](ListValue.newBuilder())(
//      (listValue, hashTag) => listValue.addValue(convertToDatastore(hashtagKeyFactory, hashTag))
//    )
//
//    val rowKeyFactory: KeyFactory = keyFactory("TrendingHashtags")
//
//    val fullTime = fullDateFromMilliseconds(time.milliseconds)
//
//    FullEntity.newBuilder(rowKeyFactory.newKey())
//      .set("batchTime", fullTime)
//      .set("batchData", listValue.build())
//      .build()
//  }
//
//  def saveRDDtoDataStore(time: Time, rdd: RDD[DataObject]): Unit = {
//
//    val datastore: Datastore = DatastoreOptions.getDefaultInstance.getService
//    val keyFactoryBuilder = (s: String) => datastore.newKeyFactory().setKind(s)
//
//    val data = rdd.collect()
//    val entity: FullEntity[IncompleteKey] = convertToEntity(data, keyFactoryBuilder, time)
//
//    datastore.add(entity)
//
//  }
}
