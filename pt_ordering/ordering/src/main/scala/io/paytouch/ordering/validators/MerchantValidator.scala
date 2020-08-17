package io.paytouch.ordering.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.data.daos.{ Daos, MerchantDao }
import io.paytouch.ordering.data.model.MerchantRecord
import io.paytouch.ordering.entities
import io.paytouch.ordering.errors.{ InvalidMerchantIds, NonAccessibleMerchantIds, UrlSlugAlreadyTaken }
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData
import io.paytouch.ordering.utils.validation.{ ValidatedData, ValidatedOptData }
import io.paytouch.ordering.validators.features.{ UpsertionValidator, Validator }

import scala.concurrent.{ ExecutionContext, Future }

class MerchantValidator(implicit val ec: ExecutionContext, val daos: Daos) extends Validator with UpsertionValidator {

  type Context = entities.UserContext
  type Dao = MerchantDao
  type Record = MerchantRecord
  type Upsertion = entities.MerchantUpdate

  protected val dao = daos.merchantDao

  val validationErrorF = InvalidMerchantIds(_)
  val accessErrorF = NonAccessibleMerchantIds(_)

  protected def recordsFinder(ids: Seq[UUID])(implicit user: Context): Future[Seq[Record]] =
    dao.findByIds(ids)

  protected def validityCheck(record: Record)(implicit user: Context): Boolean =
    record.id == user.merchantId

  def validateUpsertion(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[ValidatedData[Upsertion]] =
    validateUrlSlugUnique(upsertion.urlSlug).mapValid(_ => upsertion)

  private def validateUrlSlugUnique(
      urlSlug: Option[String],
    )(implicit
      context: entities.AppContext,
    ): Future[ValidatedOptData[String]] = {
    val merchantId = context.merchantId
    urlSlug match {
      case None => Future.successful(ValidatedOptData.empty)
      case Some(slug) =>
        dao.existsUrlSlug(idToExclude = merchantId, slug).map { exists =>
          if (exists) ValidatedOptData.failure(UrlSlugAlreadyTaken(slug))
          else ValidatedOptData.successOpt(slug)
        }
    }
  }

  def validateUrlSlug(urlSlug: String)(implicit context: entities.AppContext): Future[ValidatedData[Unit]] =
    validateUrlSlugUnique(Some(urlSlug)).map {
      case Valid(_)       => ValidatedData.success(())
      case i @ Invalid(_) => i
    }

}
