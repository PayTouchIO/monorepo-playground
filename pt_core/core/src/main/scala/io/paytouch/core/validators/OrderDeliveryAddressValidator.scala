package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, OrderDeliveryAddressDao }
import io.paytouch.core.data.model._
import io.paytouch.core.errors._
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class OrderDeliveryAddressValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultValidator[OrderDeliveryAddressRecord] {
  type Record = OrderDeliveryAddressRecord
  type Dao = OrderDeliveryAddressDao

  protected val dao = daos.orderDeliveryAddressDao
  val validationErrorF = InvalidOrderDeliveryAddressIds(_)
  val accessErrorF = NonAccessibleOrderDeliveryAddressIds(_)
}
