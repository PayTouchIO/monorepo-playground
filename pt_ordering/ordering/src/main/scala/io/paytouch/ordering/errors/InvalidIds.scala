package io.paytouch.ordering.errors

import java.util.UUID

final case class InvalidIds(
    message: String,
    code: String,
    values: Seq[UUID],
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends NotFound

trait InvalidIdsCreation extends ErrorMessageCodeForIds[InvalidIds] {
  def apply(ids: Seq[UUID]): InvalidIds = InvalidIds(message = message, code = code, values = ids)
}

object InvalidCartIds extends InvalidIdsCreation {
  val message = "Cart ids not valid"
  val code = "InvalidCartIds"
}

object InvalidCartItemIds extends InvalidIdsCreation {
  val message = "Cart item ids not valid"
  val code = "InvalidCartItemIds"
}

object InvalidMerchantIds extends InvalidIdsCreation {
  val message = "Merchant ids not valid. A merchant slug already exists"
  val code = "InvalidMerchantIds"
}

object InvalidStoreIds extends InvalidIdsCreation {
  val message = "Store ids not valid"
  val code = "InvalidStoreIds"
}

object InvalidPaymentIntentIds extends InvalidIdsCreation {
  val message = "Payment intent ids not valid"
  val code = "InvalidPaymentIntentIds"
}

object InvalidOrderItemIds extends InvalidIdsCreation {
  val message = "Order item ids not valid"
  val code = "InvalidOrderItemIds"
}
