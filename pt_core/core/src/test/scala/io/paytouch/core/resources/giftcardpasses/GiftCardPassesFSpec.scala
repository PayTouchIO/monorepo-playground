package io.paytouch.core.resources.giftcardpasses

import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.utils._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class GiftCardPassesFSpec extends FSpec {
  abstract class GiftCardPassResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val giftCardPassDao = daos.giftCardPassDao
    val giftCardPassTransactionDao = daos.giftCardPassTransactionDao

    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val giftCard = Factory.giftCard(giftCardProduct).create
    val order = Factory.order(merchant).create
    val orderItem = Factory.orderItem(order).create

    def assertResponse(
        entity: GiftCardPass,
        record: GiftCardPassRecord,
        transactions: Seq[GiftCardPassTransactionRecord] = Seq.empty,
      ) = {
      entity.id ==== record.id
      entity.lookupId ==== record.lookupId
      entity.giftCardId ==== record.giftCardId
      entity.orderItemId ==== record.orderItemId
      entity.originalBalance.amount ==== record.originalAmount
      entity.balance.amount ==== record.balanceAmount
      entity.passPublicUrls.ios ==== record.iosPassPublicUrl
      entity.passPublicUrls.android ==== record.androidPassPublicUrl
      entity.passInstalledAt ==== record.passInstalledAt
      entity.recipientEmail ==== record.recipientEmail
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
      entity.transactions.getOrElse(Seq.empty).map(_.id) should containTheSameElementsAs(transactions.map(_.id))

      entity.transactions.getOrElse(Seq.empty).foreach { transaction =>
        val transactionRecord = transactions.find(_.id == transaction.id).get
        assertTransactionResponse(transaction, transactionRecord)
      }
    }

    private def assertTransactionResponse(entity: GiftCardPassTransaction, record: GiftCardPassTransactionRecord) = {
      entity.id ==== record.id
      entity.total.amount ==== record.totalAmount
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
    }
  }
}
