package entities

case class DataObject(
                       incident_id: Option[Int],
                       date: String,
                       state: String,
                       city_or_county: String,
                       address: String,
                       n_killed: Option[Int],
                       n_injured: Option[Int],
                       incident_url: String,
                       incident_url_fields_missing: Option[Boolean],
                       congressional_district: Option[Double],
                       gun_stolen: String,
                       gun_type: String,
                       incident_characteristics: String,
                       latitude: Option[Double],
                       location_description: String,
                       longitude: Option[Double],
                       n_guns_involved: String,
                       notes: String,
                       participant_age: String,
                       participant_age_group: String,
                       participant_gender: String,
                       participant_name: String,
                       participant_relationship: String,
                       participant_status: String,
                       participant_type: String,
                       sources: String,
                       state_house_district: Option[Double],
                       state_senate_district: Option[Double]
                     )