package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID
import cats.implicits._

import io.paytouch.core.ordering
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ ResettableSeating, ResettableString, Seating }
import io.paytouch.core.utils.UtcTime

final case class OrderRecord(
    id: UUID,
    merchantId: UUID,
    locationId: Option[UUID],
    deviceId: Option[UUID],
    userId: Option[UUID],
    customerId: Option[UUID],
    deliveryAddressId: Option[UUID],
    onlineOrderAttributeId: Option[UUID],
    number: Option[String],
    tag: Option[String],
    source: Option[Source],
    `type`: Option[OrderType],
    paymentType: Option[OrderPaymentType],
    totalAmount: Option[BigDecimal],
    subtotalAmount: Option[BigDecimal],
    discountAmount: Option[BigDecimal],
    taxAmount: Option[BigDecimal],
    tipAmount: Option[BigDecimal],
    ticketDiscountAmount: Option[BigDecimal],
    deliveryFeeAmount: Option[BigDecimal],
    customerNotes: Seq[CustomerNote],
    merchantNotes: Seq[MerchantNote],
    paymentStatus: Option[PaymentStatus],
    status: Option[OrderStatus],
    fulfillmentStatus: Option[FulfillmentStatus],
    statusTransitions: Seq[StatusTransition],
    isInvoice: Boolean,
    isFiscal: Boolean,
    version: Int,
    seating: Option[Seating],
    deliveryProvider: Option[DeliveryProvider],
    deliveryProviderId: Option[String],
    deliveryProviderNumber: Option[String],
    receivedAt: ZonedDateTime,
    receivedAtTz: ZonedDateTime,
    completedAt: Option[ZonedDateTime],
    completedAtTz: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def deriveUpdateFromPreviousState(other: OrderRecord): Option[OrderUpdate] = {
    val candidate = OrderUpdate(
      id = id.some,
      merchantId = if (merchantId != other.merchantId) merchantId.some else None,
      locationId = if (locationId != other.locationId) locationId else None,
      deviceId = if (deviceId != other.deviceId) deviceId else None,
      userId = if (userId != other.userId) userId else None,
      customerId = if (customerId != other.customerId) customerId else None,
      deliveryAddressId = if (deliveryAddressId != other.deliveryAddressId) deliveryAddressId else None,
      onlineOrderAttributeId =
        if (onlineOrderAttributeId != other.onlineOrderAttributeId) onlineOrderAttributeId else None,
      tag = if (tag != other.tag) tag else None,
      source = if (source != other.source) source else None,
      `type` = if (`type` != other.`type`) `type` else None,
      paymentType = if (paymentType != other.paymentType) paymentType else None,
      totalAmount = if (totalAmount != other.totalAmount) totalAmount else None,
      subtotalAmount = if (subtotalAmount != other.subtotalAmount) subtotalAmount else None,
      discountAmount = if (discountAmount != other.discountAmount) discountAmount else None,
      taxAmount = if (taxAmount != other.taxAmount) taxAmount else None,
      tipAmount = if (tipAmount != other.tipAmount) tipAmount else None,
      ticketDiscountAmount = if (ticketDiscountAmount != other.ticketDiscountAmount) ticketDiscountAmount else None,
      deliveryFeeAmount = if (deliveryFeeAmount != other.deliveryFeeAmount) deliveryFeeAmount else None,
      customerNotes = if (customerNotes != other.customerNotes) customerNotes else Seq.empty,
      merchantNotes = if (merchantNotes != other.merchantNotes) merchantNotes else Seq.empty,
      paymentStatus = if (paymentStatus != other.paymentStatus) paymentStatus else None,
      status = if (status != other.status) status else None,
      fulfillmentStatus = if (fulfillmentStatus != other.fulfillmentStatus) fulfillmentStatus else None,
      statusTransitions = if (statusTransitions != other.statusTransitions) statusTransitions.some else None,
      isInvoice = if (isInvoice != other.isInvoice) isInvoice.some else None,
      isFiscal = if (isFiscal != other.isFiscal) isFiscal.some else None,
      version = if (version != other.version) version.some else None,
      seating = if (seating != other.seating) seating else None,
      deliveryProvider = if (deliveryProvider != other.deliveryProvider) deliveryProvider else None,
      deliveryProviderId = if (deliveryProviderId != other.deliveryProviderId) deliveryProviderId else None,
      deliveryProviderNumber =
        if (deliveryProviderNumber != other.deliveryProviderNumber) deliveryProviderNumber else None,
      receivedAt = if (receivedAt != other.receivedAt) receivedAt.some else None,
      receivedAtTz = if (receivedAtTz != other.receivedAtTz) receivedAtTz.some else None,
      completedAt = if (completedAt != other.completedAt) completedAt else None,
      completedAtTz = if (completedAtTz != other.completedAtTz) completedAtTz else None,
    )
    if (candidate != OrderUpdate.empty(id)) candidate.some
    else none
  }

}

