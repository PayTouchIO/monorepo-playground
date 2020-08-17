package io.paytouch.ordering.services

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.{ Merchant, StoreContext }
import io.paytouch.ordering.services.features.{ FindAllByMerchantIdFromCoreFeature, FindByIdFromCoreFeature }
import io.paytouch.ordering.withTag

import scala.concurrent.{ ExecutionContext, Future }

class LocationService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends FindByIdFromCoreFeature
       with FindAllByMerchantIdFromCoreFeature {

  type Entity = Location
  type Filters = Unit

  type RelId = (UUID withTag Location, UUID withTag Merchant)

  val storeDao = daos.storeDao

  def getStoreContext(id: UUID): Future[Option[StoreContext]] =
    storeDao.findStoreContextByLocationId(id)

  protected def findCoreEntityById(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Entity]] =
    ptCoreClient.locationsGet(id)

  protected def findCoreEntities(
      filters: Filters,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Entity]]] = ptCoreClient.locationsList

  def findByStoreContext()(implicit storeContext: StoreContext): Future[Option[Entity]] =
    findById(id = storeContext.locationId, merchantId = storeContext.merchantId)

  def findAll(merchantId: UUID): Future[Seq[Entity]] = findAll(filters = (), merchantId)

  def findPerRelId(ids: Seq[RelId]): Future[Map[RelId, Option[Entity]]] = {
    val locationIds = ids.map { case (l, _) => l }.distinct
    val merchantIds = ids.map { case (_, m) => m }.distinct

    if (locationIds.nonEmpty && merchantIds.nonEmpty) {
      val merchantId = merchantIds.head // TODO - can we improve this?
      findAll(merchantId).map(locations => groupLocationByRelIds(locations, ids))
    }
    else {
      val description = s"locationId -> $locationIds; merchantIds -> $merchantIds"
      val errorMsg =
        s"""Data missing from GraphQLContext!
           |Expected to find at least a locationId and merchantId. 
           |Found: $description""".stripMargin
      logger.error(errorMsg)
      Future.successful(Map.empty)
    }
  }

  def findByMerchantIdAndId(merchantId: UUID, locationId: UUID): Future[Option[Entity]] =
    findById(id = locationId, merchantId = merchantId)

  private def groupLocationByRelIds(locations: Seq[Location], relIds: Seq[RelId]): Map[RelId, Option[Entity]] =
    relIds.map { relId =>
      val (locationId, _) = relId
      relId -> locations.find(_.id == locationId)
    }.toMap

}
