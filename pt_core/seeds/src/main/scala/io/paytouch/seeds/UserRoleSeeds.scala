package io.paytouch.seeds

import scala.concurrent._

import io.paytouch.core.data.model._

object UserRoleSeeds extends Seeds {
  lazy val userRoleDao = daos.userRoleDao

  def load(email: String, merchant: MerchantRecord): Future[Seq[UserRoleRecord]] = {
    val userRoles = UserRoleUpdate.defaults(merchant.id, enums.SetupType.Paytouch)
    userRoleDao.bulkUpsert(userRoles).extractRecords(email)
  }
}
