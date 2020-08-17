package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.conversions.CustomerLocationConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.GlobalCustomerUpdate
import io.paytouch.core.entities._

class GlobalCustomerService(implicit val ec: ExecutionContext, val daos: Daos) extends CustomerLocationConversions {
  protected val dao = daos.globalCustomerDao

  def findByIds(customerIds: Seq[UUID])(implicit user: UserContext): Future[Seq[GlobalCustomer]] =
    dao.findByIds(customerIds).map(fromRecordsToEntities)

  def convertToGlobalCustomerUpdate(optId: Option[UUID], email: Option[String]): Future[GlobalCustomerUpdate] =
    dao.findByEmail(email).map { record =>
      val id = record.map(_.id).orElse(optId).getOrElse(UUID.randomUUID)

      toGlobalCustomerUpdate(id, email)
    }
}
