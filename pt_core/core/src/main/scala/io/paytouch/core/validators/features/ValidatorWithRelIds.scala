package io.paytouch.core.validators.features

import java.util.UUID

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities.UserContext

import scala.concurrent._

trait ValidatorWithRelIds[Record <: SlickMerchantRecord] extends DefaultValidator[Record] {

  def recordFinderByRelIds(relIds: Seq[(UUID, UUID)])(implicit user: UserContext): Future[Seq[Record]]

  def getValidByRelIds(relIds: Seq[(UUID, UUID)])(implicit user: UserContext): Future[Seq[Record]] =
    recordFinderByRelIds(relIds).map(_.filter(validityCheck))
}
