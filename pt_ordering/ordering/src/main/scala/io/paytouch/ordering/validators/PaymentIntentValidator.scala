package io.paytouch.ordering.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.data.daos.{ Daos, PaymentIntentDao }
import io.paytouch.ordering.data.model.{ PaymentIntentRecord, PaymentProcessorConfig, WorldpayConfig }
import io.paytouch.ordering.entities.{ PaymentIntentUpsertion, RapidoOrderContext }
import io.paytouch.ordering.entities.enums.PaymentMethodType
import io.paytouch.ordering.errors.{
  AlreadyPaidOrderItems,
  InvalidOrderItemIds,
  InvalidPaymentIntentIds,
  NonAccessiblePaymentIntentIds,
  NotIncludedOrderItems,
  UnsupportedPaymentMethodType,
}
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.features.{ UpsertionValidator, Validator }

import scala.concurrent.{ ExecutionContext, Future }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentStatus

class PaymentIntentValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends Validator
       with UpsertionValidator {

  type Context = RapidoOrderContext
  type Dao = PaymentIntentDao
  type Record = PaymentIntentRecord
  type Upsertion = PaymentIntentUpsertion

  protected val dao = daos.paymentIntentDao
  val merchantDao = daos.merchantDao

  val validationErrorF = InvalidPaymentIntentIds(_)
  val accessErrorF = NonAccessiblePaymentIntentIds(_)

  val supportedPaymentMethods = Seq(PaymentMethodType.Worldpay)

  protected def recordsFinder(ids: Seq[UUID])(implicit context: Context): Future[Seq[Record]] =
    dao.findByIds(ids)

  protected def validityCheck(record: Record)(implicit context: Context): Boolean =
    record.merchantId == context.merchantId && record.orderId == context.order.id

  def validateUpsertion(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[ValidatedData[Upsertion]] =
    for {
      validPaymentMethod <- validatePaymentMethod(upsertion)
      validItems <- validateOrderItems(upsertion)
    } yield ValidatedData.combine(validPaymentMethod, validItems) { case _ => upsertion }

  protected def validatePaymentMethod(
      upsertion: Upsertion,
    )(implicit
      context: Context,
    ): Future[ValidatedData[Upsertion]] =
    merchantDao.findPaymentProcessorConfig(context.merchantId).map {
      case Some(config: PaymentProcessorConfig) =>
        (upsertion.paymentMethodType, config) match {
          case (PaymentMethodType.Worldpay, c: WorldpayConfig) =>
            ValidatedData.success(upsertion)
          case _ => ValidatedData.failure(UnsupportedPaymentMethodType(upsertion.paymentMethodType))
        }
      case _ => ValidatedData.failure(UnsupportedPaymentMethodType(upsertion.paymentMethodType))
    }

  protected def validateOrderItems(
      upsertion: Upsertion,
    )(implicit
      context: Context,
    ): Future[ValidatedData[Upsertion]] =
    Future {
      val orderItemIds = context.order.items.map(_.id)
      val invalidIds = upsertion.orderItemIds.filterNot(orderItemIds contains _)

      invalidIds match {
        case Nil =>
          val unpaidOrderItemIds = context.order.items.filter(_.paymentStatus == Some(PaymentStatus.Pending)).map(_.id)
          val paidIds = upsertion.orderItemIds.filterNot(unpaidOrderItemIds contains _)
          paidIds match {
            case Nil =>
              // For v1 only. To be removed for payment intents v2.
              val notIncludedIds = context.order.items.filterNot(upsertion.orderItemIds contains _.id)
              notIncludedIds match {
                case Nil =>
                  ValidatedData.success(upsertion)
                case _ => ValidatedData.failure(NotIncludedOrderItems(invalidIds))

              }

            case _ => ValidatedData.failure(AlreadyPaidOrderItems(invalidIds))
          }
        case _ => ValidatedData.failure(InvalidOrderItemIds(invalidIds))
      }
    }
}
