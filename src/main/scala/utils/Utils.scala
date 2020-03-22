package utils

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}



object Utils {

  val patternFilename: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val patternDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val patternHours: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

  def dateFromMilliseconds(time: Long): String = {
    val instant = Instant.ofEpochMilli(time)
    patternDate.withZone(ZoneId.systemDefault()).format(instant)
  }

  def hoursFromMilliseconds(time: Long): String = {
    val instant = Instant.ofEpochMilli(time)
    patternHours.withZone(ZoneId.systemDefault()).format(instant)
  }

  def fullDateFromMilliseconds(time: Long): String = {
    val instant = Instant.ofEpochMilli(time)
    patternFilename.withZone(ZoneId.systemDefault()).format(instant)
  }
}
