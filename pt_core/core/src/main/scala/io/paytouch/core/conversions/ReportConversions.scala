package io.paytouch.core.conversions

import java.util.{ Currency, UUID }

import io.paytouch.core.entities._

trait ReportConversions {

  protected def toProfitSummary(
      profitAmount: BigDecimal,
      profitAmountPrevWeek: Option[BigDecimal],
      profitAmountPrevMonth: Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): Map[Currency, ReportProfitSummary] = {
    val profit = MonetaryAmount(profitAmount)
    val profitPrevWeek = MonetaryAmount.extract(profitAmountPrevWeek)
    val profitPrevMonth = MonetaryAmount.extract(profitAmountPrevMonth)
    Map(user.currency -> ReportProfitSummary(profit, profitPrevWeek, profitPrevMonth))
  }

  protected def extractCurrentCustomersData(
      currentCustomerData: Map[UUID, BigDecimal],
      pastCustomersData: Map[UUID, BigDecimal],
    )(implicit
      user: UserContext,
    ): CustomersSummary = {
    val newCustomerData = currentCustomerData.view.filterKeys(!hasReturningCustomer(_, pastCustomersData)).toMap
    toCustomerSummary(newCustomerData)
  }

  protected def extractReturningCustomersData(
      currentCustomerData: Map[UUID, BigDecimal],
      pastCustomersData: Map[UUID, BigDecimal],
    )(implicit
      user: UserContext,
    ): CustomersSummary = {
    val returningCustomerData = currentCustomerData.view.filterKeys(hasReturningCustomer(_, pastCustomersData)).toMap
    toCustomerSummary(returningCustomerData)
  }

  private def toCustomerSummary(customerData: Map[UUID, BigDecimal])(implicit user: UserContext): CustomersSummary = {
    val count = customerData.keySet.size
    val totalSpend = MonetaryAmount(customerData.values.sum)
    CustomersSummary(count, totalSpend)
  }

  private def hasReturningCustomer(customerId: UUID, pastData: Map[UUID, BigDecimal]): Boolean =
    pastData.get(customerId).isDefined

  protected def toReportCustomerSummary(
      newCustomers: CustomersSummary,
      returningCustomers: CustomersSummary,
    )(implicit
      user: UserContext,
    ): Map[Currency, ReportCustomerSummary] =
    Map(user.currency -> ReportCustomerSummary(newCustomers, returningCustomers))
}
