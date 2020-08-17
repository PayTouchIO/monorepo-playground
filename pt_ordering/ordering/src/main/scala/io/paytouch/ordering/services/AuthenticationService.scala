package io.paytouch.ordering.services

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.{ ApiResponse, RapidoOrderContext, StoreContext, UserContext }

import scala.concurrent.{ ExecutionContext, Future }
import io.paytouch.ordering.entities.RapidoOrderContext

class AuthenticationService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos) {

  private val cartDao = daos.cartDao
  private val storeDao = daos.storeDao
  private val merchantDao = daos.merchantDao

  def getUserContext(implicit authToken: Authorization): Future[Option[UserContext]] =
    ptCoreClient.usersContext.map {
      case Right(ApiResponse(coreContext, _)) => Some(UserContext(coreContext, authToken))
      case _                                  => None
    }

  def getStoreContextFromCartId(cartId: UUID): Future[Option[StoreContext]] =
    storeDao.findStoreContextByCartId(cartId)

  def getStoreContextFromStoreId(storeId: UUID): Future[Option[StoreContext]] =
    storeDao.findStoreContextById(storeId)

  def getRapidoOrderContext(merchantId: UUID, orderId: UUID): Future[Option[RapidoOrderContext]] =
    merchantDao.findById(merchantId).flatMap {
      case Some(merchant) =>
        implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)
        ptCoreClient.ordersGet(orderId).map {
          case Right(ApiResponse(order, _)) => Some(RapidoOrderContext(merchant.id, order))
          // Order does not exist
          case _ => None
        }
      // Merchant does not exist, or merchant not setup on ordering
      case _ => Future.successful(None)
    }
}
