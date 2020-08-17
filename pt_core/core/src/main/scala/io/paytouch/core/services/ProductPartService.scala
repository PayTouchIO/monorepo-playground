package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.conversions.ProductPartConversions
import io.paytouch.core.data.daos.{ Daos, ProductPartDao }
import io.paytouch.core.data.model.{ ArticleUpdate, ProductPartRecord }
import io.paytouch.core.data.model.upsertions.ProductPartUpsertion
import io.paytouch.core.entities.{ ProductPart, ProductPartAssignment, UserContext }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.ProductPartFilters
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ProductPartValidator

class ProductPartService(val articleService: ArticleService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends ProductPartConversions
       with FindAllFeature {
  type Dao = ProductPartDao
  type Entity = ProductPart
  type Expansions = NoExpansions
  type Filters = ProductPartFilters
  type Record = ProductPartRecord

  protected val dao = daos.productPartDao
  protected val validator = new ProductPartValidator

  def assignProductParts(
      productId: UUID,
      assignments: Seq[ProductPartAssignment],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator
      .validateProductPartAssignments(productId, assignments)
      .mapNested { _ =>
        convertToProductUpsertion(productId, assignments)
          .pipe(dao.bulkUpsert)
          .void
      }

  private def convertToProductUpsertion(
      productId: UUID,
      assignments: Seq[ProductPartAssignment],
    )(implicit
      user: UserContext,
    ): ProductPartUpsertion =
    ProductPartUpsertion(
      product = convertToArticleUpdate(productId, assignments),
      productParts = fromUpsertionsToUpdates(productId, assignments),
    )

  private def convertToArticleUpdate(productId: UUID, assignments: Seq[ProductPartAssignment]): ArticleUpdate = {
    val hasParts = assignments.toSet.nonEmpty
    ArticleUpdate.empty.copy(id = Some(productId), hasParts = Some(hasParts))
  }

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    for {
      parts <- articleService.findByIds(records.map(_.partId))
    } yield fromRecordsAndOptionsToEntities(records, parts)

}
