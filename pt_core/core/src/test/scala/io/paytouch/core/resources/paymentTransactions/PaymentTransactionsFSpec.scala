package io.paytouch.core.resources.paymentTransactions

import io.paytouch.core.entities.{ PurchaseOrder => PurchaseOrderEntity }
import io.paytouch.core.utils._

abstract class PaymentTransactionsFSpec extends FSpec {

  abstract class PaymentTransactionsResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val paymentTransactionDao = daos.paymentTransactionDao
  }
}
