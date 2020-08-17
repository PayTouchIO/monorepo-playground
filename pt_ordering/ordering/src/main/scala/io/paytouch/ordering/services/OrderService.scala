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
import io.paytouch.ordering.services.features._

import scala.concurrent.{ ExecutionContext, Future }
import io.paytouch.ordering.entities.ApiResponse
import io.paytouch.ordering.errors.ClientError

class OrderService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends FindAllByMerchantIdFromCoreFeature
       with FindByMerchantIdAndIdFromCoreFeature
       with LazyLogging {
  type Entity = Order
  type Filters = (UUID)

  def findCoreEntities(filters: Filters)(implicit authToken: Authorization): Future[CoreApiResponse[Seq[Entity]]] = {
    val (tableId) = filters
    ptCoreClient.ordersListByTable(tableId, open = Some(true))
  }

  def findCoreEntityById(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Entity]] =
    ptCoreClient.ordersGet(id)

  def findByTableId(tableId: UUID, merchantId: UUID) =
    findAll(filters = tableId, merchantId)
}
