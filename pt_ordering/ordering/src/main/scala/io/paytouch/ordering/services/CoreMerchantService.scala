package io.paytouch.ordering.services

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.withTag

import scala.concurrent.{ ExecutionContext, Future }

class CoreMerchantService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends LazyLogging {
  type Entity = CoreMerchant
  type Id = UUID withTag CoreMerchant

  def findCoreEntityByMerchantId(merchantId: UUID): Future[CoreApiResponse[Entity]] = {
    implicit val token =
      ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
    ptCoreClient.merchantsMe()
  }
}
