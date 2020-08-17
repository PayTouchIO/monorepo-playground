package io.paytouch.core.entities

import java.time.{ LocalDate, ZonedDateTime }
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.SlickId
import io.paytouch.core.data.model.enums.PaySchedule
import io.paytouch.core.entities.enums.{ ExposedName, LoginSource }

final case class User(
    id: UUID,
    firstName: String,
    lastName: String,
    email: String,
    merchantId: UUID,
    userRoleId: Option[UUID],
    userRole: Option[UserRole],
    locations: Option[Seq[Location]],
    merchant: Option[Merchant],
    dob: Option[LocalDate],
    phoneNumber: Option[String],
    address: Address,
    avatarBgColor: Option[String],
    active: Boolean,
    hourlyRate: Option[MonetaryAmount],
    overtimeRate: Option[MonetaryAmount],
    paySchedule: Option[PaySchedule],
    dashboardLastLoginAt: Option[ZonedDateTime],
    registerLastLoginAt: Option[ZonedDateTime],
    ticketsLastLoginAt: Option[ZonedDateTime],
    avatarImageUrls: Seq[ImageUrls],
    isOwner: Boolean,
    access: Option[Seq[LoginSource]],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.User
}

final case class UserInfo(
    id: UUID,
    firstName: String,
    lastName: String,
    email: String,
  ) {
  val fullName = s"$firstName $lastName"
}

final case class UserLogin(
    id: UUID,
    merchantId: UUID,
    userRoleId: Option[UUID],
    email: String,
    encryptedPassword: String,
    hashedPin: Option[String],
    active: Boolean,
    isOwner: Boolean,
    deletedAt: Option[ZonedDateTime],
  ) extends SlickId

final case class UserCreation(
    userRoleId: Option[UUID],
    firstName: String,
    lastName: String,
    password: String,
    pin: Option[String],
    email: String,
    dob: Option[LocalDate],
    phoneNumber: Option[String],
    address: AddressUpsertion = AddressUpsertion.empty,
    avatarBgColor: Option[String],
    active: Boolean,
    isOwner: Option[Boolean],
    hourlyRateAmount: Option[BigDecimal],
    overtimeRateAmount: Option[BigDecimal],
    paySchedule: Option[PaySchedule],
    avatarImageId: ResettableUUID,
    locationIds: Option[Seq[UUID]],
    locationOverrides: Map[UUID, Boolean] = Map.empty,
  ) extends CreationEntity[User, UserUpdate] {
  def asUpdate =
    UserUpdate(
      userRoleId = userRoleId,
      firstName = Some(firstName),
      lastName = Some(lastName),
      password = Some(password),
      pin = pin,
      email = Some(email),
      dob = dob,
      phoneNumber = phoneNumber,
      address = address,
      avatarBgColor = avatarBgColor,
      active = Some(active),
      isOwner = isOwner,
      hourlyRateAmount = hourlyRateAmount,
      overtimeRateAmount = overtimeRateAmount,
      paySchedule = paySchedule,
      avatarImageId = avatarImageId,
      locationIds = locationIds,
      locationOverrides = locationOverrides,
    )
}

final case class UserUpdate(
    userRoleId: Option[UUID],
    firstName: Option[String],
    lastName: Option[String],
    password: Option[String],
    pin: ResettableString,
    email: Option[String],
    dob: Option[LocalDate],
    phoneNumber: Option[String],
    address: AddressUpsertion = AddressUpsertion.empty,
    avatarBgColor: Option[String],
    active: Option[Boolean],
    isOwner: Option[Boolean],
    hourlyRateAmount: Option[BigDecimal],
    overtimeRateAmount: Option[BigDecimal],
    paySchedule: Option[PaySchedule],
    avatarImageId: ResettableUUID,
    locationIds: Option[Seq[UUID]],
    locationOverrides: Map[UUID, Boolean] = Map.empty,
  ) extends UpdateEntity[User]
