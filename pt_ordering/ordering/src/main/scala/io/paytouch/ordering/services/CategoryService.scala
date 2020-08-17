package io.paytouch.ordering.services

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering._
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.services.features.FindAllFromCoreFeature

import scala.concurrent.{ ExecutionContext, Future }

class CategoryService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends FindAllFromCoreFeature
       with LazyLogging {

  type Entity = Category

  type CatalogId = UUID withTag Catalog
  type LocationId = UUID withTag Location
  type Filters = (CatalogId, LocationId)

  val storeDao = daos.storeDao

  def getStoreContext(filters: Filters): Future[Option[StoreContext]] = {
    val (_, locationId) = filters
    storeDao.findStoreContextByLocationId(locationId = locationId)
  }

  protected def findCoreEntities(
      filters: Filters,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Entity]]] =
    ptCoreClient.catalogCategoriesList(filters._1)

  def findAll(
      catalogId: UUID,
      locationId: UUID,
      merchantId: UUID,
    ): Future[Seq[Entity]] = {
    val filters = (catalogId.taggedWith[Catalog], locationId.taggedWith[Location])
    findAll(filters = filters, merchantId)
  }
}
