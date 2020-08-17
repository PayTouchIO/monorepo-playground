package io.paytouch.core.data.tables

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

import com.github.tminglei.slickpg.utils.PlainSQLUtils

import slick.jdbc.GetResult
import slickless._

import shapeless._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ActiveColumn
import io.paytouch.core.data.model.enums.PaySchedule
import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.entities.{ UserInfo, UserLogin }

class UsersTable(tag: Tag) extends SlickSoftDeleteTable[UserRecord](tag, "users") with ActiveColumn {
  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def userRoleId = column[Option[UUID]]("user_role_id")

  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def password = column[String]("password")
  def pin = column[Option[String]]("pin")
  def email = column[String]("email")

  def dob = column[Option[LocalDate]]("dob")
  def phoneNumber = column[Option[String]]("phone_number")

  def addressLine1 = column[Option[String]]("address_line_1")
  def addressLine2 = column[Option[String]]("address_line_2")
  def city = column[Option[String]]("city")
  def state = column[Option[String]]("state")
  def country = column[Option[String]]("country")
  def stateCode = column[Option[String]]("state_code")
  def countryCode = column[Option[String]]("country_code")
  def postalCode = column[Option[String]]("postal_code")

  def avatarBgColor = column[Option[String]]("avatar_bg_color")
  def active = column[Boolean]("active")

  def hourlyRateAmount = column[Option[BigDecimal]]("hourly_rate_amount")
  def overtimeRateAmount = column[Option[BigDecimal]]("overtime_rate_amount")
  def paySchedule = column[Option[PaySchedule]]("pay_schedule")

  def dashboardLastLoginAt = column[Option[ZonedDateTime]]("dashboard_last_login_at")
  def registerLastLoginAt = column[Option[ZonedDateTime]]("register_last_login_at")
  def ticketsLastLoginAt = column[Option[ZonedDateTime]]("tickets_last_login_at")

  def auth0UserId = column[Option[String]]("auth0_user_id")

  def isOwner = column[Boolean]("is_owner")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def userInfo =
    (
      id,
      firstName,
      lastName,
      email,
    ).<>(UserInfo.tupled, UserInfo.unapply)

  def userLogin =
    (
      id,
      merchantId,
      userRoleId,
      email,
      password,
      pin,
      active,
      isOwner,
      deletedAt,
    ).<>(UserLogin.tupled, UserLogin.unapply)

  def * = {
    val userGeneric = Generic[UserRecord]

    (id :: merchantId :: userRoleId ::
      firstName :: lastName :: password :: pin :: email ::
      dob :: phoneNumber ::
      addressLine1 :: addressLine2 :: city :: state :: country :: stateCode :: countryCode :: postalCode ::
      avatarBgColor :: active ::
      hourlyRateAmount :: overtimeRateAmount :: paySchedule ::
      dashboardLastLoginAt :: registerLastLoginAt :: ticketsLastLoginAt :: auth0UserId :: isOwner ::
      deletedAt :: createdAt :: updatedAt :: HNil).<>(
      (dbRow: userGeneric.Repr) => userGeneric.from(dbRow),
      (caseClass: UserRecord) => Some(userGeneric.to(caseClass)),
    )
  }
}

object UsersTable {
  implicit def getResultUserRecord: GetResult[UserRecord] = {
    import io.paytouch.core.data.driver.CustomPostgresDriver.plainApi._
    implicit val getUUID: GetResult[UUID] = PlainSQLUtils.mkGetResult(_.nextUUID())
    implicit val getOptionUUID: GetResult[Option[UUID]] = PlainSQLUtils.mkGetResult(_.nextUUIDOption())
    implicit def getOptionPaySchedule: GetResult[Option[PaySchedule]] =
      PlainSQLUtils.mkGetResult(_.nextEnumOption(PaySchedule))

    GetResult(r =>
      UserRecord(
        id = r.<<,
        merchantId = r.<<,
        userRoleId = r.<<,
        firstName = r.<<,
        lastName = r.<<,
        encryptedPassword = r.<<,
        pin = r.<<?,
        email = r.<<,
        dob = r.<<?,
        phoneNumber = r.<<?,
        addressLine1 = r.<<?,
        addressLine2 = r.<<?,
        city = r.<<?,
        state = r.<<?,
        country = r.<<?,
        stateCode = r.<<?,
        countryCode = r.<<?,
        postalCode = r.<<?,
        avatarBgColor = r.<<?,
        active = r.<<,
        hourlyRateAmount = r.<<?,
        overtimeRateAmount = r.<<?,
        paySchedule = r.<<?,
        dashboardLastLoginAt = r.<<?,
        registerLastLoginAt = r.<<?,
        ticketsLastLoginAt = r.<<?,
        auth0UserId = r.<<?,
        isOwner = r.<<,
        deletedAt = r.<<,
        createdAt = r.<<,
        updatedAt = r.<<,
      ),
    )
  }
}
