package io.paytouch.ordering.utils

import java.time.{ LocalTime, ZoneId, ZoneOffset, ZonedDateTime }
import java.util.{ Currency, UUID }

import scala.jdk.CollectionConverters._

import cats.implicits._

import org.scalacheck._

import io.paytouch.ordering.clients.paytouch.core.{ entities => CoreEntities }
import io.paytouch.ordering.clients.paytouch.core.entities.{ Address => _, _ }
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.clients.paytouch.core.entities.OrderItemTaxRateUpsertion
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ OrderType, _ }
import io.paytouch.ordering.entities.MonetaryAmount._

trait Generators {
  implicit class RichGen[T](gen: Gen[T]) {
    def instance = gen.sample.get
  }

  val genUUID: Gen[UUID] = Gen.uuid

  val genString: Gen[String] = Gen.listOfN(20, Gen.alphaChar).map(_.mkString)
  val genOptString: Gen[Option[String]] = Gen.option(genString)

  val genEmail: Gen[String] = for {
    w1 <- genString
    w2 <- genString
    w3 <- genString
    w4 <- genString
  } yield s"$w1.$w2@$w3.$w4"

  val genInt: Gen[Int] = Gen.chooseNum(1, 100)
  val genOptInt: Gen[Option[Int]] = Gen.option(genInt)

  val genBoolean: Gen[Boolean] = Gen.oneOf(true, false)
  val genOptBoolean: Gen[Option[Boolean]] = Gen.option(genBoolean)

  val genBigDecimal: Gen[BigDecimal] = genInt.map(x => x)
  val genCurrency: Gen[Currency] = Gen.const(Currency.getInstance("USD"))

  val genMonetaryAmount: Gen[MonetaryAmount] = genBigDecimal.map(_.USD)

  def genWords(n: Int): Gen[Seq[String]] = Gen.listOfN(n, genString)

  val genPostalCode: Gen[String] = for {
    postalCode <- Gen.chooseNum(10000, 99999)
  } yield postalCode.toString

  val genAddress: Gen[Address] = for {
    line1Prefix <- Gen.option(genString)
    line2Prefix <- Gen.option(genString)
    houseOrBuilding <- Gen.oneOf("House", "Building")
    postalCode <- Gen.option(genPostalCode)
    city <- Gen.option(genString)
    country <- Gen.option(genString)
    state <- Gen.option(genString)
    words <- genWords(2)
  } yield {
    val line1 = line1Prefix.map(l1p => Seq(l1p, words.mkString(" "), "Street").mkString(" "))
    val line2 =
      line2Prefix.map(l2p => Seq(l2p.capitalize, houseOrBuilding).mkString(" "))
    Address(
      line1 = line1,
      line2 = line2,
      city = city,
      state = state,
      country = country,
      postalCode = postalCode,
    )
  }

  val genCoreAddress: Gen[CoreEntities.Address] = {
    import io.scalaland.chimney.dsl._

    genAddress.map { address =>
      address
        .into[CoreEntities.Address]
        .withFieldConst(
          _.stateData,
          (address.country, address.state).mapN { (countryName, stateName) =>
            State(
              State.Name(stateName).some,
              State.Code("NY"), // not ideal
              Country(
                Country.Code("US"), // not ideal
                Country.Name(countryName),
              ).some,
            )
          },
        )
        .transform
    }
  }

  val genAddressUpsertion: Gen[AddressUpsertion] =
    genAddress.map { address =>
      AddressUpsertion(
        line1 = address.line1,
        line2 = address.line2,
        city = address.city,
        state = address.state,
        country = address.country,
        postalCode = address.postalCode,
      )
    }

  val genZonedDateTime: Gen[ZonedDateTime] =
    Gen
      .calendar
      .map(c =>
        ZonedDateTime
          .ofInstant(c.toInstant, ZoneOffset.UTC)
          .withYear(UtcTime.now.getYear),
      )

