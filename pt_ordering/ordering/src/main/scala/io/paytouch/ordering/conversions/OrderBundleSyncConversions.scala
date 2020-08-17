package io.paytouch.ordering.conversions
import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentStatus
import io.paytouch.ordering.entities.{ Cart, CartItem, CartItemBundleOption, CartItemBundleSet }

trait OrderBundleSyncConversions { self: OrderItemConversions =>

  protected def toOrderBundles(
      cart: Cart,
      existingOrder: Option[Order],
    ): (Seq[OrderBundleUpsertion], Seq[OrderItemUpsertion]) =
    cart.items.filter(_.isCombo).map(toOrderBundleAndOrderItemsPairs(_, existingOrder)).unzip match {
      case (bundles, items) => (bundles, items.flatten)
    }

  private def toOrderBundleAndOrderItemsPairs(
      item: CartItem,
      existingOrder: Option[Order],
    ): (OrderBundleUpsertion, Seq[OrderItemUpsertion]) = {
    val existingBundle = existingOrder.flatMap(_.bundles.find(_.bundleOrderItemId == item.id))
    val bundleSets = item.bundleSets.getOrElse(Seq.empty)
    val (orderBundleSets, orderItemUpsertions) =
      bundleSets.map(toOrderBundleSetAndOrderItemUpsertions(_, existingBundle)).unzip
    val orderBundleUpsertion = toOrderBundleUpsertion(item, orderBundleSets, existingBundle)
    (orderBundleUpsertion, orderItemUpsertions.flatten)
  }

  private def toOrderBundleUpsertion(
      item: CartItem,
      orderBundleSets: Seq[OrderBundleSetUpsertion],
      existingBundle: Option[OrderBundle],
    ) =
    OrderBundleUpsertion(
      id = existingBundle.map(_.id).getOrElse(UUID.randomUUID),
      bundleOrderItemId = item.id,
      orderBundleSets = orderBundleSets,
    )

  private def toOrderBundleSetAndOrderItemUpsertions(
      bundleSet: CartItemBundleSet,
      existingBundle: Option[OrderBundle],
    ): (OrderBundleSetUpsertion, Seq[OrderItemUpsertion]) = {
    val existingBundleSet =
      existingBundle.flatMap(_.orderBundleSets.find(_.bundleSetId == Some(bundleSet.bundleSetId)))

    val (orderItemUpsertions, orderBundleOptions) =
      bundleSet
        .cartItemBundleOptions
        .map(toOrderItemAndBundleOptionPairs(_, existingBundleSet))
        .unzip

    val orderBundleSet =
      toOrderBundleSetUpsertion(bundleSet, orderBundleOptions, existingBundleSet)

    orderBundleSet -> orderItemUpsertions
  }

  private def toOrderBundleSetUpsertion(
      bundleSet: CartItemBundleSet,
      orderBundleOptions: Seq[OrderBundleOptionUpsertion],
      existingBundleSet: Option[OrderBundleSet],
    ): OrderBundleSetUpsertion =
    OrderBundleSetUpsertion(
      id = existingBundleSet.map(_.id).getOrElse(UUID.randomUUID),
      bundleSetId = bundleSet.bundleSetId,
      name = bundleSet.name,
      orderBundleOptions = orderBundleOptions,
      position = bundleSet.position,
    )

  private def toOrderItemAndBundleOptionPairs(
      bundleOption: CartItemBundleOption,
      existingBundleSet: Option[OrderBundleSet],
    ): (OrderItemUpsertion, OrderBundleOptionUpsertion) = {
    val existingOption =
      existingBundleSet.flatMap(_.orderBundleOptions.find(_.bundleOptionId == Some(bundleOption.bundleOptionId)))

    val orderItemId =
      existingOption.flatMap(_.articleOrderItemId).getOrElse(UUID.randomUUID)

    val orderItemUpsertion =
      toOrderItemUpsertion(orderItemId, bundleOption)

    val orderBundleOptionUpsertion =
      toOrderBundleOptionUpsertion(orderItemId, bundleOption, existingOption)

    orderItemUpsertion -> orderBundleOptionUpsertion
  }

  private def toOrderItemUpsertion(orderItemId: UUID, bundleOption: CartItemBundleOption) =
    OrderItemUpsertion(
      id = orderItemId,
      productId = Some(bundleOption.item.product.id),
      productName = Some(bundleOption.item.product.name),
      productDescription = bundleOption.item.product.description,
      quantity = Some(bundleOption.item.quantity),
      unit = Some(bundleOption.item.unit),
      paymentStatus = Some(PaymentStatus.Pending),
      priceAmount = Some(0), // register sends this
      costAmount = bundleOption.item.cost.map(_.amount),
      taxAmount = Some(0), // register sends this
      discountAmount = Some(0), // register sends this
      calculatedPriceAmount = Some(0), // register sends this
      totalPriceAmount = None,
      modifierOptions = toOrderItemModifierOptionUpsertions(bundleOption.item.modifierOptions),
      variantOptions = toOrderItemVariantOptionUpsertions(bundleOption.item.variantOptions),
      notes = bundleOption.item.notes,
      taxRates = Seq.empty,
      giftCardPassRecipientEmail = None,
    )

  private def toOrderBundleOptionUpsertion(
      orderItemId: UUID,
      bundleOption: CartItemBundleOption,
      existingOption: Option[OrderBundleOption],
    ): OrderBundleOptionUpsertion =
    OrderBundleOptionUpsertion(
      id = existingOption.map(_.id).getOrElse(UUID.randomUUID()),
      bundleOptionId = bundleOption.bundleOptionId,
      articleOrderItemId = orderItemId,
      priceAdjustment = bundleOption.priceAdjustment,
      position = bundleOption.position,
    )
}
