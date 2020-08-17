package io.paytouch.ordering.utils

import io.paytouch.ordering.utils.db.LiquibaseSupportCommon

trait LiquibaseSupportProvider {
  val liquibaseSupport = LiquibaseSupport
}

object LiquibaseSupport extends LiquibaseSupportCommon {
  lazy val configFile = ConfiguredTestDatabaseProvider.configFile
}
