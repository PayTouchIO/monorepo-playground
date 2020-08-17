package io.paytouch.ordering.errors

import java.util.UUID

final case class NonAccessibleIds(
    message: String,
    code: String,
    values: Seq[UUID],
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends NotFound

trait NonAccessibleIdsCreation extends ErrorMessageCodeForIds[NonAccessibleIds] {
  def apply(ids: Seq[UUID]): NonAccessibleIds = NonAccessibleIds(message = message, code = code, values = ids)
}

object NonAccessibleCartIds extends NonAccessibleIdsCreation {
  val message = "Cart ids not accessible"
  val code = "NonAccessibleCartIds"
}

object NonAccessibleCartItemIds extends NonAccessibleIdsCreation {
  val message = "Cart item ids not accessible"
  val code = "NonAccessibleCartItemIds"
}

object NonAccessibleMerchantIds extends NonAccessibleIdsCreation {
  val message = "Merchant ids not accessible. Make sure to create a merchant slug"
  val code = "NonAccessibleMerchantIds"
}

object NonAccessibleStoreIds extends NonAccessibleIdsCreation {
  val message = "Store ids not accessible"
  val code = "NonAccessibleStoreIds"
}

object NonAccessiblePaymentIntentIds extends NonAccessibleIdsCreation {
  val message = "Payment intent ids not accessible"
  val code = "NonAccessiblePaymentIntentIds"
}
