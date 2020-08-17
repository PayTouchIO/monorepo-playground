package io.paytouch.core.json.serializers

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ PaytouchSpec, UtcTime }
import org.json4s.JsonAST.JString
import org.json4s._

final case class TestString(
    toReset: ResettableString,
    toIgnore: ResettableString,
    toChange: ResettableString,
    toNothing: ResettableString,
  )

final case class TestBigDecimal(
    toReset: ResettableBigDecimal,
    toIgnore: ResettableBigDecimal,
    toChange: ResettableBigDecimal,
    toNothing: ResettableBigDecimal,
  )

final case class TestUUID(
    toReset: ResettableUUID,
    toIgnore: ResettableUUID,
    toChange: ResettableUUID,
    toNothing: ResettableUUID,
  )

final case class TestLocalDate(
    toReset: ResettableLocalDate,
    toIgnore: ResettableLocalDate,
    toChange: ResettableLocalDate,
    toNothing: ResettableLocalDate,
  )

final case class TestLocalTime(
    toReset: ResettableLocalTime,
    toIgnore: ResettableLocalTime,
    toChange: ResettableLocalTime,
    toNothing: ResettableLocalTime,
  )

final case class TestZonedDateTime(
    toReset: ResettableZonedDateTime,
    toIgnore: ResettableZonedDateTime,
    toChange: ResettableZonedDateTime,
    toNothing: ResettableZonedDateTime,
  )

final case class TestBillingDetails(
    toReset: ResettableBillingDetails,
    toIgnore: ResettableBillingDetails,
    toChange: ResettableBillingDetails,
    toNothing: ResettableBillingDetails,
  )

final case class TestSeating(
    toReset: ResettableSeating,
    toIgnore: ResettableSeating,
    toChange: ResettableSeating,
  )

class ResettableSerializationsSpec extends PaytouchSpec {

  "ResettableSerializers" should {

    "serialize correctly a resettable string" in {
      val value = "hello"
      val data =
        JObject(JField("toReset", JString("    ")), JField("toIgnore", JNull), JField("toChange", JString(value)))

      data.extract[TestString] ==== TestString(
        toReset = ResettableString.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )
    }

    "serialize correctly a resettable big decimal" in {
      val value = "12.34"
      val data =
        JObject(JField("toReset", JString("    ")), JField("toIgnore", JNull), JField("toChange", JString(value)))

      data.extract[TestBigDecimal] ==== TestBigDecimal(
        toReset = ResettableBigDecimal.reset,
        toIgnore = None,
        toChange = BigDecimal(value),
        toNothing = None,
      )
    }

    "serialize correctly a resettable uuid" in {
      val value = UUID.randomUUID
      val data =
        JObject(
          JField("toReset", JString("    ")),
          JField("toIgnore", JNull),
          JField("toChange", JString(value.toString)),
        )

      data.extract[TestUUID] ==== TestUUID(
        toReset = ResettableUUID.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )
    }

    "serialize correctly a resettable local date" in {
      val value = LocalDate.of(1987, 11, 22)
      val data =
        JObject(
          JField("toReset", JString("    ")),
          JField("toIgnore", JNull),
          JField("toChange", JString(value.toString)),
        )

      data.extract[TestLocalDate] ==== TestLocalDate(
        toReset = ResettableLocalDate.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )
    }

    "serialize correctly a resettable local time" in {
      val value = LocalTime.of(10, 20, 30)
      val data =
        JObject(
          JField("toReset", JString("    ")),
          JField("toIgnore", JNull),
          JField("toChange", JString(value.toString)),
        )

      data.extract[TestLocalTime] ==== TestLocalTime(
        toReset = ResettableLocalTime.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )
    }

    "serialize correctly a resettable zoned date time" in {
      val value = UtcTime.now
      val data =
        JObject(
          JField("toReset", JString("    ")),
          JField("toIgnore", JNull),
          JField("toChange", JString(value.toString)),
        )

      data.extract[TestZonedDateTime] ==== TestZonedDateTime(
        toReset = ResettableZonedDateTime.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )
    }

    "serialize correctly a resettable billing details (legal details)" in {
      val value = random[LegalDetails]

      val data =
        JObject(
          JField("toReset", JString("    ")),
          JField("toIgnore", JNull),
          JField(
            "toChange",
            Extraction.decompose(value),
          ),
        )

      data.extract[TestBillingDetails] ==== TestBillingDetails(
        toReset = ResettableBillingDetails.reset,
        toIgnore = None,
        toChange = value,
        toNothing = None,
      )

      Extraction.decompose(ResettableBillingDetails.reset) ==== JString("")
      Extraction.decompose(ResettableBillingDetails.ignore) ==== JNothing
    }

    "serialize correctly a resettable seating" in {
      val value = random[Seating].copy(
        createdAt = UtcTime.now,
        updatedAt = UtcTime.now,
      )

      val data =
        JObject(
          JField("toReset", JNull),
          JField(
            "toChange",
            Extraction.decompose(value),
          ),
        )

      val expected = TestSeating(
        toReset = ResettableSeating.reset,
        toIgnore = None,
        toChange = value,
      )

      data.extract[TestSeating] ==== expected

      val data2 =
        JObject(
          JField("toReset", JNull),
          JField(
            "toChange",
            Extraction.decompose(value),
          ),
          JField("toIgnore", JNothing),
        )

      data2.extract[TestSeating] ==== expected

      Extraction.decompose(ResettableSeating.reset) ==== JNull
      Extraction.decompose(ResettableSeating.ignore) ==== JNothing
    }
  }
}
