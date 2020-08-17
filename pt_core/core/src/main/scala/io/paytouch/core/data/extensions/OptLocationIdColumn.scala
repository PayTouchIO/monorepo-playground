package io.paytouch.core.data.extensions

import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.data.tables.SlickTable
import slick.lifted.Rep

trait OptLocationIdColumn {
  def locationId: Rep[Option[UUID]]
}
