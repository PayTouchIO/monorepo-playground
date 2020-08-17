package io.paytouch.core.services

import io.paytouch.core.conversions.GlobalCustomerConversions
import io.paytouch.core.data.daos.Daos

import scala.concurrent.ExecutionContext

class GlobalCustomerLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends GlobalCustomerConversions {

  protected val dao = daos.customerLocationDao

}
