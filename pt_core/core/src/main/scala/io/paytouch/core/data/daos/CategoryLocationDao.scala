package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ CategoryLocationRecord, CategoryLocationUpdate }
import io.paytouch.core.data.tables.CategoryLocationsTable

class CategoryLocationDao(
    categoryDao: => CategoryDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {
  type Record = CategoryLocationRecord
  type Update = CategoryLocationUpdate
  type Table = CategoryLocationsTable

  val table = TableQuery[Table]

  val itemDao = categoryDao

  def queryByRelIds(categoryLocationUpdate: Update) = {
    require(
      categoryLocationUpdate.categoryId.isDefined,
      "CategoryLocationDao - Impossible to find by category id and location id without a category id",
    )

    require(
      categoryLocationUpdate.locationId.isDefined,
      "CategoryLocationDao - Impossible to find by category id and location id without a location id",
    )

    queryFindByItemIdAndLocationId(categoryLocationUpdate.categoryId.get, categoryLocationUpdate.locationId.get)
  }

  def findByItemIds(categoryIds: Seq[UUID], locationId: Option[UUID]): Future[Seq[Record]] =
    if (categoryIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByItemIds(categoryIds)
        .filter(t => all(locationId.map(lId => t.locationId === lId)))
        .result
        .pipe(run)

  def findOneByCategoryIdAndLocationId(categoryId: UUID, locationId: UUID): Future[Option[Record]] =
    queryFindByItemIdAndLocationId(categoryId, locationId)
      .result
      .headOption
      .pipe(run)
}
