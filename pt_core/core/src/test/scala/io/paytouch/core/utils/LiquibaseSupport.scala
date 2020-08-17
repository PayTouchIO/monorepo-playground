package io.paytouch.core.utils

import io.paytouch.core.data.daos.ConfiguredDatabaseProvider
import io.paytouch.utils.LiquibaseSupportCommon

trait LiquibaseSupportProvider {
  val liquibaseSupport = LiquibaseSupport
}

object LiquibaseSupport extends LiquibaseSupportCommon {
  lazy val dbHelper = ConfiguredDatabaseProvider.dbHelper
}
