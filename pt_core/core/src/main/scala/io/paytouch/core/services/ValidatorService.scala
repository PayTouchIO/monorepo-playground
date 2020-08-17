package io.paytouch.core.services

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ IdsToValidate, UserContext }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators._

class ValidatorService(val ptOrderingClient: PtOrderingClient)(implicit val ec: ExecutionContext, val daos: Daos) {
  private lazy val locationValidator = new LocationValidator
  private lazy val catalogValidator = new CatalogValidator(ptOrderingClient)
  private lazy val imageUploadValidator = new ImageUploadValidator

  def validate(ids: IdsToValidate)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    Future.sequence(executeValidations(ids)).map { results =>
      val validations: Seq[ErrorsOr[Unit]] = results.map(_.void)
      validations.fold(Multiple.success((): Unit))((a, b) => Multiple.combine(a, b) { case _ => (): Unit })
    }

  private def executeValidations(ids: IdsToValidate)(implicit user: UserContext): Seq[Future[ErrorsOr[Any]]] =
    Seq(
      locationValidator.accessByIds(ids.locationIds),
      catalogValidator.accessByIds(ids.catalogIds),
      imageUploadValidator.accessByIdsAndImageUploadType(ids.imageUploadIds),
    )
}
