package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.conversions._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.data.model.upsertions.OrderItemUpsertion
import io.paytouch.core.entities._
import io.paytouch.core.filters.ProductRevenueFilters
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators._

class OrderItemService(
    giftCardPassService: => GiftCardPassService,
    val orderItemDiscountService: OrderItemDiscountService,
    val orderItemModifierOptionService: OrderItemModifierOptionService,
    val orderItemTaxRateService: OrderItemTaxRateService,
    val orderItemVariantOptionService: OrderItemVariantOptionService,
    ticketOrderItemService: => TicketOrderItemService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends OrderItemConversions
       with ProductRevenueConversions {

  protected val dao = daos.orderItemDao
  protected val validator = new OrderItemValidator

  val productValidator = new ProductValidatorIncludingDeleted

  def findByTickets(
      tickets: Seq[TicketRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[TicketRecord, Seq[OrderItem]]] =
    dao
      .findByTicketIds(tickets.map(_.id))
      .flatMap { recordsPerTicketId =>
        val records = recordsPerTicketId.values.flatten.toSeq.distinct

        enrich(records)(withGiftCardPasses = false).map { entities =>
          recordsPerTicketId
            .mapKeysToRecords(tickets)
            .transform((_, orderItemRecords) => entities.filter(e => orderItemRecords.map(_.id).contains(e.id)))
        }
      }

  def findBundleItemsByOrderId(
      tickets: Seq[TicketRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[OrderItem]]] = {
    val orderIds = tickets.map(_.orderId)
    dao.findBundleItemsByOrderIds(orderIds).flatMap { recordsPerOrderId =>
      val records = recordsPerOrderId.values.flatten.toSeq.distinct
      enrich(records)(withGiftCardPasses = false).map(_.groupBy(_.orderId))
    }
  }

  def findByIds(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[OrderItem]] =
    dao.findByIds(ids).flatMap(enrich(_)(withGiftCardPasses = false))

  def findRecordsByOrderIds(orderIds: Seq[UUID]): Future[Seq[OrderItemRecord]] =
    dao.findByOrderIds(orderIds)

  def findByOrderIds(
      orderIds: Seq[UUID],
    )(
      withGiftCardPasses: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderItem]] =
    findRecordsByOrderIds(orderIds).flatMap(enrich(_)(withGiftCardPasses))

  def enrich(
      orderItems: Seq[OrderItemRecord],
    )(
      withGiftCardPasses: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderItem]] = {
    val orderItemDiscountsR = getDiscountsPerOrderItem(orderItems)
    val orderItemModifierOptionsR = getModifierOptionsPerOrderItem(orderItems)
    val orderItemTaxRatesR = orderItemTaxRateService.findOrderItemTaxRatesPerOrderItem(orderItems)
    val orderItemVariantOptionsR = getVariantOptionsPerOrderItem(orderItems)
    val ticketsPerOrderItemR = getTicketsPerOrderItem(orderItems)
    val giftCardPassesPerOrderItemR = getOptionalGiftCardPasses(orderItems)(withGiftCardPasses)

    for {
      orderItemDiscounts <- orderItemDiscountsR
      orderItemModifierOptions <- orderItemModifierOptionsR
      orderItemTaxRates <- orderItemTaxRatesR
      orderItemVariantOptions <- orderItemVariantOptionsR
      ticketsPerOrderItem <- ticketsPerOrderItemR
      giftCardPassesPerOrderItem <- giftCardPassesPerOrderItemR
    } yield fromRecordsAndOptionsToEntities(
      orderItems,
      orderItemDiscounts,
      orderItemModifierOptions,
      orderItemTaxRates,
      orderItemVariantOptions,
      ticketsPerOrderItem,
      giftCardPassesPerOrderItem,
    )
  }

  def getVariantOptionsPerOrderItem(
      records: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderItemRecord, Seq[OrderItemVariantOption]]] =
    orderItemVariantOptionService
      .findAllItemsByOrderItemIds(records.map(_.id))
      .map(_.mapKeysToRecords(records))

  def getModifierOptionsPerOrderItem(
      records: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderItemRecord, Seq[OrderItemModifierOption]]] =
    orderItemModifierOptionService
      .findAllItemsByOrderItemIds(records.map(_.id))
      .map(_.mapKeysToRecords(records))

  def getDiscountsPerOrderItem(
      records: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderItemRecord, Seq[OrderItemDiscount]]] =
    orderItemDiscountService
      .findAllItemsByOrderItemIds(records.map(_.id))
      .map(_.mapKeysToRecords(records))

  def getTicketsPerOrderItem(records: Seq[OrderItemRecord]): Future[Map[OrderItemRecord, Seq[TicketRecord]]] =
    ticketOrderItemService.getTicketsPerOrderItem(records)

  def getOptionalGiftCardPasses(
      records: Seq[OrderItemRecord],
    )(
      withGiftCardPasses: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[OrderItemRecord, GiftCardPassInfo]]] =
    if (withGiftCardPasses)
      giftCardPassService
        .findInfoByOrderItemIds(records.map(_.id))
        .map(_.mapKeysToRecords(records).some)
    else
      Future.successful(None)

  def recoverUpsertionModels(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderItemUpsertion]] =
    upsertion
      .items
      .map { itemUpsertion =>
        OrderItemUpsertion(
          orderItem = convertToOrderItemUpdate(upsertion.orderId, itemUpsertion),
          discounts = orderItemDiscountService.convertToOrderItemDiscountUpdates(itemUpsertion).some,
          modifierOptions = orderItemModifierOptionService.convertToOrderItemModifierOptionUpdates(itemUpsertion).some,
          variantOptions = orderItemVariantOptionService.convertToOrderItemVariantOptionUpdates(itemUpsertion).some,
          taxRates = orderItemTaxRateService.convertToOrderItemTaxRateUpdates(itemUpsertion).some,
          giftCardPassRecipientEmail = itemUpsertion.giftCardPassRecipientEmail,
        )
      }
      .pure[Future]

  private def convertToOrderItemUpdate(
      orderId: UUID,
      upsertion: RecoveredOrderItemUpsertion,
    )(implicit
      user: UserContext,
    ): OrderItemUpdate = {
    val newPriceAmount = upsertion.priceAmount
    OrderItemUpdate(
      id = Some(upsertion.id),
      merchantId = Some(user.merchantId),
      orderId = Some(orderId),
      productId = upsertion.productId,
      productName = upsertion.productName,
      productDescription = upsertion.productDescription,
      productType = upsertion.productType,
      quantity = upsertion.quantity,
      unit = upsertion.unit,
      paymentStatus = upsertion.paymentStatus,
      priceAmount = upsertion.priceAmount,
      costAmount = upsertion.costAmount,
      discountAmount = upsertion.discountAmount,
      taxAmount = upsertion.taxAmount,
      basePriceAmount = upsertion.basePriceAmount,
      // TODO: remove `orElse` when we are sure Register will always send calculatedPriceAmount
      calculatedPriceAmount = upsertion.calculatedPriceAmount.orElse(newPriceAmount),
      totalPriceAmount = upsertion.totalPriceAmount,
      giftCardPassRecipientEmail = upsertion.giftCardPassRecipientEmail,
      notes = upsertion.notes,
    )
  }

  def computeRevenue(f: ProductRevenueFilters)(implicit user: UserContext): Future[ErrorsOr[ProductRevenue]] =
    productValidator.accessOneById(f.productId).flatMapTraverse { _ =>
      dao.findByMerchantIdAndProductIdPerLocation(user.merchantId, f.productId, user.locationIds, f.from, f.to).map {
        recordsPerLocation => toProductRevenue(f.productId, recordsPerLocation)
      }
    }
}