case class OrderUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    deviceId: Option[UUID],
    userId: Option[UUID],
    customerId: Option[UUID],
    deliveryAddressId: Option[UUID],
    onlineOrderAttributeId: Option[UUID],
    tag: ResettableString,
    source: Option[Source],
    `type`: Option[OrderType],
    paymentType: Option[OrderPaymentType],
    totalAmount: Option[BigDecimal],
    subtotalAmount: Option[BigDecimal],
    discountAmount: Option[BigDecimal],
    taxAmount: Option[BigDecimal],
    tipAmount: Option[BigDecimal],
    ticketDiscountAmount: Option[BigDecimal],
    deliveryFeeAmount: Option[BigDecimal],
    customerNotes: Seq[CustomerNote],
    merchantNotes: Seq[MerchantNote],
    paymentStatus: Option[PaymentStatus],
    status: Option[OrderStatus],
    fulfillmentStatus: Option[FulfillmentStatus],
    statusTransitions: Option[Seq[StatusTransition]],
    isInvoice: Option[Boolean],
    isFiscal: Option[Boolean],
    version: Option[Int],
    seating: ResettableSeating,
    deliveryProvider: Option[DeliveryProvider],
    deliveryProviderId: Option[String],
    deliveryProviderNumber: Option[String],
    receivedAt: Option[ZonedDateTime],
    receivedAtTz: Option[ZonedDateTime],
    completedAt: Option[ZonedDateTime],
    completedAtTz: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[OrderRecord] {
  require(
    completedAt.isDefined == completedAtTz.isDefined,
    s"Fields completedAt and completedAtTz should change at the same time. [$this]",
  )

  def inferCurrentStatusTransitions(currentStatus: Option[OrderStatus]): Seq[StatusTransition] = {
    val inferredStatusTransitions = {
      if (status.isDefined && currentStatus == status) Seq.empty
      else status.map(StatusTransition(_)).toSeq
    }
    statusTransitions.getOrElse(inferredStatusTransitions)
  }

  def toRecord: OrderRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderUpdate without a merchant id. [$this]")
    require(version.isDefined, s"Impossible to convert OrderUpdate without a version. [$this]")
    require(receivedAt.isDefined, s"Impossible to convert OrderUpdate without a received at. [$this]")
    require(receivedAtTz.isDefined, s"Impossible to convert OrderUpdate without a received at tz. [$this]")
    OrderRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId,
      deviceId = deviceId,
      userId = userId,
      customerId = customerId,
      deliveryAddressId = deliveryAddressId,
      onlineOrderAttributeId = onlineOrderAttributeId,
      number = None,
      tag = tag,
      source = source,
      `type` = `type`,
      paymentType = paymentType,
      totalAmount = totalAmount,
      subtotalAmount = subtotalAmount,
      discountAmount = discountAmount,
      taxAmount = taxAmount,
      tipAmount = tipAmount,
      ticketDiscountAmount = ticketDiscountAmount,
      deliveryFeeAmount = deliveryFeeAmount,
      customerNotes = customerNotes,
      merchantNotes = merchantNotes,
      paymentStatus = paymentStatus,
      status = status,
      fulfillmentStatus = fulfillmentStatus,
      statusTransitions = inferCurrentStatusTransitions(None),
      isInvoice = isInvoice.getOrElse(false),
      isFiscal = isFiscal.getOrElse(false),
      version = version.get,
      seating = seating,
      deliveryProvider = deliveryProvider,
      deliveryProviderId = deliveryProviderId,
      deliveryProviderNumber = deliveryProviderNumber,
      receivedAt = receivedAt.get,
      receivedAtTz = receivedAtTz.get,
      completedAt = completedAt,
      completedAtTz = completedAtTz,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderRecord): OrderRecord =
    OrderRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.orElse(record.locationId),
      deviceId = deviceId.orElse(record.deviceId),
      userId = userId.orElse(record.userId),
      customerId = customerId.orElse(record.customerId),
      deliveryAddressId = deliveryAddressId.orElse(record.deliveryAddressId),
      onlineOrderAttributeId = onlineOrderAttributeId.orElse(record.onlineOrderAttributeId),
      number = record.number,
      tag = tag.getOrElse(record.tag),
      source = source.orElse(record.source),
      `type` = `type`.orElse(record.`type`),
      paymentType = paymentType.orElse(record.paymentType),
      totalAmount = totalAmount.orElse(record.totalAmount),
      subtotalAmount = subtotalAmount.orElse(record.subtotalAmount),
      discountAmount = discountAmount.orElse(record.discountAmount),
      taxAmount = taxAmount.orElse(record.taxAmount),
      tipAmount = tipAmount.orElse(record.tipAmount),
      ticketDiscountAmount = ticketDiscountAmount.orElse(record.ticketDiscountAmount),
      deliveryFeeAmount = deliveryFeeAmount.orElse(record.deliveryFeeAmount),
      customerNotes = customerNotes.sortBy(_.createdAt),
      merchantNotes = merchantNotes.sortBy(_.createdAt),
      paymentStatus = paymentStatus.orElse(record.paymentStatus),
      status = status.orElse(record.status),
      fulfillmentStatus = fulfillmentStatus.orElse(record.fulfillmentStatus),
      statusTransitions =
        (inferCurrentStatusTransitions(record.status) ++ record.statusTransitions).distinct.sortBy(_.createdAt),
      isInvoice = isInvoice.getOrElse(record.isInvoice),
      isFiscal = isFiscal.getOrElse(record.isFiscal),
      version = version.getOrElse(record.version),
      seating = seating.getOrElse(record.seating),
      deliveryProvider = deliveryProvider.orElse(record.deliveryProvider),
      deliveryProviderId = deliveryProviderId.orElse(record.deliveryProviderId),
      deliveryProviderNumber = deliveryProviderNumber.orElse(record.deliveryProviderNumber),
      receivedAt = receivedAt.getOrElse(record.receivedAt),
      receivedAtTz = receivedAtTz.getOrElse(record.receivedAtTz),
      completedAt = completedAt.orElse(record.completedAt),
      completedAtTz = completedAtTz.orElse(record.completedAtTz),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object OrderUpdate {
  def empty(id: UUID): OrderUpdate =
    OrderUpdate(
      id = id.some,
      merchantId = None,
      locationId = None,
      deviceId = None,
      userId = None,
      customerId = None,
      deliveryAddressId = None,
      onlineOrderAttributeId = None,
      tag = None,
      source = None,
      `type` = None,
      paymentType = None,
      totalAmount = None,
      subtotalAmount = None,
      discountAmount = None,
      taxAmount = None,
      tipAmount = None,
      ticketDiscountAmount = None,
      deliveryFeeAmount = None,
      customerNotes = Seq.empty,
      merchantNotes = Seq.empty,
      paymentStatus = None,
      status = None,
      fulfillmentStatus = None,
      statusTransitions = None,
      isInvoice = None,
      isFiscal = None,
      version = None,
      seating = None,
      deliveryProvider = None,
      deliveryProviderId = None,
      deliveryProviderNumber = None,
      receivedAt = None,
      receivedAtTz = None,
      completedAt = None,
      completedAtTz = None,
    )
}
