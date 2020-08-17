package io.paytouch.ordering.utils

import io.paytouch.ordering.data.daos.Daos
import org.specs2.specification.Scope

abstract class DaoSpec extends PaytouchSpec with LiquibaseSupportProvider with ConfiguredTestDatabase {

  implicit lazy val daos = new Daos

  abstract class DaoSpecContext extends Scope
}
