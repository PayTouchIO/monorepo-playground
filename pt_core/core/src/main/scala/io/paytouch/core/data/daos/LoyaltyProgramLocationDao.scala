package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LoyaltyProgramLocationRecord, LoyaltyProgramLocationUpdate }
import io.paytouch.core.data.tables.LoyaltyProgramLocationsTable

import scala.concurrent.ExecutionContext

class LoyaltyProgramLocationDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickItemLocationDao {

  type Record = LoyaltyProgramLocationRecord
  type Update = LoyaltyProgramLocationUpdate
  type Table = LoyaltyProgramLocationsTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.loyaltyProgramId.isDefined,
      "LoyaltyProgramLocationDao - Impossible to find by loyal program id and location id without a loyal program id",
    )
    require(
      upsertion.locationId.isDefined,
      "LoyaltyProgramLocationDao - Impossible to find by loyal program id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(upsertion.loyaltyProgramId.get, upsertion.locationId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByLoyaltyProgramId(
      loyaltyProgramLocationUpdates: Seq[Update],
      loyaltyProgramId: UUID,
    ) =
    queryBulkUpsertAndDeleteTheRestByRelIds(loyaltyProgramLocationUpdates, t => t.loyaltyProgramId === loyaltyProgramId)
}
