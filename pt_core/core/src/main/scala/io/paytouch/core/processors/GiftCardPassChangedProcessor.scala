package io.paytouch.core.processors

import scala.concurrent._
import scala.util.{ Failure, Success }

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities._
import io.paytouch.core.services.GiftCardPassService

class GiftCardPassChangedProcessor(
    giftCardPassService: GiftCardPassService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: GiftCardPassChanged => processGiftCardPassChanged(msg)
  }

  private def processGiftCardPassChanged(msg: GiftCardPassChanged): Future[Unit] = {
    val giftCardPass = msg.payload.data
    implicit val u: UserContext = msg.payload.userContext
    giftCardPassService.upsertPass(giftCardPass).void
  }
}
