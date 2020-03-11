//package database
//import com.google.cloud.Timestamp
//import com.google.cloud.datastore._
//import metrics.CalculateMetrics.Popularity
//
//
//object Database {
//
//  private def convertToDatastore(keyFactory: KeyFactory,
//                                 record: Popularity): FullEntity[IncompleteKey] =
//    FullEntity.newBuilder(keyFactory.newKey())
//      .set("name", record.tag)
//      .set("occurrences", record.amount)
//      .build()
//
//  // [START convert_identity]
//  private[demo] def convertToEntity(hashtags: Array[Popularity],
//                                    keyFactory: String => KeyFactory): FullEntity[IncompleteKey] = {
//    val hashtagKeyFactory: KeyFactory = keyFactory("Hashtag")
//
//    val listValue = hashtags.foldLeft[ListValue.Builder](ListValue.newBuilder())(
//      (listValue, hashTag) => listValue.addValue(convertToDatastore(hashtagKeyFactory, hashTag))
//    )
//
//    val rowKeyFactory: KeyFactory = keyFactory("TrendingHashtags")
//
//    FullEntity.newBuilder(rowKeyFactory.newKey())
//      .set("datetime", Timestamp.now())
//      .set("hashtags", listValue.build())
//      .build()
//  }
//
//  // [END convert_identity]
//
//  def saveRDDtoDataStore(tags: Array[Popularity],
//                         windowLength: Int): Unit = {
//    val datastore: Datastore = DatastoreOptions.getDefaultInstance.getService
//
//    val keyFactoryBuilder = (s: String) => datastore.newKeyFactory().setKind(s)
//
//    val entity: FullEntity[IncompleteKey] = convertToEntity(tags, keyFactoryBuilder)
//
//    datastore.add(entity)
//
//  }
//}
