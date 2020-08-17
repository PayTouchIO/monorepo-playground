package io.paytouch.ordering.services

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.headers.Authorization

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCard
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.services.features.FindAllByMerchantIdFromCoreFeature

final class GiftCardService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends FindAllByMerchantIdFromCoreFeature
       with LazyLogging {
  final override type Entity = GiftCard
  final override type Filters = Unit

  final override def findCoreEntities(
      filters: Filters,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Entity]]] =
    ptCoreClient.giftCardsList
}
