package io.paytouch.core.services

import java.util.Currency

import io.paytouch.core.conversions.ReportConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.filters.{ ReportCustomerSummaryFilters, ReportProfitSummaryFilters, ReportSalesSummaryFilters }

import scala.concurrent._

class ReportService(implicit val ec: ExecutionContext, val daos: Daos) extends ReportConversions {

  protected val dao = daos.reportDao

  def computeReportSalesSummary(
      f: ReportSalesSummaryFilters,
    )(implicit
      user: UserContext,
    ): Future[Map[Currency, ReportSalesSummary]] =
    dao.computeReportSalesSummary(user.merchantId, f).map {
      case (count, amount) => Map(user.currency -> ReportSalesSummary(revenue = MonetaryAmount(amount), count = count))
    }

  def getProfitSummary(
      f: ReportProfitSummaryFilters,
    )(implicit
      user: UserContext,
    ): Future[Map[Currency, ReportProfitSummary]] = {
    val profitR = dao.computeReportProfitSummary(user.merchantId, f)
    val profitPrevWeekR = dao.computeReportProfitSummaryPrevWeek(user.merchantId, f)
    val profitPrevMonthR = dao.computeReportProfitSummaryPrevMonth(user.merchantId, f)
    for {
      profit <- profitR
      profitPrevWeek <- profitPrevWeekR
      profitPrevMonth <- profitPrevMonthR
    } yield toProfitSummary(profit, profitPrevWeek, profitPrevMonth)
  }

  def getCustomerSummary(
      f: ReportCustomerSummaryFilters,
    )(implicit
      user: UserContext,
    ): Future[Map[Currency, ReportCustomerSummary]] =
    for {
      currentCustomerData <- dao.computeCurrentCustomerSpendData(user.merchantId, f)
      currentCustomers = currentCustomerData.map { case (customerId, _) => customerId }.toSeq
      pastCustomerData <- dao.computePastCustomerSpendData(user.merchantId, f, currentCustomers)
    } yield {
      val currentCustomersSummary = extractCurrentCustomersData(currentCustomerData, pastCustomerData)
      val returningCustomersSummary = extractReturningCustomersData(currentCustomerData, pastCustomerData)
      toReportCustomerSummary(currentCustomersSummary, returningCustomersSummary)
    }

}
