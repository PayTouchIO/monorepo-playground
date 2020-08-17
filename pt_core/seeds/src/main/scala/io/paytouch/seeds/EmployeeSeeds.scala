package io.paytouch.seeds

import scala.concurrent._

import io.paytouch.core.data.model.{ UserRecord, UserRoleRecord, UserUpdate }
import io.paytouch.core.entities.ResettableString
import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.utils.EncryptionSupport
import io.paytouch.seeds.IdsProvider._

object EmployeeSeeds extends Seeds with EncryptionSupport {
  lazy val userDao = daos.userDao

  val Password = "Paytouch2016"
  val EncryptedPassword = bcryptEncrypt(Password)

  def load(userRoles: Seq[UserRoleRecord])(implicit user: UserRecord): Future[Seq[UserRecord]] = {
    val employeeIds = employeeIdsPerEmail(user.email)

    val nonAdminUserRoles = userRoles.filterNot(_.name == "Admin")

    val employees = employeeIds.zipWithIndex.map {
      case (employeeId, idx) =>
        val userRole = nonAdminUserRoles.random
        val firstName = randomWord
        val lastName = randomWord
        val address = genAddress.instance
        UserUpdate(
          id = Some(employeeId),
          merchantId = Some(user.merchantId),
          userRoleId = Some(userRole.id),
          firstName = Some(firstName),
          lastName = Some(lastName),
          encryptedPassword = Some(EncryptedPassword),
          pin = None,
          email = Some(s"$firstName.$lastName.$employeeId.$idx@paytouch.test.io"),
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
          isOwner = Some(false),
          hourlyRateAmount = Some(genBigDecimal.instance),
          overtimeRateAmount = Some(genBigDecimal.instance),
          paySchedule = Some(genPaySchedule.instance),
          deletedAt = None,
        )
    }
    userDao.bulkUpsert(employees).extractRecords
  }
}
