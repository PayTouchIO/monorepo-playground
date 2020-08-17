package io.paytouch.ordering.services

import java.util.UUID

import cats.data.OptionT
import cats.implicits._
import cats.data.Validated.{ Invalid, Valid }

import io.paytouch.ordering.Result
import io.paytouch.ordering.{ ServiceConfigurations, UpsertionResult }
import io.paytouch.ordering.calculations.PaymentIntentCalculations
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities.PaymentTransactionUpsertion
import io.paytouch.ordering.conversions.PaymentIntentConversions
import io.paytouch.ordering.data.daos.{ Daos, PaymentIntentDao }
import io.paytouch.ordering.data.model.{ PaymentIntentRecord, PaymentIntentUpdate, StoreRecord, WorldpayConfig }
import io.paytouch.ordering.entities.{
  PaymentIntentCreation,
  PaymentIntent => PaymentIntentEntity,
  PaymentIntentUpsertion,
  RapidoOrderContext,
  PaymentProcessorData,
}
import io.paytouch.ordering.entities.enums.{ PaymentIntentStatus, PaymentMethodType }
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.PaymentIntentValidator

import scala.concurrent.{ ExecutionContext, Future }

class PaymentIntentService(
    val merchantService: MerchantService,
    worldpayService: => WorldpayService,
    val coreClient: PtCoreClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends PaymentIntentConversions
       with PaymentIntentCalculations
       with CreateFeature {
  type Context = RapidoOrderContext
  type Creation = PaymentIntentCreation
  type Dao = PaymentIntentDao
  type Entity = PaymentIntentEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = PaymentIntentUpdate
  type Record = PaymentIntentRecord
  type Validator = PaymentIntentValidator
  type Upsertion = PaymentIntentUpsertion

  val dao = daos.paymentIntentDao
  protected val validator = new PaymentIntentValidator

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()
  protected val expanders = Seq.empty

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[Model] =
    Future.successful {
      existing match {
        case None =>
          val calculations = priceCalculationsOnCreation(upsertion)
          creationToUpsertionModel(id, upsertion, calculations)
        case _ => updateToUpsertionModel(id, upsertion)
      }
    }

  override protected def upsert(
      model: Model,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[Result[Entity]] =
    for {
      (resultType, record) <- dao.upsert(model)
      entity <- enrich(record)
      _ <- processAfterUpsert(entity, existing)
      paymentProcessorResult <- getPaymentProcessorData(record)
    } yield paymentProcessorResult match {
      case Valid(paymentProcessorData) =>
        resultType -> entity.copy(paymentProcessorData = Some(paymentProcessorData))

      case _ =>
        resultType -> entity
    }

  private def getPaymentProcessorData(
      paymentIntent: Record,
    )(implicit
      context: Context,
    ): Future[ValidatedData[PaymentProcessorData]] =
    paymentIntent.paymentMethodType match {
      case PaymentMethodType.Worldpay =>
        merchantService.getPaymentProcessorConfig(context).flatMap {
          case Some(config: WorldpayConfig) =>
            worldpayService.setupTransaction(paymentIntent, config)

          case _ =>
            Future.successful(ValidatedData.success(PaymentProcessorData.empty))
        }

      case _ =>
        Future.successful(ValidatedData.success(PaymentProcessorData.empty))
    }

  def markAsPaid(
      paymentIntent: Record,
      paymentTransaction: PaymentTransactionUpsertion,
    )(implicit
      context: Context,
    ): Future[Unit] = {
    implicit val authHeader = coreClient.generateAuthHeaderForCoreMerchant(paymentIntent.merchantId)

    for {
      _ <- updatePaymentStatus(paymentIntent, PaymentIntentStatus.Paid)
      _ <- coreClient.orderStorePaymentTransaction(
        paymentIntent.orderId,
        paymentTransactionWithExtras(paymentIntent, paymentTransaction),
      )
    } yield ()
  }

  private def updatePaymentStatus(paymentIntent: Record, status: PaymentIntentStatus) = {
    val update = PaymentIntentUpdate(
      id = paymentIntent.id,
      status = Some(status),
    )

    dao.upsert(update)
  }
}
