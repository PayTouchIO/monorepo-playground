package io.paytouch.seeds

import cats.implicits._

import io.paytouch.core.data.model._
import io.paytouch.core.utils.EncryptionSupport
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object AdminSeeds extends Seeds with EncryptionSupport {
  lazy val adminDao = daos.adminDao

  val EncryptedPassword = {
    val Password = "Paytouch2016"
    bcryptEncrypt(Password)
  }

  def load(
      firstName: String,
      lastName: String,
      email: String,
    ): Future[Option[AdminRecord]] =
    if (EmailHelper.isSpecialAccount(email))
      Future.successful(None)
    else
      adminDao
        .upsert(
          AdminUpdate(
            id = Some(adminIdPerEmail(email)),
            firstName = Some(firstName),
            lastName = Some(lastName),
            email = Some(email),
            password = Some(EncryptedPassword),
            lastLoginAt = None,
          ),
        )
        .extractRecord(email)
        .map(_.some)
}
