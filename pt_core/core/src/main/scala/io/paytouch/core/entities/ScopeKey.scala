package io.paytouch.core.entities

import java.time.ZoneId
import java.util.UUID

import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.data.model.enums.ScopeType
import io.paytouch.core.data.model.{ LocationRecord, LocationSettingsRecord }
import io.paytouch.core.utils.{ Formatters, UtcTime }

case class Scope(`type`: ScopeType, key: ScopeKey)

object Scope {
  def dailyScopeKey(zoneId: ZoneId) = UtcTime.now.toLocationTimezone(zoneId).format(Formatters.CompactDateFormatter)

  def buildScope(`type`: ScopeType, keys: String*) = Scope(`type`, ScopeKey.buildScopeKey(keys: _*))

  def fromLocationWithSettings(location: LocationRecord, locationSettings: LocationSettingsRecord) =
    locationSettings.nextOrderNumberScopeType match {
      case s @ ScopeType.Location      => buildScope(s, location.id.toString)
      case s @ ScopeType.LocationDaily => buildScope(s, location.id.toString, dailyScopeKey(location.timezone))
      case s @ ScopeType.Merchant      => buildScope(s, location.merchantId.toString)
    }

  def fromLocationId(locationId: UUID) = apply(ScopeType.Location, locationId)

  def fromMerchantId(merchantId: UUID) = apply(ScopeType.Merchant, merchantId)

  def apply(`type`: ScopeType, id: UUID): Scope = buildScope(`type`, id.toString)
}

case class ScopeKey(value: String) {
  def toDb = value
}

object ScopeKey {
  def buildScopeKey(keys: String*) = ScopeKey(keys.mkString("--"))
}
