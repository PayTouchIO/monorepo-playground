package io.paytouch.core.data.queries

import java.util.UUID

import io.paytouch.core.data.daos._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.OrderRecord
import io.paytouch.core.data.tables.OrdersTable
import io.paytouch.core.entities.enums.View

class OrdersTableQuery[R <: OrderRecord, T <: OrdersTable](
    q: Query[T, T#TableElementType, Seq],
  )(implicit
    customerMerchantDao: CustomerMerchantDao,
    onlineOrderAttributeDao: OnlineOrderAttributeDao,
    paymentTransactionDao: PaymentTransactionDao,
  ) {

  def filterByOptIsInvoice(maybeIsInvoice: Option[Boolean]) =
    maybeIsInvoice.fold(q)(isInvoice => q.filter(_.isInvoice === isInvoice))

  def filterByOptCustomerId(maybeCustomerId: Option[UUID]) =
    maybeCustomerId.fold(q)(customerId => q.filter(_.customerId === customerId))

  def filterByOptTableId(maybeTableId: Option[UUID]) =
    maybeTableId.fold(q)(tableId => q.filter(_.seatingJson +>> "tableId" === tableId.toString))

  def filterByOptPaymentType(maybePaymentType: Option[OrderPaymentType]) =
    maybePaymentType.fold(q)(paymentType => q.filter(t => t.paymentType === paymentType))

  def filterByOptEitherSourceOrDeliveryProvider(
      maybeSourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]],
    ) =
    maybeSourcesOrDeliveryProviders.fold(q)(
      _.fold(
        sources => q.filter(_.source inSet sources),
        deliveryProviders => q.filter(_.deliveryProvider inSet deliveryProviders),
      ),
    )

  def filterByOptView(view: Option[View]) =
    view match {
      case Some(View.Active)    => q.filterNot(isOrderCompleted).filterNot(isOrderCanceled)
      case Some(View.Completed) => q.filter(isOrderCompleted)
      case Some(View.Canceled)  => q.filter(isOrderCanceled)
      case Some(View.Pending)   => q.filter(isOrderPending)
      case _                    => q
    }

  def filterCompletedOrders() = q.filter(isOrderCompleted)

  private def isOrderCompleted(t: T) =
    t.paymentStatus.inSet(PaymentStatus.isPaid) && t.status === (OrderStatus.Completed: OrderStatus)

  private def isOrderCanceled(t: T) =
    t.status === (OrderStatus.Canceled: OrderStatus)

  private def isOrderPending(t: T) = {
    val canceled: OrderStatus = OrderStatus.Canceled
    val completed: OrderStatus = OrderStatus.Completed
    val pending: PaymentStatus = PaymentStatus.Pending
    (t.status =!= completed || t.paymentStatus === pending) && t.status =!= canceled
  }

  def filterByOptQuery(maybeQuery: Option[String]) =
    maybeQuery.fold(q) { query =>
      q.filter(t =>
        querySearchByNumber(t, query) || querySearchByCustomerName(t, query) || querySearchByCardDigits(t, query),
      )
    }

  private def querySearchByNumber(t: T, query: String) =
    (t.number.toLowerCase like s"%${query.toLowerCase}%").getOrElse(false)

  private def querySearchByCustomerName(t: T, query: String) =
    (t.customerId in customerMerchantDao.querySearchByName(Some(query)).map(_.customerId)).getOrElse(false)

  private def querySearchByCardDigits(t: T, query: String) =
    t.id in paymentTransactionDao.querySearchByCardDigits(query).map(_.orderId)

  def filterByNumber(query: String) =
    q.filter(t => (t.number.toLowerCase like s"%${query.toLowerCase}%").getOrElse(false))

  def filterByOptOrderStatus(maybeOrderStatus: Option[Seq[OrderStatus]]) =
    maybeOrderStatus.fold(q)(orderStatus => q.filter(_.status inSet orderStatus))

  def filterByOptAcceptanceStatus(maybeAcceptanceStatus: Option[AcceptanceStatus]) =
    maybeAcceptanceStatus.fold(q) { acceptanceStatus =>
      q.join(onlineOrderAttributeDao.queryFilterByAcceptanceStatus(acceptanceStatus))
        .on(_.onlineOrderAttributeId === _.id)
        .map { case (orders, _) => orders }
    }

  def filterByOptPaymentStatus(maybePaymentStatus: Option[PaymentStatus]) =
    maybePaymentStatus.fold(q)(paymentStatus => q.filter(t => t.paymentStatus === paymentStatus))

}
