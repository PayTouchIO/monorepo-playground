package io.paytouch.core.entities

import java.time.ZoneId

import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck

import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.{ PaytouchSpec, TimezoneUtils }

class UtilHelperSpec extends PaytouchSpec with ScalaCheck {
  implicit val arbTimeZones: Arbitrary[TimeZone] = Arbitrary(Gen.oneOf(UtilService.Geo.timezones))

  "Utils" should {
    "convert timezones to zone ids" ! prop { timezone: TimeZone =>
      ZoneId.of(timezone.id) should beAnInstanceOf[ZoneId]
    }
  }
}
