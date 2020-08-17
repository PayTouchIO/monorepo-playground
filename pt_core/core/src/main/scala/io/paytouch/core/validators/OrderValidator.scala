package io.paytouch.core.validators

import java.util.UUID

import cats.data._
import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, OrderDao }
import io.paytouch.core.data.model.{ OrderRecord, PaymentTransactionRecord }
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils._

import io.paytouch.core.validators.features.{ DefaultRecoveryValidator, EmailValidator }

import scala.concurrent._

class OpenOrderValidator(implicit override val ec: ExecutionContext, override val daos: Daos) extends OrderValidator {
  final override protected def recordsFinder(ids: Seq[Id])(implicit user: UserContext): Future[Seq[Record]] =
    daos.orderDao.findOpenByIds(ids)
}

class OrderValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultRecoveryValidator[OrderRecord]
       with EmailValidator {

  type Record = OrderRecord
  type Dao = OrderDao

  protected val dao = daos.orderDao
  val paymentTransactionDao = daos.paymentTransactionDao

  val validationErrorF = InvalidOrderIds(_)
  val accessErrorF = NonAccessibleOrderIds(_)

  val locationValidator = new LocationValidator
  val locationSettingsValidator = new LocationSettingsValidator
  val paymentTransactionValidator = new PaymentTransactionValidator(this)

  def canSendReceipt(
      orderId: UUID,
      paymentTransactionId: Option[UUID],
      sendReceiptData: SendReceiptData,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderRecord]] =
    for {
      validEmail <- validateEmailFormat(sendReceiptData.recipientEmail)
      existingOrder <- accessOneById(orderId)
      locationId = existingOrder.toOption.flatMap(_.locationId)
      location <- locationValidator.accessOneByOptId(locationId)
      paymentTransaction <- checkPaymentTransaction(paymentTransactionId, existingOrder)
      orderLocation <- validateOrderLocation(existingOrder.toOption)
    } yield Multiple.combine(validEmail, existingOrder, location, paymentTransaction, orderLocation) {
      case (_, ord, _, _, _) => ord
    }

  private def checkPaymentTransaction(
      paymentTransactionId: Option[UUID],
      validatedOrder: ErrorsOr[OrderRecord],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderRecord]] =
    validatedOrder match {
      case Validated.Valid(o) =>
        paymentTransactionId match {
          case Some(ptId) => validateAccessToPaymentTransaction(o.id, ptId).mapNested(_ => o)
          case None       => validateAtLeastOnePaymentTransactionExists(o.id).mapNested(_ => o)
        }
      case _ => Future.successful(validatedOrder)
    }

  private def validateAccessToPaymentTransaction(
      orderId: UUID,
      paymentTransactionId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[PaymentTransactionRecord]] =
    paymentTransactionValidator
      .accessOneByIdAndOrderId(paymentTransactionId, orderId)

  private def validateAtLeastOnePaymentTransactionExists(
      orderId: UUID,
    ): Future[ErrorsOr[Seq[PaymentTransactionRecord]]] =
    paymentTransactionDao.findByOrderIds(Seq(orderId)).map {
      case transactions if transactions.isEmpty => Multiple.failure(NoPaymentTransactionsForOrderId(orderId))
      case transactions                         => Multiple.success(transactions)
    }

  private def validateOrderLocation(order: Option[OrderRecord]): Future[ErrorsOr[Option[Record]]] =
    Future.successful {
      order match {
        case Some(o) if o.locationId.isEmpty => Multiple.failure(InvalidOrderSendReceipt(o.id))
        case o                               => Multiple.success(o)
      }
    }
}
