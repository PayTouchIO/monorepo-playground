package io.paytouch.core.validators.features

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model.SlickId
import io.paytouch.core.errors.EmailAlreadyInUse
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.Multiple

import scala.concurrent._

trait UniqueEmailValidator[Record <: SlickId] extends EmailValidator {

  def validateEmail(id: UUID, email: Option[String]): Future[ErrorsOr[Option[String]]] =
    email match {
      case Some(em) => validateEmail(id, em).mapNested(Some(_))
      case _        => Future.successful(Multiple.empty)
    }

  def validateEmail(
      id: UUID,
      email: String,
      idToIgnore: Option[UUID] = None,
    ): Future[ErrorsOr[String]] =
    for {
      validEmail <- validateEmailFormat(email)
      validUniqueEmail <- validateUniqueEmail(id, email, idToIgnore)
    } yield Multiple.combine(validEmail, validUniqueEmail) { case (ve, _) => ve }

  protected def findUserById(email: String): Future[Option[Record]]

  private def validateUniqueEmail(
      id: UUID,
      email: String,
      idToIgnore: Option[UUID],
    ): Future[ErrorsOr[String]] =
    findUserById(email).map {
      case Some(userLogin) if idToIgnore.contains(userLogin.id) => Multiple.success(email)
      case Some(userLogin) if userLogin.id != id                => Multiple.failure(EmailAlreadyInUse())
      case _                                                    => Multiple.success(email)
    }
}
