package io.paytouch.core.data.model

import java.time.{ LocalDate, ZonedDateTime }
import java.util.{ Currency, UUID }

import io.paytouch._
import io.paytouch.core.data.model.enums.PaySchedule
import io.paytouch.core.entities._

final case class UserRecord(
    id: UUID,
    merchantId: UUID,
    userRoleId: Option[UUID],
    firstName: String,
    lastName: String,
    encryptedPassword: String,
    pin: Option[String],
    email: String,
    dob: Option[LocalDate],
    phoneNumber: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
    avatarBgColor: Option[String],
    active: Boolean,
    hourlyRateAmount: Option[BigDecimal],
    overtimeRateAmount: Option[BigDecimal],
    paySchedule: Option[PaySchedule],
    dashboardLastLoginAt: Option[ZonedDateTime],
    registerLastLoginAt: Option[ZonedDateTime],
    ticketsLastLoginAt: Option[ZonedDateTime],
    auth0UserId: Option[String],
    isOwner: Boolean,
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord
       with SlickToggleableRecord {
  def toUserInfo: UserInfo =
    UserInfo(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
    )

  def toUserLogin: UserLogin =
    UserLogin(
      id,
      merchantId,
      userRoleId,
      email,
      encryptedPassword,
      pin,
      active,
      isOwner,
      deletedAt,
    )
}

// BUSINESS RULES: for user owners
// 1) it shouldn't be possible to change the is_owner attribute
// 2) It shouldn't be possible to disable
case class UserUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userRoleId: Option[UUID],
    firstName: Option[String],
    lastName: Option[String],
    encryptedPassword: Option[String],
    pin: ResettableString,
    email: Option[String],
    dob: Option[LocalDate],
    phoneNumber: Option[String],
    addressLine1: ResettableString,
    addressLine2: ResettableString,
    city: ResettableString,
    state: ResettableString,
    country: ResettableString,
    stateCode: ResettableString,
    countryCode: ResettableString,
    postalCode: ResettableString,
    avatarBgColor: Option[String],
    active: Option[Boolean],
    hourlyRateAmount: Option[BigDecimal],
    overtimeRateAmount: Option[BigDecimal],
    paySchedule: Option[PaySchedule],
    auth0UserId: Option[Auth0UserId],
    isOwner: Option[Boolean],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[UserRecord] {
  def toRecord: UserRecord = {
    require(merchantId.isDefined, s"Impossible to convert UserUpdate without a merchant id. [$this]")
    require(firstName.isDefined, s"Impossible to convert UserUpdate without a firstName. [$this]")
    require(lastName.isDefined, s"Impossible to convert UserUpdate without a lastName. [$this]")
    require(encryptedPassword.isDefined, s"Impossible to convert UserUpdate without an encryptedPassword. [$this]")
    require(email.isDefined, s"Impossible to convert UserUpdate without an email. [$this]")

    val hasOwnership = isOwner.contains(true)

    UserRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userRoleId = userRoleId,
      firstName = firstName.get,
      lastName = lastName.get,
      encryptedPassword = encryptedPassword.get,
      pin = pin,
      email = email.get.toLowerCase,
      dob = dob,
      phoneNumber = phoneNumber,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      city = city,
      state = state,
      country = country,
      stateCode = stateCode,
      countryCode = countryCode,
      postalCode = postalCode,
      avatarBgColor = avatarBgColor,
      active = if (hasOwnership) hasOwnership else active.getOrElse(true),
      hourlyRateAmount = hourlyRateAmount,
      overtimeRateAmount = overtimeRateAmount,
      paySchedule = paySchedule,
      dashboardLastLoginAt = None,
      ticketsLastLoginAt = None,
      registerLastLoginAt = None,
      auth0UserId = auth0UserId.map(_.value),
      isOwner = isOwner.getOrElse(false),
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: UserRecord): UserRecord = {
    val hasOwnership = record.isOwner || isOwner.contains(true)
    UserRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userRoleId = userRoleId.orElse(record.userRoleId),
      firstName = firstName.getOrElse(record.firstName),
      lastName = lastName.getOrElse(record.lastName),
      encryptedPassword = encryptedPassword.getOrElse(record.encryptedPassword),
      pin = pin.getOrElse(record.pin),
      email = email.getOrElse(record.email).toLowerCase,
      dob = dob.orElse(record.dob),
      phoneNumber = phoneNumber.orElse(record.phoneNumber),
      addressLine1 = addressLine1.getOrElse(record.addressLine1),
      addressLine2 = addressLine2.getOrElse(record.addressLine2),
      city = city.getOrElse(record.city),
      state = state.getOrElse(record.state),
      country = country.getOrElse(record.country),
      stateCode = stateCode.getOrElse(record.stateCode),
      countryCode = countryCode.getOrElse(record.countryCode),
      postalCode = postalCode.getOrElse(record.postalCode),
      avatarBgColor = avatarBgColor.orElse(record.avatarBgColor),
      active = if (hasOwnership) hasOwnership else active.getOrElse(record.active),
      hourlyRateAmount = hourlyRateAmount.orElse(record.hourlyRateAmount),
      overtimeRateAmount = overtimeRateAmount.orElse(record.overtimeRateAmount),
      paySchedule = paySchedule.orElse(record.paySchedule),
      dashboardLastLoginAt = record.dashboardLastLoginAt,
      registerLastLoginAt = record.registerLastLoginAt,
      ticketsLastLoginAt = record.ticketsLastLoginAt,
      auth0UserId = auth0UserId.map(_.value).orElse(record.auth0UserId),
      isOwner = if (hasOwnership) hasOwnership else isOwner.getOrElse(record.isOwner),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
  }
}
