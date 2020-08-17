package io.paytouch.ordering.services.features

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.entities.{ ApiResponse, StoreContext }
import io.paytouch.ordering.utils.Implicits

import scala.concurrent.Future

trait FindAllFromCoreFeature extends FindAllByMerchantIdFromCoreFeature {
  def getStoreContext(filters: Filters): Future[Option[StoreContext]]

  def findAll(filters: Filters)(implicit storeContext: StoreContext): Future[Seq[Entity]] =
    findAll(filters, storeContext.merchantId)
}

trait FindAllByMerchantIdFromCoreFeature extends LazyLogging with Implicits {
  type Entity
  type Filters

  def ptCoreClient: PtCoreClient

  protected def findCoreEntities(
      filters: Filters,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Entity]]]

  def findAll(filters: Filters, merchantId: UUID): Future[Seq[Entity]] = {
    implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
    findCoreEntities(filters).map {
      case Right(ApiResponse(data, _)) => data
      case Left(error) =>
        val className = this.getClass.getSimpleName
        val errorMsg =
          s"""Error while performing findAll for $className
             |(params: filters $filters and merchant $merchantId).
             |Returning empty sequence. [${error.uri} -> ${error.errors}]""".stripMargin
        logger.error(errorMsg)
        Seq.empty
    }
  }
}
