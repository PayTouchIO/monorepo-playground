package io.paytouch.core.calculations

import java.util.UUID

import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus }
import io.paytouch.core.entities._
import io.paytouch.core.validators.RecoveredOrderBundleSet

import StockModifierCalculations._

trait StockModifierCalculations {
  final protected def allProductIds(oldOrderItems: Seq[OrderItemRecord], currentOrderItems: Seq[UUID]): Seq[UUID] =
    (oldOrderItems.flatMap(_.productId) ++ currentOrderItems).distinct

  final protected def calculateDiff(
      productId: UUID,
      upsertionModel: model.upsertions.OrderUpsertion,
      oldOrderStatus: Option[OrderStatus],
      oldOrderItems: Seq[OrderItemRecord],
      oldOrderBundles: Seq[OrderBundleRecord],
      orderItems: Seq[OrderItemUpdate],
    ): BigDecimal = {
    val newQuantity: BigDecimal =
      quantity(
        productId,
        upsertionModel.order.status,
        StockModifierCalculations.Source.Update(upsertionModel.orderBundles.getOrElse(Seq.empty), orderItems),
      )

    val prevQuantity: BigDecimal =
      quantity(
        productId,
        oldOrderStatus,
        StockModifierCalculations.Source.Record(oldOrderBundles, oldOrderItems),
      )

    val diff: BigDecimal = newQuantity - prevQuantity

    -diff
  }

  final protected def calculateDiff(
      productId: UUID,
      upsertion: OrderUpsertion,
      oldOrderStatus: Option[OrderStatus],
      oldOrderItems: Seq[OrderItemRecord],
      oldOrderBundles: Seq[OrderBundleRecord],
      orderItems: Seq[OrderItemUpsertion],
    ): BigDecimal = {
    val newQuantity: BigDecimal =
      quantity(
        productId,
        Some(upsertion.status),
        StockModifierCalculations.Source.Upsertion(upsertion.bundles, orderItems),
      )

    val prevQuantity: BigDecimal =
      quantity(
        productId,
        oldOrderStatus,
        StockModifierCalculations.Source.Record(oldOrderBundles, oldOrderItems),
      )

    val diff = newQuantity - prevQuantity

    diff
  }

  private[this] def quantity(
      productId: UUID,
      orderStatus: Option[OrderStatus],
      orderItemsFor: Source,
    )(implicit
      convert: Source => UUID => Seq[Target],
    ): BigDecimal =
    quantity(
      orderStatus,
      orderItemsFor(productId).map(target => target.orderItem.paymentStatus -> quantity(target)),
    )

  private[this] def quantity(
      orderStatus: Option[OrderStatus],
      data: Seq[(Option[PaymentStatus], Option[BigDecimal])],
    ): BigDecimal =
    orderStatus
      .collect {
        case OrderStatus.Canceled => 0: BigDecimal
      }
      .getOrElse {
        data.flatMap {
          case (paymentStatus, quantity) =>
            paymentStatus
              .collect {
                case PaymentStatus.Refunded => Some(0: BigDecimal)
              }
              .getOrElse(quantity)

        }.sum
      }

  private[this] def quantity(target: Target): Option[BigDecimal] = {
    val matchingBundle: Option[Bundle] =
      target
        .orderBundles
        .find { bundle =>
          bundle
            .orderBundleSets
            .exists(bundleSet => bundleSet.orderBundleOptions.exists(_.articleOrderItemId == target.orderItem.id))
        }

    val bundleQuantity: BigDecimal =
      matchingBundle
        .flatMap { bundle =>
          target
            .orderItems
            .find(_.id == bundle.bundleOrderItemId)
            .flatMap(_.quantity)
        }
        .getOrElse(1)

    target.orderItem.quantity.map(_ * bundleQuantity)
  }
}

object StockModifierCalculations {
  sealed abstract class Source extends scala.Product with Serializable
  object Source {
    final case class Record(orderBundles: Seq[OrderBundleRecord], orderItems: Seq[OrderItemRecord]) extends Source {
      def orderItemsFor(productId: UUID): Seq[OrderItemRecord] =
        orderItems.filter(_.productId.contains(productId))
    }

    final case class Upsertion(orderBundles: Seq[OrderBundleUpsertion], orderItems: Seq[OrderItemUpsertion])
        extends Source {
      def orderItemsFor(productId: UUID): Seq[OrderItemUpsertion] =
        orderItems.filter(_.productId.contains(productId))
    }

    final case class Update(orderBundles: Seq[OrderBundleUpdate], orderItems: Seq[OrderItemUpdate]) extends Source {
      def orderItemsFor(productId: UUID): Seq[OrderItemUpdate] =
        orderItems.filter(_.productId.contains(productId))
    }
  }

  implicit protected def toTarget(source: Source)(productId: UUID): Seq[Target] =
    source match {
      case s: Source.Record =>
        s.orderItemsFor(productId).map { orderItem =>
          Target(s.orderBundles.map(toBundle), s.orderItems.map(toItem), toItem(orderItem))
        }

      case s: Source.Upsertion =>
        s.orderItemsFor(productId).map { orderItem =>
          Target(s.orderBundles.map(toBundle), s.orderItems.map(toItem), toItem(orderItem))
        }

      case s: Source.Update =>
        s.orderItemsFor(productId).map { orderItem =>
          Target(s.orderBundles.map(toBundle), s.orderItems.map(toItem), toItem(orderItem))
        }
    }

  private[this] def toBundle(source: OrderBundleRecord): Bundle =
    Bundle(source.orderBundleSets.map(toBundleSet), Some(source.bundleOrderItemId))

  private[this] def toBundle(source: OrderBundleUpsertion): Bundle =
    Bundle(source.orderBundleSets.map(toBundleSet), Some(source.bundleOrderItemId))

  private[this] def toBundle(source: OrderBundleUpdate): Bundle =
    Bundle(source.orderBundleSets.getOrElse(Seq.empty).map(toBundleSet), source.bundleOrderItemId)

  private[this] def toBundleSet(source: RecoveredOrderBundleSet): BundleSet =
    BundleSet(source.orderBundleOptions.map(_.articleOrderItemId).map(BundleOption))

  private[this] def toBundleSet(source: OrderBundleSetUpsertion): BundleSet =
    BundleSet(source.orderBundleOptions.map(_.articleOrderItemId).map(BundleOption))

  private[this] def toItem(source: OrderItemRecord): Item =
    Item(Some(source.id), source.quantity, source.paymentStatus)

  private[this] def toItem(source: OrderItemUpsertion): Item =
    Item(Some(source.id), source.quantity, source.paymentStatus)

  private[this] def toItem(source: OrderItemUpdate): Item =
    Item(source.id, source.quantity, source.paymentStatus)

  final case class Target(
      orderBundles: Seq[Bundle],
      orderItems: Seq[Item],
      orderItem: Item,
    )
  final case class Bundle(orderBundleSets: Seq[BundleSet], bundleOrderItemId: Option[UUID])
  final case class BundleSet(orderBundleOptions: Seq[BundleOption])
  final case class BundleOption(articleOrderItemId: Any)
  final case class Item(
      id: Option[UUID],
      quantity: Option[BigDecimal],
      paymentStatus: Option[PaymentStatus],
    )
}
