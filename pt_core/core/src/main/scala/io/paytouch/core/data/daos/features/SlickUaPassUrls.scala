package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.daos._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.UaPassColumns
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.utils.UtcTime
import slick.lifted.Rep

import scala.concurrent._

trait SlickUaPassUrls extends SlickCommonDao {
  type Table <: SlickMerchantTable[Record] with UaPassColumns

  def updateIosPassPublicUrl(id: UUID, passPublicUrl: String): Future[Option[Record]] =
    updatePassPublicUrl(id, _.iosPassPublicUrl, passPublicUrl)

  def updateAndroidPassPublicUrl(id: UUID, passPublicUrl: String): Future[Option[Record]] =
    updatePassPublicUrl(id, _.androidPassPublicUrl, passPublicUrl)

  private def updatePassPublicUrl(
      id: UUID,
      fieldSelector: Table => Rep[Option[String]],
      passPublicUrl: String,
    ): Future[Option[Record]] =
    runWithTransaction {
      table
        .filterById(id)
        .map(o => fieldSelector(o) -> o.updatedAt)
        .update(Some(passPublicUrl), UtcTime.now)
        .flatMap(_ => queryFindById(id))
    }

  def updatedPassInstalledAtField(id: UUID) =
    runWithTransaction {
      val now = UtcTime.now

      table
        .filterById(id)
        .filter(_.passOptInColumn.isEmpty)
        .map(o => o.passOptInColumn -> o.updatedAt)
        .update(Some(now), now)
        .flatMap(_ => queryFindById(id))
    }
}
