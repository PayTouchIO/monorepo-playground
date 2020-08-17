package io.paytouch.core.conversions

import java.util.{ Currency, UUID }

import io.paytouch._
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.EncryptionSupport

trait UserConversions extends EntityConversionMerchantContext[model.UserRecord, User] with EncryptionSupport {
  def fromRecordToEntity(record: model.UserRecord)(implicit merchant: MerchantContext): User =
    fromRecordAndOptionsToEntity(record, None, None, None, Seq.empty, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[model.UserRecord],
      userRoles: Map[model.UserRecord, UserRole],
      locationsPerUser: Option[Map[model.UserRecord, Seq[Location]]],
      merchant: Option[Merchant],
      imageUrlsPerUser: Map[model.UserRecord, Seq[ImageUrls]],
      accessPerUser: Option[Map[model.UserRecord, Seq[LoginSource]]],
    )(implicit
      user: UserContext,
    ): Seq[User] = {
    implicit val merchantContext = user.toMerchantContext
    records.map { record =>
      val userRole = userRoles.get(record)
      val locations = locationsPerUser.map(_.getOrElse(record, Seq.empty))
      val imageUrls = imageUrlsPerUser.getOrElse(record, Seq.empty)
      val access = accessPerUser.map(_.getOrElse(record, Seq.empty))
      fromRecordAndOptionsToEntity(record, userRole, locations, merchant, imageUrls, access)
    }
  }

  def fromRecordAndOptionsToEntity(
      record: model.UserRecord,
      userRole: Option[UserRole],
      locations: Option[Seq[Location]],
      merchant: Option[Merchant],
      imageUrls: Seq[ImageUrls],
      access: Option[Seq[LoginSource]],
    )(implicit
      merchantContext: MerchantContext,
    ): User =
    User(
      id = record.id,
      firstName = record.firstName,
      lastName = record.lastName,
      email = record.email,
      merchantId = record.merchantId,
      userRoleId = record.userRoleId,
      userRole = userRole,
      locations = locations,
      merchant = merchant,
      dob = record.dob,
      phoneNumber = record.phoneNumber,
      address = Address(
        line1 = record.addressLine1,
        line2 = record.addressLine2,
        city = record.city,
        state = record.state,
        country = record.country,
        stateData = UtilService
          .Geo
          .addressState(
            record.countryCode.map(CountryCode),
            record.stateCode.map(StateCode),
            record.country.map(CountryName),
            record.state.map(StateName),
          ),
        countryData = UtilService
          .Geo
          .country(
            record.countryCode.map(CountryCode),
            record.country.map(CountryName),
          ),
        postalCode = record.postalCode,
      ),
      avatarBgColor = record.avatarBgColor,
      active = record.active,
      isOwner = record.isOwner,
      hourlyRate = MonetaryAmount.extract(record.hourlyRateAmount, merchantContext),
      overtimeRate = MonetaryAmount.extract(record.overtimeRateAmount, merchantContext),
      paySchedule = record.paySchedule,
      dashboardLastLoginAt = record.dashboardLastLoginAt,
      registerLastLoginAt = record.registerLastLoginAt,
      ticketsLastLoginAt = record.ticketsLastLoginAt,
      avatarImageUrls = imageUrls,
      access = access,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(id: UUID, upsertion: UserUpdate)(implicit user: UserContext): model.UserUpdate =
    model.UserUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userRoleId = upsertion.userRoleId,
      firstName = upsertion.firstName,
      lastName = upsertion.lastName,
      encryptedPassword = upsertion.password.map(bcryptEncrypt),
      pin = upsertion.pin.map(_.map(sha1Encrypt)),
      email = upsertion.email,
      dob = upsertion.dob,
      phoneNumber = upsertion.phoneNumber,
      addressLine1 = upsertion.address.line1,
      addressLine2 = upsertion.address.line2,
      city = upsertion.address.city,
      state = upsertion.address.state,
      country = upsertion.address.country,
      stateCode = upsertion.address.stateCode,
      countryCode = upsertion.address.countryCode,
      postalCode = upsertion.address.postalCode,
      avatarBgColor = upsertion.avatarBgColor,
      active = upsertion.active,
      hourlyRateAmount = upsertion.hourlyRateAmount,
      overtimeRateAmount = upsertion.overtimeRateAmount,
      paySchedule = upsertion.paySchedule,
      auth0UserId = None,
      isOwner = upsertion.isOwner,
      deletedAt = None,
    )

  def fromMerchantCreationToOwnerUserUpdate(merchantId: UUID, creation: MerchantCreation): model.UserUpdate =
    model.UserUpdate(
      id = None,
      merchantId = Some(merchantId),
      userRoleId = None, // populated after admin user role is created
      firstName = Some(creation.firstName),
      lastName = Some(creation.lastName),
      encryptedPassword = Some(bcryptEncrypt(creation.password)),
      pin = creation.pin.map(sha1Encrypt),
      email = Some(creation.email),
      dob = None,
      phoneNumber = None,
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
      avatarBgColor = None,
      active = Some(true),
      hourlyRateAmount = None,
      overtimeRateAmount = None,
      paySchedule = None,
      auth0UserId = creation.auth0UserId,
      isOwner = Some(true),
      deletedAt = None,
    )

  protected def toUserRolePerUser(
      users: Seq[model.UserRecord],
      userRoles: Seq[UserRole],
    )(
      withPermission: Boolean,
    ): Map[model.UserRecord, UserRole] =
    if (!withPermission) Map.empty
    else
      users.flatMap { user =>
        val maybeUserRole = userRoles.find(role => user.userRoleId.contains(role.id))
        maybeUserRole.map(role => user -> role)
      }.toMap

  protected def toAccessPerUser(
      users: Seq[model.UserRecord],
      userRoles: Seq[UserRole],
    )(
      withAccess: Boolean,
    ): Option[Map[model.UserRecord, Seq[LoginSource]]] =
    if (!withAccess) None
    else {
      val accessPerUserRole: Map[UUID, Seq[LoginSource]] = userRoles.map { userRole =>
        val sources = Map(
          userRole.hasDashboardAccess -> LoginSource.PtDashboard,
          userRole.hasRegisterAccess -> LoginSource.PtRegister,
          userRole.hasTicketsAccess -> LoginSource.PtTickets,
        )
        userRole.id -> sources.view.filterKeys(identity).values.toSeq
      }.toMap
      val accessPerUser = users.map { user =>
        val access =
          user.userRoleId.fold(Seq.empty[LoginSource])(roleId => accessPerUserRole.getOrElse(roleId, Seq.empty))
        user -> access
      }.toMap
      Some(accessPerUser)
    }
}
