package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.calculations.OrderItemsCalculations
import io.paytouch.core.data.model._
import io.paytouch.core.entities._

trait ProductRevenueConversions extends OrderItemsCalculations {

  def toProductRevenue(
      productId: UUID,
      recordsPerLocation: Map[UUID, Seq[OrderItemRecord]],
    )(implicit
      user: UserContext,
    ): ProductRevenue =
    if (recordsPerLocation.isEmpty) ProductRevenue(productId, Seq.empty)
    else {
      val revenuePerLocation = recordsPerLocation.map {
        case (locationId, records) => toProductRevenuePerLocation(locationId, records)
      }.toSeq
      ProductRevenue(productId, revenuePerLocation)
    }

  private def toProductRevenuePerLocation(
      locationId: UUID,
      orderItems: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): ProductRevenuePerLocation =
    ProductRevenuePerLocation(
      locationId = locationId,
      avgPrice = computeAvgPrice(orderItems),
      avgDiscount = computeAvgDiscount(orderItems),
      avgCost = computeAvgCost(orderItems),
      avgMargin = computeAvgMargin(orderItems),
      totalSold = computeTotalSold(orderItems),
      totalRevenue = computeTotalRevenue(orderItems),
      totalTax = computeTotalTax(orderItems),
      totalProfit = computeTotalProfit(orderItems),
    )
}
