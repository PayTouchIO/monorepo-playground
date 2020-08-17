package io.paytouch.ordering.validators

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities.CoreIds
import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageType
import io.paytouch.ordering.data.daos.{ Daos, StoreDao }
import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.entities.{ StoreUpdate, UserContext }
import io.paytouch.ordering.errors.{
  InvalidStoreIds,
  InvalidStoreLocationAssociation,
  NonAccessibleStoreIds,
  UrlSlugAlreadyTaken,
}
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.features.{ DefaultUserValidator, PtCoreValidator, UpsertionValidator }

import scala.concurrent.{ ExecutionContext, Future }

class StoreValidator(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultUserValidator
       with UpsertionValidator
       with PtCoreValidator {
  type Dao = StoreDao
  type Record = StoreRecord
  type Upsertion = StoreUpdate

  protected val dao = daos.storeDao

  val validationErrorF = InvalidStoreIds(_)
  val accessErrorF = NonAccessibleStoreIds(_)

  private val merchantValidator = new MerchantValidator

  def validateUpsertion(
      id: UUID,
      upsertion: StoreUpdate,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[ValidatedData[StoreUpdate]] = {
    Future
    val validCoreIdsR = validateCoreIds(upsertion)
    val validMerchantR = merchantValidator.accessOneById(context.merchantId)
    val validLocationR = validateLocationIdUnique(id, upsertion)
    val validUrlSlugForMerchantR = validateUrlSlugUniqueForMerchant(id, upsertion)
    for {
      validCoreIds <- validCoreIdsR
      validMerchant <- validMerchantR
      validLocation <- validLocationR
      validUrlSlugForMerchant <- validUrlSlugForMerchantR
    } yield ValidatedData.combine(validCoreIds, validMerchant, validLocation, validUrlSlugForMerchant) {
      case _ => upsertion
    }
  }

  private def validateCoreIds(upsertion: StoreUpdate)(implicit user: UserContext): Future[ValidatedData[Unit]] = {
    val heroImageIds = upsertion.heroImageUrls.getOrElse(Seq.empty).map(_.imageUploadId)
    val logoImageIds = upsertion.logoImageUrls.getOrElse(Seq.empty).map(_.imageUploadId)
    val coreIds = CoreIds(
      locationIds = upsertion.locationId.toSeq,
      catalogIds = upsertion.catalogId.toSeq,
      imageUploadIds = Map(ImageType.StoreHero -> heroImageIds, ImageType.StoreLogo -> logoImageIds),
    )
    validateCoreIds(coreIds)
  }

  private def validateLocationIdUnique(
      id: UUID,
      upsertion: StoreUpdate,
    )(implicit
      user: UserContext,
    ): Future[ValidatedData[StoreUpdate]] =
    upsertion.locationId match {
      case None => Future.successful(ValidatedData.success(upsertion))
      case Some(locationId) =>
        dao.existsLocationId(idToExclude = id, locationId).map { exists =>
          if (exists) ValidatedData.failure(InvalidStoreLocationAssociation(locationId))
          else ValidatedData.success(upsertion)
        }
    }

  private def validateUrlSlugUniqueForMerchant(
      id: UUID,
      upsertion: StoreUpdate,
    )(implicit
      user: UserContext,
    ): Future[ValidatedData[StoreUpdate]] =
    upsertion.urlSlug match {
      case None => Future.successful(ValidatedData.success(upsertion))
      case Some(urlSlug) =>
        dao.existsMerchantIdAndUrlSlug(idToExclude = id, user.merchantId, urlSlug).map { exists =>
          if (exists) ValidatedData.failure(UrlSlugAlreadyTaken(urlSlug))
          else ValidatedData.success(upsertion)
        }
    }
}
