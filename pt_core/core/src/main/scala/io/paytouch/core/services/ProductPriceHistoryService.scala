package io.paytouch.core.services

import io.paytouch.core.conversions.ProductPriceHistoryConversions
import io.paytouch.core.data.daos.{ Daos, ProductPriceHistoryDao }
import io.paytouch.core.data.model.ProductPriceHistoryRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.ProductHistoryFilters
import io.paytouch.core.services.features.FindAllFeature

import scala.concurrent._

class ProductPriceHistoryService(
    val locationService: LocationService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ProductPriceHistoryConversions
       with FindAllFeature {

  type Dao = ProductPriceHistoryDao
  type Entity = ProductPriceHistory
  type Expansions = NoExpansions
  type Filters = ProductHistoryFilters
  type Record = ProductPriceHistoryRecord

  protected val dao = daos.productPriceHistoryDao

  def defaultFilters =
    throw new IllegalStateException("No default filter available as product id is a mandatory filter")

  def groupByLocationByChange(locations: Seq[Location], items: Seq[Record]): Map[Record, Location] =
    items.flatMap(item => locations.find(_.id == item.locationId).map(location => (item, location))).toMap

  def groupByUserByChange(users: Seq[UserInfo], items: Seq[Record]): Map[Record, UserInfo] =
    items.flatMap(item => users.find(_.id == item.userId).map(user => (item, user))).toMap

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val locationsResp = locationService.findByIds(records.map(_.locationId))
    val usersResp = userService.getUserInfoByIds(records.map(_.userId))
    for {
      locations <- locationsResp
      users <- usersResp
    } yield {
      val groupedLocations = groupByLocationByChange(locations, records)
      val groupedUsers = groupByUserByChange(users, records)
      fromRecordsToPriceEntities(records, groupedLocations, groupedUsers)
    }
  }
}
