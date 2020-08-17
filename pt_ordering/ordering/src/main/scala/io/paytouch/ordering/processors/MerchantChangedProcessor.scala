package io.paytouch.ordering.processors

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.messages.entities._
import io.paytouch.ordering.Result
import io.paytouch.ordering.services._

class MerchantChangedProcessor(
    merchantService: MerchantService,
    storeService: StoreService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {

  type OrderingPaymentProcessorData = (PaymentProcessor, model.PaymentProcessorConfig)
  val merchantDao = daos.merchantDao

  def execute: PartialFunction[PtOrderingMsg[_], Future[Unit]] = {
    case msg: MerchantChanged => processMerchantChanged(msg)
  }

  private def processMerchantChanged(msg: MerchantChanged): Future[Unit] = {
    val payload = msg.payload
    merchantDao.findById(payload.merchantId).flatMap {
      case Some(merchant) =>
        for {
          (_, merchant) <- updatePaymentProcessor(merchant, toOrderingPaymentProcessorData(payload))
          _ <- storeService.enableProcessorPaymentMethodForAllStores(merchant)
        } yield ()

      case None =>
        for {
          urlSlug <- merchantService.generateUrlSlug(payload.data.displayName)
          _ <-
            merchantDao
              .upsert(toCreation(payload.merchantId, urlSlug, toOrderingPaymentProcessorData(payload)))
        } yield ()
    }
  }

  private def toOrderingPaymentProcessorData(payload: MerchantPayload): Option[OrderingPaymentProcessorData] =
    toPaymentProcessorConfigUpsertion(payload.orderingPaymentPaymentProcessorConfigUpsertion)
      .map(payload.data.paymentProcessor -> _)

  private def toPaymentProcessorConfigUpsertion(
      orderingPaymentPaymentProcessorConfigUpsertion: Option[MerchantPayload.OrderingPaymentProcessorConfigUpsertion],
    ): Option[model.PaymentProcessorConfig] = {
    import io.scalaland.chimney.dsl._

    orderingPaymentPaymentProcessorConfigUpsertion match {
      case Some(v: MerchantPayload.OrderingPaymentProcessorConfigUpsertion.WorldpayConfigUpsertion) =>
        v.transformInto[model.WorldpayConfig].some

      case Some(v: MerchantPayload.OrderingPaymentProcessorConfigUpsertion.StripeConfigUpsertion) =>
        v.transformInto[model.StripeConfig].some

      // temporary fix until we stop core from sending none
      case _ =>
        model.PaytouchConfig().some
    }
  }

  private def toCreation(
      id: UUID,
      urlSlug: String,
      paymentProcessorData: Option[OrderingPaymentProcessorData],
    ) =
    model.MerchantUpdate(
      id = Some(id),
      urlSlug = Some(urlSlug),
      paymentProcessor = paymentProcessorData.map(_._1),
      paymentProcessorConfig = paymentProcessorData.map(_._2),
    )

  private def updatePaymentProcessor(
      merchant: model.MerchantRecord,
      paymentProcessorData: Option[OrderingPaymentProcessorData],
    ): Future[Result[model.MerchantRecord]] =
    merchantDao.upsert(toUpdate(merchant, paymentProcessorData))

  private def toUpdate(merchant: model.MerchantRecord, paymentProcessorData: Option[OrderingPaymentProcessorData]) =
    model.MerchantUpdate(
      id = Some(merchant.id),
      urlSlug = None,
      paymentProcessor = paymentProcessorData.map(_._1),
      paymentProcessorConfig = paymentProcessorData.map(_._2),
    )
}
