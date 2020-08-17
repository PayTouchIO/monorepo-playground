package io.paytouch.core.processors

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.{ PrepareGiftCardPassReceiptRequested, SQSMessage }
import io.paytouch.core.services._

import scala.concurrent._

class PrepareGiftCardPassReceiptRequestedProcessor(
    val giftCardPassService: GiftCardPassService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {

  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: PrepareGiftCardPassReceiptRequested => processPrepareGiftCardPassReceiptRequested(msg)
  }

  private def processPrepareGiftCardPassReceiptRequested(msg: PrepareGiftCardPassReceiptRequested): Future[Unit] = {
    val giftCardPass = msg.payload.data
    implicit val userContext = msg.payload.userContext
    giftCardPassService.upsertPass(giftCardPass).map {
      _.foreach(giftCardPassService.sendGiftCardPassReceiptMsg)
    }
  }
}
