package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, StockDao }
import io.paytouch.core.data.model.StockRecord
import io.paytouch.core.entities.{ StockUpdate, UserContext }
import io.paytouch.core.errors.{ InvalidStockIds, NonAccessibleStockIds, StockProductLocationDuplication }
import io.paytouch.core.utils._
import io.paytouch.core.validators.features.ValidatorWithRelIds

import scala.concurrent._

class StockValidator(implicit val ec: ExecutionContext, val daos: Daos) extends ValidatorWithRelIds[StockRecord] {

  type Record = StockRecord
  type Dao = StockDao

  protected val dao = daos.stockDao
  val validationErrorF = InvalidStockIds(_)
  val accessErrorF = NonAccessibleStockIds(_)

  val articleValidator = new StorableArticleValidator
  val productLocationValidator = new ProductLocationValidator

  def recordFinderByRelIds(relIds: Seq[(UUID, UUID)])(implicit user: UserContext): Future[Seq[StockRecord]] = {
    val productIds = relIds.map { case (productId, _) => productId }
    val locationIds = relIds.map { case (_, locationId) => locationId }
    dao.findByProductIdsAndLocationIds(productIds = productIds, locationIds = locationIds)
  }

  def validateStockUpsertions(
      upsertions: Seq[StockUpdate],
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[Seq[StockRecord]]] = {
    val prodLocRels = upsertions.map(s => (s.productId, s.locationId))
    val productIds = upsertions.map(_.productId)
    for {
      products <- articleValidator.accessByIds(productIds)
      existingProdLocs <- productLocationValidator.validateProductLocationsByRelationIds(prodLocRels)
      nonDupProdLocs <- validateProductLocationRelations(prodLocRels)
      stocks <- getValidByRelIds(prodLocRels)
    } yield Multiple.combine(products, existingProdLocs, nonDupProdLocs) { case _ => stocks }
  }

  private def validateProductLocationRelations(rel: Seq[(UUID, UUID)]): Future[Multiple.ErrorsOr[Seq[(UUID, UUID)]]] =
    Future.successful {
      val duplicates = rel.groupBy(identity).collect { case (x, ys) if ys.size > 1 => x }.toSeq
      if (duplicates.isEmpty)
        Multiple.success(rel)
      else
        Multiple.failure(StockProductLocationDuplication(duplicates))
    }
}
