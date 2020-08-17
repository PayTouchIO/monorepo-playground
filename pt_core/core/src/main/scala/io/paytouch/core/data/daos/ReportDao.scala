package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickLocationOptTimeZoneHelper, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.data.model.{ OrderRecord, OrderUpdate }
import io.paytouch.core.data.tables.OrdersTable
import io.paytouch.core.filters.{ ReportCustomerSummaryFilters, ReportProfitSummaryFilters, ReportSalesSummaryFilters }

import scala.concurrent._

class ReportDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickLocationOptTimeZoneHelper {

  type Record = OrderRecord
  type Update = OrderUpdate
  type Table = OrdersTable

  val table = TableQuery[Table]

  def computeReportSalesSummary(merchantId: UUID, f: ReportSalesSummaryFilters): Future[(Int, BigDecimal)] = {
    val q = queryFindAllByReportSalesSummaryFilters(merchantId, f)
    val totalAmountQ = q.map(_.totalAmount).sum
    run((q.length, totalAmountQ).result).map {
      case (count, amount) => count -> amount.getOrElse(0)
    }
  }

  private def queryFindAllByReportSalesSummaryFilters(merchantId: UUID, f: ReportSalesSummaryFilters) =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      isInvoice = f.isInvoice,
      paymentStatuses = f.paymentStatuses,
      from = f.from,
      to = f.to,
    )

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      isInvoice: Boolean,
      paymentStatuses: Seq[PaymentStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.locationId.isDefined && t.locationId.inSet(locationIds).getOrElse(false)),
        Some(t.isInvoice === isInvoice),
        Some(t.paymentStatus.isDefined && t.paymentStatus.inSet(paymentStatuses).getOrElse(false)),
        from.map(start => t.id in itemIdsAtOrAfterDate(start)(_.receivedAt)),
        to.map(end => t.id in itemIdsBeforeDate(end)(_.receivedAt)),
      ),
    )

  def computeReportProfitSummary(merchantId: UUID, f: ReportProfitSummaryFilters): Future[BigDecimal] =
    computeReportProfitSummary(merchantId, f.locationIds, f.isInvoice, f.paymentStatuses, f.from, f.to)

  def computeReportProfitSummaryPrevWeek(merchantId: UUID, f: ReportProfitSummaryFilters): Future[Option[BigDecimal]] =
    computeReportProfitSummaryInThePast(merchantId, f)(_.minusWeeks(1))

  def computeReportProfitSummaryPrevMonth(merchantId: UUID, f: ReportProfitSummaryFilters): Future[Option[BigDecimal]] =
    computeReportProfitSummaryInThePast(merchantId, f)(_.minusMonths(1))

  private def computeReportProfitSummaryInThePast(
      merchantId: UUID,
      f: ReportProfitSummaryFilters,
    )(
      timeAdjuster: LocalDateTime => LocalDateTime,
    ): Future[Option[BigDecimal]] =
    (f.from, f.to) match {
      case (Some(from), Some(to)) =>
        val fromInThePast = timeAdjuster(from)
        val toInThePast = timeAdjuster(to)
        computeReportProfitSummary(
          merchantId,
          f.locationIds,
          f.isInvoice,
          f.paymentStatuses,
          Some(fromInThePast),
          Some(toInThePast),
        ).map(Some(_))
      case _ => Future.successful(None)
    }

  private def computeReportProfitSummary(
      merchantId: UUID,
      locationIds: Seq[UUID],
      isInvoice: Boolean,
      paymentStatuses: Seq[PaymentStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[BigDecimal] = {
    val q = queryFindAllByMerchantId(merchantId, locationIds, isInvoice, paymentStatuses, from, to)
    val subtotalAmountQ = q.map(_.subtotalAmount).sum
    val discountAmountQ = q.map(_.discountAmount).sum
    run((subtotalAmountQ - discountAmountQ).result).map(_.getOrElse(0))
  }

  def computeCurrentCustomerSpendData(
      merchantId: UUID,
      f: ReportCustomerSummaryFilters,
    ): Future[Map[UUID, BigDecimal]] =
    computeCustomerSpendData(
      merchantId,
      f.locationIds,
      f.isInvoice,
      f.paymentStatuses,
      f.from,
      f.to,
      customerIds = None,
    )

  def computePastCustomerSpendData(
      merchantId: UUID,
      f: ReportCustomerSummaryFilters,
      customerIds: Seq[UUID],
    ): Future[Map[UUID, BigDecimal]] =
    f.from match {
      case Some(date) =>
        computeCustomerSpendData(
          merchantId,
          f.locationIds,
          f.isInvoice,
          f.paymentStatuses,
          from = None,
          to = Some(date),
          customerIds = Some(customerIds),
        )
      case _ => Future.successful(Map.empty)
    }

  private def computeCustomerSpendData(
      merchantId: UUID,
      locationIds: Seq[UUID],
      isInvoice: Boolean,
      paymentStatuses: Seq[PaymentStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      customerIds: Option[Seq[UUID]],
    ): Future[Map[UUID, BigDecimal]] = {
    val q = queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = locationIds,
      isInvoice = isInvoice,
      paymentStatuses = paymentStatuses,
      from = from,
      to = to,
    ).filter(t => all(customerIds.map(custIds => t.customerId.inSet(custIds).getOrElse(false))))
      .filter(_.customerId.isDefined)
      .groupBy(_.customerId)
      .map { case (customerId, rows) => customerId -> rows.map(_.totalAmount).sum.getOrElse(BigDecimal(0)) }

    run(q.result).map(result => result.toMap.view.filterKeys(_.isDefined).map { case (k, v) => k.get -> v }.toMap)
  }
}
