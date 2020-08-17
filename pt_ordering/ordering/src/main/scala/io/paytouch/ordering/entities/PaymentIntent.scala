package io.paytouch.ordering.entities

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import akka.http.scaladsl.model.Uri
import io.paytouch.ordering.entities.enums.{ ExposedName, PaymentIntentStatus, PaymentMethodType }

final case class PaymentIntent(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    orderItemIds: Seq[UUID],
    subtotal: MonetaryAmount,
    tax: MonetaryAmount,
    tip: MonetaryAmount,
    total: MonetaryAmount,
    paymentMethodType: PaymentMethodType,
    paymentProcessorData: Option[PaymentProcessorData],
    status: PaymentIntentStatus,
    metadata: PaymentIntentMetadata,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.PaymentIntent
}

final case class PaymentIntentCustomer(
    firstName: String,
    lastName: String,
    phoneNumber: ResettableString,
    email: ResettableString,
    enrollInLoyaltyProgram: Boolean,
  )

final case class PaymentIntentMetadata(customer: Option[PaymentIntentCustomer])

object PaymentIntentMetadata {
  def empty =
    PaymentIntentMetadata(
      customer = None,
    )
}

final case class PaymentIntentCreation(
    merchantId: UUID,
    orderId: UUID,
    orderItemIds: Seq[UUID],
    tipAmount: Option[BigDecimal],
    paymentMethodType: PaymentMethodType,
    metadata: Option[PaymentIntentMetadata],
    successReturnUrl: String,
    failureReturnUrl: String,
  ) extends CreationEntity[PaymentIntentUpsertion] {
  def asUpsert =
    PaymentIntentUpsertion(
      merchantId = merchantId,
      orderId = orderId,
      orderItemIds = orderItemIds,
      tipAmount = tipAmount.getOrElse(0),
      paymentMethodType = paymentMethodType,
      successReturnUrl = successReturnUrl,
      failureReturnUrl = failureReturnUrl,
      metadata = metadata.getOrElse(PaymentIntentMetadata.empty),
    )
}

final case class PaymentIntentUpsertion(
    merchantId: UUID,
    orderId: UUID,
    orderItemIds: Seq[UUID],
    tipAmount: BigDecimal,
    paymentMethodType: PaymentMethodType,
    successReturnUrl: Uri,
    failureReturnUrl: Uri,
    metadata: PaymentIntentMetadata,
  )
