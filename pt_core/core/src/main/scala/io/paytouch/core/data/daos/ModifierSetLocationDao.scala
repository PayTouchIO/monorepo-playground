package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ModifierSetLocationRecord, ModifierSetLocationUpdate }
import io.paytouch.core.data.tables.ModifierSetLocationsTable

import scala.concurrent._

class ModifierSetLocationDao(
    modifierSetDao: => ModifierSetDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {

  type Record = ModifierSetLocationRecord
  type Update = ModifierSetLocationUpdate
  type Table = ModifierSetLocationsTable

  val table = TableQuery[Table]

  val itemDao = modifierSetDao

  def findOneByModifierSetIdAndLocationId(modifierSetId: UUID, locationId: UUID): Future[Option[Record]] =
    run(queryFindByItemIdAndLocationId(modifierSetId, locationId).result.headOption)

  def queryByRelIds(modifierSetLocationUpdate: Update) = {
    require(
      modifierSetLocationUpdate.modifierSetId.isDefined,
      "ModifierSetLocationDao - Impossible to find by modifier set id and location id without a modifier set id",
    )
    require(
      modifierSetLocationUpdate.locationId.isDefined,
      "ModifierSetLocationDao - Impossible to find by modifier set id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(
      modifierSetLocationUpdate.modifierSetId.get,
      modifierSetLocationUpdate.locationId.get,
    )
  }

  def queryBulkUpsertAndDeleteTheRestByModifierSetId(modifierSetLocations: Seq[Update], modifierSetId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(modifierSetLocations, t => t.modifierSetId === modifierSetId)
}
