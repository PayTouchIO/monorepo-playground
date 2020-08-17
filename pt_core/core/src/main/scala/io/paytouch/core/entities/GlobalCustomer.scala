package io.paytouch.core.entities

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

final case class GlobalCustomer(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    dob: Option[LocalDate],
    anniversary: Option[LocalDate],
    email: Option[String],
    phoneNumber: Option[String],
    address: Address,
    mobileStorefrontLastLogin: Option[ZonedDateTime],
    webStorefrontLastLogin: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  )
