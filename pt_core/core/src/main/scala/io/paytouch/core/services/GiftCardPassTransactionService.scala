package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.GiftCardPassTransactionConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.GiftCardPassTransactionRecord
import io.paytouch.core.entities.{ UserContext, GiftCardPassTransaction => GiftCardPassTransactionEntity }

import scala.concurrent._

class GiftCardPassTransactionService(implicit val ec: ExecutionContext, val daos: Daos)
    extends GiftCardPassTransactionConversions {

  type Record = GiftCardPassTransactionRecord
  type Entity = GiftCardPassTransactionEntity

  protected val dao = daos.giftCardPassTransactionDao

  def findAllPerGiftCardId(giftCardIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    dao
      .findByGiftCardPassIds(giftCardIds)
      .map(_.groupBy(_.giftCardPassId).transform((_, v) => toSeqEntity(v)))
}
