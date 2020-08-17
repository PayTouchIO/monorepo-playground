package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import slick.jdbc.GetResult

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductVariantOptionRecord, ProductVariantOptionUpdate }
import io.paytouch.core.data.tables.ProductVariantOptionsTable
import io.paytouch.core.entities.VariantOptionWithType

class ProductVariantOptionDao(
    val variantOptionDao: VariantOptionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {
  type Record = ProductVariantOptionRecord
  type Update = ProductVariantOptionUpdate
  type Table = ProductVariantOptionsTable

  val table = TableQuery[Table]

  def queryFindByProductIdAndVariantOptionId(productId: UUID, variantOptionId: UUID) =
    table.filter(t => t.productId === productId && t.variantOptionId === variantOptionId)

  def queryByRelIds(productVariantOption: Update) = {
    require(
      productVariantOption.productId.isDefined,
      "ProductVariantOptionDao - Impossible to find by product id and variant option id without a product id",
    )

    require(
      productVariantOption.variantOptionId.isDefined,
      "ProductVariantOptionDao - Impossible to find by product id and variant option id without a variant option id",
    )

    queryFindByProductIdAndVariantOptionId(productVariantOption.productId.get, productVariantOption.variantOptionId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByProductId(productVariantOptions: Seq[Update], productId: UUID) =
    queryBulkUpsertAndDeleteTheRestByProductIds(productVariantOptions, Seq(productId))

  def queryBulkUpsertAndDeleteTheRestByProductIds(productVariantOptions: Seq[Update], productIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(productVariantOptions, t => t.productId inSet productIds)

  def bulkUpsertAndDeleteTheRestByProductIds(productVariantOptions: Seq[Update], productIds: Seq[UUID]) =
    runWithTransaction(queryBulkUpsertAndDeleteTheRestByProductIds(productVariantOptions, productIds))

  def queryFindByProductIds(productIds: Seq[UUID]) = table.filter(_.productId inSet productIds)

  def findByProductId(productId: UUID): Future[Seq[Record]] =
    findByProductIds(Seq(productId))

  def findByProductIds(productIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByProductIds(productIds)
        .result
        .pipe(run)

  def findByVariantOptionIds(variantOptionIds: Seq[UUID]): Future[Seq[Record]] =
    if (variantOptionIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(_.variantOptionId inSet variantOptionIds)
        .result
        .pipe(run)

  def findVariantOptionsByVariantIdsGroupedByVariantIds(
      variantIds: Seq[UUID],
    ): Future[Map[UUID, Seq[VariantOptionWithType]]] =
    if (variantIds.isEmpty)
      Future.successful(Map.empty)
    else {
      implicit val getMapResult = GetResult(r =>
        (r.nextUUID(), VariantOptionWithType(r.nextUUID(), r.nextString(), r.nextString(), r.nextInt(), r.nextInt())),
      )

      sql"""SELECT pvo.product_id, vo.id, vo.name, vot.name, vo.position, vot.position
              FROM product_variant_options pvo
              JOIN variant_options vo
              ON pvo.variant_option_id = vo.id
              JOIN variant_option_types vot
              ON vo.variant_option_type_id = vot.id
              WHERE pvo.product_id IN (#${variantIds.asInParametersList})
            ;"""
        .as[(UUID, VariantOptionWithType)]
        .pipe(run)
        .map(
          _.groupBy {
            case (variantId, _) => variantId
          }.transform { (_, v) =>
            v.map {
              case (_, variantOption) => variantOption
            }
          },
        )
    }
}