  val genLocalTime: Gen[LocalTime] = genZonedDateTime.map(_.toLocalTime)
  val genZoneId: Gen[ZoneId] =
    Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toSeq.map(ZoneId.of))

  val genImageUrls: Gen[ImageUrls] = genUUID.map { imageUploadId =>
    ImageUrls(
      imageUploadId = imageUploadId,
      urls = ImageSize
        .values
        .map(value => value -> s"my-url-$imageUploadId-${value.entryName}")
        .toMap,
    )
  }

  val genOnlineOrderAttribute: Gen[OnlineOrderAttribute] = for {
    id <- Arbitrary.arbitrary[UUID]
    acceptanceStatus <- genAcceptanceStatus
    rejectionReason <- Arbitrary.arbitrary[String]
    prepareByTime <- genLocalTime
    prepareByDateTime <- genZonedDateTime
    estimatedPrepTimeInMins <- Arbitrary.arbitrary[Int]
    rejectedAt <- genZonedDateTime
    acceptedAt <- genZonedDateTime
    estimatedReadyAt <- genZonedDateTime
    estimatedDeliveredAt <- genZonedDateTime
    cancellationReason <- Arbitrary.arbitrary[String]
  } yield OnlineOrderAttribute(
    id,
    acceptanceStatus,
    rejectionReason.some,
    prepareByTime.some,
    prepareByDateTime.some,
    estimatedPrepTimeInMins.some,
    rejectedAt.some,
    acceptedAt.some,
    estimatedReadyAt.some,
    estimatedDeliveredAt.some,
    cancellationReason.some,
  )

  val genGiftCardData: Gen[GiftCardData] = for {
    recipientEmail <- Arbitrary.arbitrary[String]
    amount <- Arbitrary.arbitrary[BigDecimal]
  } yield GiftCardData(recipientEmail, amount)

  // Resettable //

  val genResettableString: Gen[ResettableString] =
    Gen.option(genString).map(ResettableString.fromOptT)
  val genResettableInt: Gen[ResettableInt] =
    Gen.option(genInt).map(ResettableInt.fromOptT)
  val genResettableBigDecimal: Gen[ResettableBigDecimal] =
    Gen.option(genBigDecimal).map(ResettableBigDecimal.fromOptT)
  val genResettableUUID: Gen[ResettableUUID] =
    Gen.option(genUUID).map(ResettableUUID.fromOptT)
  val genResettableLocalTime: Gen[ResettableLocalTime] =
    Gen.option(genLocalTime).map(ResettableLocalTime.fromOptT)

  // Enumeratum //

  val genAcceptanceStatus: Gen[AcceptanceStatus] =
    Gen.oneOf(AcceptanceStatus.values.filterNot(_ == AcceptanceStatus.Open))
  val genCartItemType: Gen[CartItemType] = Gen.oneOf(CartItemType.values)
  val genCartStatus: Gen[CartStatus] = Gen.oneOf(CartStatus.values)
  val genDayType: Gen[Day] = Gen.oneOf(Day.values)
  val genModifierSetType: Gen[ModifierSetType] =
    Gen.oneOf(ModifierSetType.values)
  val genOrderSource: Gen[OrderSource] = Gen.oneOf(OrderSource.values)
  val genOrderType: Gen[OrderType] = Gen.oneOf(OrderType.values)
  val genUnitType: Gen[UnitType] = Gen.oneOf(UnitType.values)
  val genSetupType: Gen[SetupType] = Gen.oneOf(SetupType.values)

  // Random //
  def randomOrder(acceptanceStatus: Option[AcceptanceStatus] = None) =
    Order(
      id = UUID.randomUUID(),
      location = None,
      number = Some(genInt.instance.toString),
      status = OrderStatus.Received,
      paymentStatus = PaymentStatus.Paid,
      items = Seq.empty,
      bundles = Seq.empty,
      taxRates = Seq.empty,
      paymentTransactions = Seq.empty,
      subtotal = Some(genMonetaryAmount.instance),
      tax = Some(genMonetaryAmount.instance),
      tip = Some(genMonetaryAmount.instance),
      total = Some(genMonetaryAmount.instance),
      completedAt = Some(genZonedDateTime.instance),
      createdAt = genZonedDateTime.instance,
      updatedAt = genZonedDateTime.instance,
      onlineOrderAttribute = genOnlineOrderAttribute.sample.map { base =>
        base.copy(
          acceptanceStatus = acceptanceStatus.getOrElse(base.acceptanceStatus),
        )
      },
    )

  def randomOrderItem() =
    OrderItem(
      id = UUID.randomUUID(),
      productId = Some(UUID.randomUUID()),
      productName = Some(genString.instance),
      quantity = Some(genBigDecimal.instance),
      paymentStatus = Some(PaymentStatus.Pending),
      calculatedPrice = Some(genMonetaryAmount.instance),
      totalPrice = Some(genMonetaryAmount.instance),
      tax = Some(genMonetaryAmount.instance),
      taxRates = Seq.empty,
      variantOptions = Seq.empty,
      modifierOptions = Seq.empty,
    )

  def randomPaymentTransactionUpsertion() =
    PaymentTransactionUpsertion(
      id = UUID.randomUUID,
      `type` = TransactionType.Payment,
      paymentProcessorV2 = PaymentProcessor.Worldpay,
      paymentType = TransactionPaymentType.CreditCard,
      paymentDetails = GenericPaymentDetails(
        accountId = Some(genString.instance),
        applicationId = Some(genString.instance),
        authCode = Some(genString.instance),
        amount = genBigDecimal.instance,
        tipAmount = genBigDecimal.instance,
        currency = Currency.getInstance("USD"),
        transactionResult = CardTransactionResultType.Approved.some,
        transactionStatus = CardTransactionStatusType.Committed.some,
        maskPan = Some(genString.instance),
        cardType = Some(CardType.Visa),
        cardHolderName = Some(genString.instance),
        terminalId = Some(genString.instance),
        transactionReference = Some(genString.instance),
        gatewayTransactionReference = Some(genString.instance),
        entryMode = Some("Manual"),
        transactionStatusInfo = Some(genString.instance),
      ),
      paidAt = UtcTime.now,
      fees = Seq.empty,
    )

  def randomMerchant() =
    CoreMerchant(
      id = UUID.randomUUID,
      setupType = genSetupType.instance,
    )
}
