package io.paytouch.core.utils

import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.TimeZone

import scala.jdk.CollectionConverters._
import scala.collection.immutable

import io.paytouch.core.entities.{ TimeZone => TimeZoneEntity }

trait TimezoneUtils {
  final val timezones: Seq[TimeZoneEntity] = {
    val ids = immutable.ArraySeq.unsafeWrapArray(TimeZone.getAvailableIDs)
    val zones: Set[String] = ZoneId.getAvailableZoneIds.asScala.toSet

    ids.flatMap { id =>
      val timeZone = TimeZone.getTimeZone(id)
      val rawOffSet = timeZone.getRawOffset
      val hours = TimeUnit.MILLISECONDS.toHours(rawOffSet)
      val minutes = (TimeUnit.MILLISECONDS.toMinutes(rawOffSet) - TimeUnit.HOURS.toMinutes(hours)).abs
      val sign = if (hours >= 0) "+" else ""
      val offset = f"GMT$sign$hours%d:$minutes%02d"

      zones
        .find(_.equalsIgnoreCase(timeZone.getID))
        .map { zoneId =>
          TimeZoneEntity(
            offset = offset,
            id = zoneId,
          )
        }
    }
  }
}
