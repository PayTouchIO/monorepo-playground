package io.paytouch.core.conversions

import io.paytouch.core.data.model.{ OrderUpdate, MerchantNote => MerchantNoteModel }
import io.paytouch.core.entities.{ MerchantNoteUpsertion, UserContext }
import io.paytouch.core.validators.RecoveredOrderUpsertion

trait OrderSyncConversions {
  def fromUpsertionToUpdate(upsertion: RecoveredOrderUpsertion)(implicit user: UserContext): OrderUpdate =
    OrderUpdate(
      id = Some(upsertion.orderId),
      merchantId = Some(user.merchantId),
      locationId = upsertion.locationId,
      deviceId = upsertion.deviceId,
      userId = upsertion.creatorUserId,
      customerId = upsertion.customerId,
      deliveryAddressId = upsertion.deliveryAddress.map(_.id),
      onlineOrderAttributeId = upsertion.onlineOrderAttribute.map(_.id),
      tag = upsertion.tag,
      source = Some(upsertion.source),
      `type` = Some(upsertion.`type`),
      paymentType = upsertion.paymentType,
      totalAmount = Some(upsertion.totalAmount),
      subtotalAmount = Some(upsertion.subtotalAmount),
      discountAmount = upsertion.discountAmount,
      taxAmount = Some(upsertion.taxAmount),
      tipAmount = upsertion.tipAmount,
      ticketDiscountAmount = upsertion.ticketDiscountAmount,
      deliveryFeeAmount = upsertion.deliveryFeeAmount,
      customerNotes = Seq.empty, // leave untouched
      merchantNotes = fromMerchantNoteUpsertionToModel(upsertion.merchantNotes),
      paymentStatus = Some(upsertion.paymentStatus),
      status = Some(upsertion.status),
      fulfillmentStatus = upsertion.fulfillmentStatus,
      statusTransitions = None,
      isInvoice = Some(upsertion.isInvoice),
      isFiscal = upsertion.isFiscal,
      version = Some(upsertion.version),
      seating = upsertion.seating,
      deliveryProvider = upsertion.deliveryProvider,
      deliveryProviderId = upsertion.deliveryProviderId,
      deliveryProviderNumber = upsertion.deliveryProviderNumber,
      receivedAt = Some(upsertion.receivedAt),
      receivedAtTz = Some(upsertion.receivedAtTz),
      completedAt = upsertion.completedAt,
      completedAtTz = upsertion.completedAtTz,
    )

  private def fromMerchantNoteUpsertionToModel(merchantNotes: Seq[MerchantNoteUpsertion]): Seq[MerchantNoteModel] =
    merchantNotes.map { merchantNote =>
      MerchantNoteModel(
        merchantNote.id,
        merchantNote.userId,
        merchantNote.body,
        merchantNote.createdAt,
      )
    }
}
