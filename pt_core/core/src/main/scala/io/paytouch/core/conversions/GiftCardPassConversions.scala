package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.services._
import io.paytouch.core.validators.RecoveredOrderItemUpsertion

trait GiftCardPassConversions extends EntityConversion[GiftCardPassRecord, GiftCardPass] {
  val passService: PassService

  def fromRecordsAndOptionsToEntities(
      records: Seq[GiftCardPassRecord],
      transactionsPerGiftCard: Option[Map[GiftCardPassRecord, Seq[GiftCardPassTransaction]]],
    )(implicit
      user: UserContext,
    ): Seq[GiftCardPass] =
    records.map { record =>
      fromRecordAndOptionsToEntity(
        record,
        transactions = transactionsPerGiftCard
          .map(_.getOrElse(record, Seq.empty)),
      )
    }

  def fromRecordToEntity(record: GiftCardPassRecord)(implicit user: UserContext): GiftCardPass =
    fromRecordAndOptionsToEntity(record, None)

  private def fromRecordAndOptionsToEntity(
      record: GiftCardPassRecord,
      transactions: Option[Seq[GiftCardPassTransaction]],
    )(implicit
      user: UserContext,
    ): GiftCardPass =
    GiftCardPass(
      id = record.id,
      lookupId = record.lookupId,
      giftCardId = record.giftCardId,
      orderItemId = record.orderItemId,
      originalBalance = MonetaryAmount(record.originalAmount),
      balance = MonetaryAmount(record.balanceAmount),
      passPublicUrls = generatePassUrls(record),
      transactions = transactions,
      passInstalledAt = record.passInstalledAt,
      recipientEmail = record.recipientEmail,
      onlineCode = record.onlineCode.hyphenated,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  private def generatePassUrls(record: GiftCardPassRecord): PassUrls =
    PassUrls(
      ios = record
        .iosPassPublicUrl
        .as(passService.generateUrl(record.id, PassType.Ios, PassItemType.GiftCard)),
      android = record
        .androidPassPublicUrl
        .as(passService.generateUrl(record.id, PassType.Android, PassItemType.GiftCard)),
    )

  def fromUpsertionToUpdate(
      id: UUID,
      lookupId: String,
      giftCard: GiftCardRecord,
      item: RecoveredOrderItemUpsertion,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode,
    )(implicit
      user: UserContext,
    ): GiftCardPassUpdate =
    GiftCardPassUpdate(
      id = id.some,
      merchantId = user.merchantId.some,
      lookupId = lookupId.some,
      giftCardId = giftCard.id.some,
      orderItemId = item.id.some,
      originalAmount = item.priceAmount, // guaranteed non empty by validator
      balanceAmount = item.priceAmount, // guaranteed non empty by validator
      iosPassPublicUrl = None,
      androidPassPublicUrl = None,
      isCustomAmount = item.priceAmount.map(!giftCard.amounts.contains(_)),
      passInstalledAt = None,
      recipientEmail = None,
      onlineCode = onlineCode.some,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      lookupId: String,
      giftCard: GiftCardRecord,
      item: OrderItemRecord,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode,
    )(implicit
      user: UserContext,
    ): GiftCardPassUpdate =
    GiftCardPassUpdate(
      id = id.some,
      merchantId = user.merchantId.some,
      lookupId = lookupId.some,
      giftCardId = giftCard.id.some,
      orderItemId = item.id.some,
      originalAmount = item.priceAmount, // guaranteed non empty by validator
      balanceAmount = item.priceAmount, // guaranteed non empty by validator
      iosPassPublicUrl = None,
      androidPassPublicUrl = None,
      isCustomAmount = item.priceAmount.map(!giftCard.amounts.contains(_)),
      passInstalledAt = None,
      recipientEmail = None,
      onlineCode = onlineCode.some,
    )

  def toGiftCardPassSalesReport(
      salesReport: (Int, Int, BigDecimal),
    )(implicit
      user: UserContext,
    ): GiftCardPassSalesReport =
    salesReport match {
      case (count, customers, valueAmount) => GiftCardPassSalesReport(count, customers, valueAmount)
    }
}
