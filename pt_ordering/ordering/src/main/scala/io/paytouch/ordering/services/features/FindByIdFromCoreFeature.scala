package io.paytouch.ordering.services.features

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.headers.Authorization

import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.utils.Implicits

trait FindByIdFromCoreFeature extends FindByMerchantIdAndIdFromCoreFeature {
  def getStoreContext(id: UUID): Future[Option[StoreContext]]
}

trait FindByMerchantIdAndIdFromCoreFeature extends Implicits {
  type Entity

  def ptCoreClient: PtCoreClient

  protected def findCoreEntityById(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Entity]]

  def findByOptId(optId: Option[UUID], merchantId: UUID): Future[Option[Entity]] =
    optId match {
      case Some(id) => findById(id, merchantId)
      case None     => Future.successful(None)
    }

  def findById(id: UUID)(implicit storeContext: StoreContext): Future[Option[Entity]] =
    findById(id = id, merchantId = storeContext.merchantId)

  def findById(id: UUID, merchantId: UUID): Future[Option[Entity]] = {
    implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
    findCoreEntityById(id).map(_.toOption.map(_.data))
  }
}
