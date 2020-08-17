package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.BrandConversions
import io.paytouch.core.data.daos.{ BrandDao, Daos }
import io.paytouch.core.data.model.{ BrandRecord, BrandUpdate => BrandUpdateModel }
import io.paytouch.core.entities.{ BrandCreation, UserContext, Brand => BrandEntity, BrandUpdate => BrandUpdateEntity }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.features.{ CreateAndUpdateFeature, FindAllFeature }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.BrandValidator

import scala.concurrent._

class BrandService(implicit val ec: ExecutionContext, val daos: Daos)
    extends BrandConversions
       with FindAllFeature
       with CreateAndUpdateFeature {

  type Creation = BrandCreation
  type Dao = BrandDao
  type Entity = BrandEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = BrandUpdateModel
  type Record = BrandRecord
  type Update = BrandUpdateEntity
  type Validator = BrandValidator

  protected val dao = daos.brandDao
  protected val validator = new BrandValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    Future.successful(toSeqEntity(records))

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Model]] =
    Future.successful {
      Multiple.success(fromUpsertionToUpdate(id, update))
    }
}
