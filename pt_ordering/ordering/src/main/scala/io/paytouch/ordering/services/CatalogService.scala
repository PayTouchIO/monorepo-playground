package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import com.typesafe.scalalogging.LazyLogging

import akka.http.scaladsl.model.headers.Authorization

import io.paytouch.ordering._
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.entities.Catalog
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities._
import io.paytouch.ordering.services.features.FindByIdFromCoreFeature

class CatalogService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends FindByIdFromCoreFeature
       with LazyLogging {
  type Entity = Catalog
  type Id = (UUID withTag Catalog, UUID withTag Merchant)

  val storeDao = daos.storeDao

  def getStoreContext(id: UUID): Future[Option[StoreContext]] =
    storeDao.findStoreContextByCatalogId(id)

  protected def findCoreEntityById(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Entity]] =
    ptCoreClient.catalogsGet(id)

  def findAllPerId(ids: Seq[Id]): Future[Map[Id, Entity]] = {
    val allCatalogIds = ids.map { case (p, _) => p }.distinct
    val merchantIds = ids.map { case (_, m) => m }.distinct

    if (allCatalogIds.nonEmpty && merchantIds.nonEmpty) {
      val merchantId = merchantIds.head
      implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
      ptCoreClient.catalogsListByIds(allCatalogIds).map {
        case Right(ApiResponse(data, _)) =>
          data.map(p => (p.id.taggedWith[Catalog], merchantId.taggedWith[Merchant]) -> p).toMap
        case Left(error) =>
          val className = this.getClass.getSimpleName
          val errorMsg =
            s"""Error while performing findAll for $className
               |(params: ids[]=$ids and merchant $merchantId).
               |Returning empty sequence. [${error.uri} -> ${error.errors}]""".stripMargin
          logger.error(errorMsg)
          Map.empty
      }
    }
    else {
      val description = s"catalogIds -> $allCatalogIds; merchantIds -> $merchantIds"
      val errorMsg =
        s"""Data missing from GraphQLContext!
           |Expected to find at least a catalogId and merchantId.
           |Found: $description""".stripMargin
      logger.error(errorMsg)
      Future.successful(Map.empty)
    }
  }
}
