package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.AdminRecord
import io.paytouch.core.entities.{ AdminUpdate, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features._

class AdminValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends UniqueEmailValidator[AdminRecord]
       with PasswordValidator {
  protected val dao = daos.adminDao
  val validationErrorF = InvalidAdminIds(_)
  val accessErrorF = NonAccessibleAdminIds(_)

  protected def findUserById(email: String) = dao.findByEmail(email)

  def accessOneById(id: UUID): Future[ErrorsOr[AdminRecord]] =
    dao.findById(id).map {
      case Some(admin) => Multiple.success(admin)
      case _           => Multiple.failure(NonAccessibleAdminIds(ids = Seq(id)))
    }

  def availableOneById(id: UUID): Future[ErrorsOr[Unit]] =
    dao.findById(id).map {
      case Some(_) => Multiple.failure(NonAccessibleAdminIds(Seq(id)))
      case _       => Multiple.success((): Unit)
    }

  def validateUpsertion(id: UUID, update: AdminUpdate): Future[ErrorsOr[AdminUpdate]] =
    for {
      validPassword <- validatePassword(update.password).pure[Future]
      validEmail <- validateEmail(id, update.email)
    } yield Multiple.combine(validPassword, validEmail) { case _ => update }
}
