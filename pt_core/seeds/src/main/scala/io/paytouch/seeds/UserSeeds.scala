package io.paytouch.seeds

import scala.concurrent._

import io.paytouch.core.data.model.{ UserRecord, UserRoleRecord, UserUpdate }
import io.paytouch.core.entities.ResettableString
import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.utils.EncryptionSupport
import io.paytouch.seeds.IdsProvider._

object UserSeeds extends Seeds with EncryptionSupport {
  lazy val userDao = daos.userDao

  val EncryptedPassword = {
    val Password = "Paytouch2016"
    bcryptEncrypt(Password)
  }

  val EncryptedPin = {
    val Pin = "2222"
    sha1Encrypt(Pin)
  }

  def load(
      firstName: String,
      lastName: String,
      email: String,
      userRole: UserRoleRecord,
    ): Future[UserRecord] = {
    val userId = userIdPerEmail(email)

    val user = {
      val address = genAddress.instance
      UserUpdate(
        id = Some(userId),
        merchantId = Some(userRole.merchantId),
        userRoleId = Some(userRole.id),
        firstName = Some(firstName),
        lastName = Some(lastName),
        encryptedPassword = Some(EncryptedPassword),
        pin = EncryptedPin,
        email = Some(email),
        dob = None,
        phoneNumber = Some(s"+$randomNumericString"),
        addressLine1 = address.line1,
        addressLine2 = Some(address.line2),
        city = address.city,
        state = address.state,
        country = address.country,
        stateCode = address.stateData.map(_.code),
        countryCode = address.countryData.map(_.code),
        postalCode = address.postalCode,
        avatarBgColor = Some(genColor.instance),
        active = None,
        auth0UserId = None,
        isOwner = Some(true),
        hourlyRateAmount = None,
        overtimeRateAmount = None,
        paySchedule = None,
        deletedAt = None,
      )
    }
    userDao.upsert(user).extractRecord(email)
  }
}
