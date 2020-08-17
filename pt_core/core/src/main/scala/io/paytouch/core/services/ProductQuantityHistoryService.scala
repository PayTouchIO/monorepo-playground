package io.paytouch.core.services

import io.paytouch.core.conversions.ProductQuantityHistoryConversions
import io.paytouch.core.data.daos.{ Daos, ProductQuantityHistoryDao }
import io.paytouch.core.data.model.ProductQuantityHistoryRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.ProductHistoryFilters
import io.paytouch.core.services.features.FindAllFeature

import scala.concurrent._

class ProductQuantityHistoryService(
    val locationService: LocationService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ProductQuantityHistoryConversions
       with FindAllFeature {

  type Dao = ProductQuantityHistoryDao
  type Entity = ProductQuantityHistory
  type Expansions = NoExpansions
  type Filters = ProductHistoryFilters
  type Record = ProductQuantityHistoryRecord

  protected val dao = daos.productQuantityHistoryDao

  def groupByLocationByChange(locations: Seq[Location], items: Seq[Record]): Map[Record, Location] =
    items.flatMap(item => locations.find(_.id == item.locationId).map(location => (item, location))).toMap

  def groupByUserByChange(users: Seq[UserInfo], items: Seq[Record]): Map[Record, UserInfo] =
    items.flatMap(item => users.find(user => item.userId.contains(user.id)).map(user => (item, user))).toMap

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val locationsResp = locationService.findByIds(records.map(_.locationId))
    val usersResp = userService.getUserInfoByIds(records.flatMap(_.userId))
    for {
      locations <- locationsResp
      users <- usersResp
    } yield {
      val groupedLocations = groupByLocationByChange(locations, records)
      val groupedUsers = groupByUserByChange(users, records)
      fromRecordsToEntities(records, groupedLocations, groupedUsers)
    }
  }
}
