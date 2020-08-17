package io.paytouch.utils

import java.time._
import java.util.{ Currency, UUID }

import scala.jdk.CollectionConverters._

import cats.implicits._

import org.scalacheck._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.clients.urbanairship.entities.PassUpsertion
import io.paytouch.core.data.model
import io.paytouch.core.data.model.enums.{ CardTransactionResultType, _ }
import io.paytouch.core.data.model.enums.UnitType._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.services._
import io.paytouch.core.utils.{ StateUtils, UtcTime }

trait Generators {
  implicit def toRichGen[T](gen: Gen[T]): RichGen[T] = new RichGen(gen)

  class RichGen[T](gen: Gen[T]) {
    def instance = gen.sample.get
  }

  def randomWord: String = randomWords(1, allCapitalized = true)

  def randomWords: String = randomWords(2, allCapitalized = true)

  def randomWords(n: Int, allCapitalized: Boolean): String =
    genWords(n)
      .instance
      .map(w => if (allCapitalized) w.capitalize else w)
      .mkString(" ")
      .capitalize

  def randomText = Gen.listOfN(300, Gen.alphaChar).map(_.mkString).instance

  def randomEmail = genEmail.instance

  def randomNumericString = genNumericString(9).instance

  def randomUpc = genUpc.instance

  val genWord: Gen[String] = Gen.oneOf(BaconIpsumHelper.words)
  def genWords(n: Int): Gen[Seq[String]] = Gen.listOfN(n, genWord)
  def genPassword(n: Int): Gen[String] = Gen.listOfN(n, Gen.alphaStr).map(_.mkString)

  val genEmail: Gen[String] = for {
    w1 <- genWord
    w2 <- genWord
    w3 <- genWord
    w4 <- genWord
  } yield s"$w1.$w2@$w3.$w4"

  val genInt: Gen[Int] = Gen.chooseNum(1, 100)
  val genOptInt: Gen[Option[Int]] = Gen.option(genInt)

  def randomInt = genInt.instance

  val genBoolean: Gen[Boolean] = Gen.oneOf(true, false)
  val genOptBoolean: Gen[Option[Boolean]] = Gen.option(genBoolean)
  val genBigDecimal: Gen[BigDecimal] = genInt.map(x => x)
  val genMonetaryAmount: Gen[MonetaryAmount] = genBigDecimal.map(_.USD)
  val genCurrency: Gen[Currency] = Gen.oneOf(Currency.getInstance("USD"), Currency.getInstance("EUR"))
  val genLookupId: Gen[String] = Gen.listOfN(20, Gen.alphaNumChar).map(_.mkString)

  // Vlad: If I use random[ZonedDateTime] I get:
  // 2020-12-16T00:00Z != 2020-12-16T00:00Z[UTC]
  // but if I use UtcTime.now my test passes. Why?
  val genZonedDateTime: Gen[ZonedDateTime] =
    Gen
      .calendar
      .map { c =>
        ZonedDateTime
          .ofInstant(c.toInstant, ZoneId.of("UTC"))
          .withYear(UtcTime.now.getYear)
      }

  val genZonedDateTimeInThePast: Gen[ZonedDateTime] = {
    val aboutThreeMonthsInMills = Duration.ofDays(90).toMillis
    for {
      randomMillis <- Gen.chooseNum(0L, aboutThreeMonthsInMills)
    } yield UtcTime.now.minus(Duration.ofMillis(randomMillis))
  }
  val genZonedDateTimeInTheFuture: Gen[ZonedDateTime] = {
    val aboutThreeMonthsInMills = Duration.ofDays(90).toMillis
    for {
      randomMillis <- Gen.chooseNum(0L, aboutThreeMonthsInMills)
    } yield UtcTime.now.plus(Duration.ofMillis(randomMillis))
  }

  val genLocalTime: Gen[LocalTime] = {
    // The offset is an attempt to work around this error:
    //
    //   [error] cannot create an instance for class io.paytouch.core.reports.resources.sales.SalesSumFSpec
    //   [error]   caused by java.time.DateTimeException: Invalid value for NanoOfDay (valid values 0 - 86399999999999): 86400000000000
    //
    val offsetNanos = Duration.ofSeconds(1).toNanos
    Gen
      .chooseNum(LocalTime.MIN.toNanoOfDay + offsetNanos, LocalTime.MAX.toNanoOfDay - offsetNanos)
      .map(LocalTime.ofNanoOfDay)
  }

  val genLocalDate: Gen[LocalDate] = genZonedDateTime.map(_.toLocalDate)
  val genZoneId: Gen[ZoneId] = Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toSeq.map(ZoneId.of))

  val genModifierOptionCount: Gen[ModifierOptionCount] = for {
    from <- Gen.chooseNum(0, 10)
    to <- Gen.chooseNum(10, 20)
    isSome <- Gen.oneOf(true, false)
  } yield ModifierOptionCount.unsafeApply(Minimum(from), if (isSome) Maximum(to).some else None)

  val genPostalCode: Gen[String] = for {
    postalCode <- Gen.chooseNum(10000, 99999)
  } yield postalCode.toString

  val genCountry: Gen[Country] =
    Gen.oneOf(UtilService.Geo.countriesWithSupportedStates)

  def genState(countryCode: CountryCode): Gen[State] =
    Gen.oneOf(UtilService.Geo.states(countryCode))

  val genAddressUpsertion: Gen[AddressUpsertion] = for {
    line1Prefix <- genWord
    line2Prefix <- genWord
    houseOrBuilding <- Gen.oneOf("House", "Building")
    postalCode <- genPostalCode
    city <- genWord
    country <- genCountry
    state <- genState(CountryCode(country.code))
    words <- genWords(2)
  } yield {
    val line1 = Seq(line1Prefix, words.mkString(" "), "Street").mkString(" ")
    val line2 = Seq(line2Prefix.capitalize, houseOrBuilding).mkString(" ")
    AddressUpsertion(
      line1 = line1.some,
      line2 = line2.some,
      city = city.some,
      state = state.name.some,
      country = country.name.some,
      stateCode = state.code.some,
      countryCode = country.code.some,
      postalCode = postalCode.some,
    )
  }

  val genAddressImprovedUpsertion: Gen[AddressImprovedUpsertion] =
    for {
      upsertion <- genAddressUpsertion
      country <- genCountry
      state <- genState(CountryCode(country.code))
    } yield AddressImprovedUpsertion(
      line1 = upsertion.line1,
      line2 = upsertion.line2,
      city = upsertion.city,
      stateCode = state.code.some,
      countryCode = country.code.some,
      postalCode = upsertion.postalCode,
    )

  val genAddressSync: Gen[AddressSync] =
    genAddressUpsertion.map(_.toAddressSync)

  val genAddressImprovedSync: Gen[AddressImprovedSync] =
    genAddressImprovedUpsertion.map(_.toAddressImprovedSync)

  val genAddress: Gen[Address] =
    genAddressUpsertion.map { upsertion =>
      val country = (
        upsertion.countryCode,
        upsertion.country,
      ).mapN(Country)

      Address(
        line1 = upsertion.line1,
        line2 = upsertion.line2,
        city = upsertion.city,
        state = upsertion.state,
        country = upsertion.country,
        stateData = (upsertion.state.some, upsertion.stateCode, country.some).mapN(AddressState),
        countryData = country,
        postalCode = upsertion.postalCode,
      )
    }

  val genAddressImproved: Gen[AddressImproved] =
    for {
      upsertion <- genAddressImprovedUpsertion
      country <- genCountry
      state <- genState(CountryCode(country.code))
    } yield AddressImproved(
      line1 = upsertion.line1,
      line2 = upsertion.line2,
      city = upsertion.city,
      stateData = state.some.map(_.toAddressState),
      countryData = country.some,
      postalCode = upsertion.postalCode,
    )

  def genOrderType(businessType: BusinessType): Gen[OrderType] =
    Gen.oneOf(OrderType.byBusinessType(businessType))

  def genQuantity(unitType: UnitType): Gen[BigDecimal] =
    genInt.map { n =>
      unitType match {
        case `Unit` => 1 + n
        case _      => 1 + n / 10
      }
    }

  val genColor: Gen[String] = Gen.oneOf("e62565", "9b2fae", "2b98f0", "fc5830", "fdc02f", "617d8a")
  def genNumbers(n: Int): Gen[Seq[Int]] = Gen.listOfN(n, Gen.chooseNum(0, 9))
  def genNumericString(n: Int): Gen[String] = genNumbers(n).map(_.mkString)

  val genUpc: Gen[String] = genNumericString(15)

  val genArticleCreation: Gen[ArticleCreation] =
    for {
      name <- genWord
      description <- Gen.option(Gen.const(randomWords(15, allCapitalized = false)))
      sku <- Gen.const(s"${UUID.randomUUID}-sku")
      upc <- genUpc
      cost <- Gen.option(genBigDecimal)
      price <- genBigDecimal
      unit <- Gen.oneOf(UnitType.values)
      margin <- Gen.option(genBigDecimal)
      trackInventory <- Gen.option(genBoolean)
      trackInventoryParts <- Gen.option(genBoolean)
      active <- Gen.option(genBoolean)
      applyPricingToAllLocations <- Gen.option(genBoolean)
      discountable <- Gen.option(genBoolean)
      avatarBgColor <- Gen.option(genColor)
      isService <- Gen.option(genBoolean)
      orderRoutingBar <- Gen.option(genBoolean)
      orderRoutingKitchen <- Gen.option(genBoolean)
      orderRoutingEnabled <- Gen.option(genBoolean)
      scope <- genArticleScope
      makesQuantity <- Gen.option(genBigDecimal)
    } yield ArticleCreation(
      name = name,
      description = description,
      categoryIds = Seq.empty,
      brandId = None,
      supplierIds = Seq.empty,
      sku = sku,
      upc = upc,
      cost = cost,
      price = price,
      unit = unit,
      margin = margin,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      active = active,
      applyPricingToAllLocations = applyPricingToAllLocations,
      discountable = discountable,
      avatarBgColor = avatarBgColor,
      isService = isService,
      orderRoutingBar = orderRoutingBar,
      orderRoutingKitchen = orderRoutingKitchen,
      orderRoutingEnabled = orderRoutingEnabled,
      variants = Seq.empty,
      variantProducts = Seq.empty,
      locationOverrides = Map.empty,
      imageUploadIds = Seq.empty,
      scope = scope,
      `type` = None,
      isCombo = false,
      makesQuantity = makesQuantity,
      bundleSets = Seq.empty,
    )

  val genProductCreation: Gen[ProductCreation] = genArticleCreation.map { creation =>
    ProductCreation(
      name = creation.name,
      description = creation.description,
      categoryIds = creation.categoryIds,
      brandId = creation.brandId,
      supplierIds = creation.supplierIds,
      sku = creation.sku,
      upc = creation.upc,
      cost = creation.cost,
      price = creation.price,
      unit = creation.unit,
      margin = creation.margin,
      trackInventory = creation.trackInventory,
      trackInventoryParts = creation.trackInventoryParts,
      active = creation.active,
      applyPricingToAllLocations = creation.applyPricingToAllLocations,
      discountable = creation.discountable,
      avatarBgColor = creation.avatarBgColor,
      isService = creation.isService,
      orderRoutingBar = creation.orderRoutingBar,
      orderRoutingKitchen = creation.orderRoutingKitchen,
      orderRoutingEnabled = creation.orderRoutingEnabled,
      variants = creation.variants,
      variantProducts = Seq.empty,
      locationOverrides = Map.empty,
      imageUploadIds = creation.imageUploadIds,
    )
  }

  val genPartCreation: Gen[PartCreation] = genArticleCreation.map { creation =>
    PartCreation(
      name = creation.name,
      description = creation.description,
      categoryIds = creation.categoryIds,
      brandId = creation.brandId,
      supplierIds = creation.supplierIds,
      sku = creation.sku,
      upc = creation.upc,
      cost = creation.cost,
      unit = creation.unit,
      trackInventory = creation.trackInventory,
      trackInventoryParts = creation.trackInventoryParts,
      active = creation.active,
      applyPricingToAllLocations = creation.applyPricingToAllLocations,
      variants = creation.variants,
      variantProducts = Seq.empty,
      locationOverrides = Map.empty,
    )
  }

  val genRecipeCreation: Gen[RecipeCreation] = genArticleCreation.map { creation =>
    RecipeCreation(
      name = creation.name,
      description = creation.description,
      categoryIds = creation.categoryIds,
      brandId = creation.brandId,
      supplierIds = creation.supplierIds,
      sku = creation.sku,
      upc = creation.upc,
      cost = creation.cost,
      unit = creation.unit,
      trackInventory = creation.trackInventory,
      trackInventoryParts = creation.trackInventoryParts,
      active = creation.active,
      applyPricingToAllLocations = creation.applyPricingToAllLocations,
      locationOverrides = Map.empty,
      makesQuantity = creation.makesQuantity,
    )
  }

  val genArticleUpdate: Gen[ArticleUpdate] = for {
    name <- Gen.option(genWord)
    description <- Gen.option(Gen.const(randomWords(15, allCapitalized = false)))
    sku <- Gen.const(s"${UUID.randomUUID}-sku")
    upc <- genUpc
    cost <- Gen.option(genBigDecimal)
    price <- Gen.option(genBigDecimal)
    unit <- Gen.option(Gen.oneOf(UnitType.values))
    margin <- Gen.option(genBigDecimal)
    trackInventory <- Gen.option(genBoolean)
    trackInventoryParts <- Gen.option(genBoolean)
    active <- Gen.option(genBoolean)
    applyPricingToAllLocations <- Gen.option(genBoolean)
    discountable <- Gen.option(genBoolean)
    avatarBgColor <- Gen.option(genColor)
    isService <- Gen.option(genBoolean)
    orderRoutingBar <- Gen.option(genBoolean)
    orderRoutingKitchen <- Gen.option(genBoolean)
    orderRoutingEnabled <- Gen.option(genBoolean)
    makesQuantity <- Gen.option(genBigDecimal)
  } yield ArticleUpdate(
    name = name,
    description = description,
    categoryIds = None,
    brandId = None,
    supplierIds = None,
    sku = sku,
    upc = upc,
    cost = cost,
    price = price,
    unit = unit,
    margin = margin,
    trackInventory = trackInventory,
    trackInventoryParts = trackInventoryParts,
    active = active,
    applyPricingToAllLocations = applyPricingToAllLocations,
    discountable = discountable,
    avatarBgColor = avatarBgColor,
    isService = isService,
    orderRoutingBar = orderRoutingBar,
    orderRoutingKitchen = orderRoutingKitchen,
    orderRoutingEnabled = orderRoutingEnabled,
    variants = None,
    variantProducts = None,
    locationOverrides = Map.empty,
    imageUploadIds = None,
    notes = None,
    scope = None,
    `type` = None,
    isCombo = None,
    makesQuantity = makesQuantity,
    bundleSets = None,
  )

  val genProductUpdate: Gen[ProductUpdate] = genArticleUpdate.map { update =>
    ProductUpdate(
      name = update.name,
      description = update.description,
      categoryIds = update.categoryIds,
      brandId = update.brandId,
      supplierIds = update.supplierIds,
      sku = update.sku,
      upc = update.upc,
      cost = update.cost,
      price = update.price,
      unit = update.unit,
      margin = update.margin,
      trackInventory = update.trackInventory,
      trackInventoryParts = update.trackInventoryParts,
      active = update.active,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      discountable = update.discountable,
      avatarBgColor = update.avatarBgColor,
      isService = update.isService,
      orderRoutingBar = update.orderRoutingBar,
      orderRoutingKitchen = update.orderRoutingKitchen,
      orderRoutingEnabled = update.orderRoutingEnabled,
      variants = update.variants,
      variantProducts = None,
      locationOverrides = Map.empty,
      imageUploadIds = update.imageUploadIds,
      notes = update.notes,
    )
  }

  val genPartUpdate: Gen[PartUpdate] = genArticleUpdate.map { update =>
    PartUpdate(
      name = update.name,
      description = update.description,
      categoryIds = update.categoryIds,
      brandId = update.brandId,
      supplierIds = update.supplierIds,
      sku = update.sku,
      upc = update.upc,
      cost = update.cost,
      unit = update.unit,
      trackInventory = update.trackInventory,
      trackInventoryParts = update.trackInventoryParts,
      active = update.active,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      variants = update.variants,
      variantProducts = None,
      locationOverrides = Map.empty,
      notes = update.notes,
    )
  }

  val genRecipeUpdate: Gen[RecipeUpdate] = genArticleUpdate.map { update =>
    RecipeUpdate(
      name = update.name,
      description = update.description,
      categoryIds = update.categoryIds,
      brandId = update.brandId,
      supplierIds = update.supplierIds,
      sku = update.sku,
      upc = update.upc,
      cost = update.cost,
      unit = update.unit,
      trackInventory = update.trackInventory,
      trackInventoryParts = update.trackInventoryParts,
      active = update.active,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      locationOverrides = Map.empty,
      notes = update.notes,
      makesQuantity = update.makesQuantity,
    )
  }

  val genVariantArticleCreation: Gen[VariantArticleCreation] = {
    for {
      sku <- Gen.const(s"${UUID.randomUUID}-sku")
      upc <- genUpc
      cost <- Gen.option(genBigDecimal)
      price <- genBigDecimal
      unit <- Gen.oneOf(UnitType.values)
      margin <- Gen.option(genBigDecimal)
      applyPricingToAllLocations <- Gen.option(genBoolean)
      discountable <- Gen.option(genBoolean)
      avatarBgColor <- Gen.option(genColor)
      isService <- Gen.option(genBoolean)
      orderRoutingBar <- Gen.option(genBoolean)
      orderRoutingKitchen <- Gen.option(genBoolean)
      orderRoutingEnabled <- Gen.option(genBoolean)
    } yield VariantArticleCreation(
      id = UUID.randomUUID,
      optionIds = Seq.empty,
      sku = sku,
      upc = upc,
      cost = cost,
      price = Some(price),
      unit = Some(unit),
      margin = margin,
      applyPricingToAllLocations = applyPricingToAllLocations,
      discountable = discountable,
      avatarBgColor = avatarBgColor,
      isService = isService,
      orderRoutingBar = orderRoutingBar,
      orderRoutingKitchen = orderRoutingKitchen,
      orderRoutingEnabled = orderRoutingEnabled,
      locationOverrides = Map.empty,
    )
  }

  val genVariantProductCreation: Gen[VariantProductCreation] = genVariantArticleCreation.map { creation =>
    VariantProductCreation(
      id = creation.id,
      optionIds = creation.optionIds,
      sku = creation.sku,
      upc = creation.upc,
      cost = creation.cost,
      price = creation.price,
      unit = creation.unit,
      margin = creation.margin,
      applyPricingToAllLocations = creation.applyPricingToAllLocations,
      discountable = creation.discountable,
      avatarBgColor = creation.avatarBgColor,
      isService = creation.isService,
      orderRoutingBar = creation.orderRoutingBar,
      orderRoutingKitchen = creation.orderRoutingKitchen,
      orderRoutingEnabled = creation.orderRoutingEnabled,
      locationOverrides = Map.empty,
    )
  }

  val genVariantPartCreation: Gen[VariantPartCreation] = genVariantArticleCreation.map { creation =>
    VariantPartCreation(
      id = creation.id,
      optionIds = creation.optionIds,
      sku = creation.sku,
      upc = creation.upc,
      cost = creation.cost,
      unit = creation.unit,
      applyPricingToAllLocations = creation.applyPricingToAllLocations,
      locationOverrides = Map.empty,
    )
  }

  val genVariantArticleUpdate: Gen[VariantArticleUpdate] = for {
    sku <- Gen.const(s"${UUID.randomUUID}-sku")
    upc <- genUpc
    cost <- Gen.option(genBigDecimal)
    price <- genBigDecimal
    unit <- Gen.oneOf(UnitType.values)
    margin <- Gen.option(genBigDecimal)
    applyPricingToAllLocations <- Gen.option(genBoolean)
    discountable <- Gen.option(genBoolean)
    avatarBgColor <- Gen.option(genColor)
    isService <- Gen.option(genBoolean)
    orderRoutingBar <- Gen.option(genBoolean)
    orderRoutingKitchen <- Gen.option(genBoolean)
    orderRoutingEnabled <- Gen.option(genBoolean)
  } yield VariantArticleUpdate(
    id = UUID.randomUUID,
    optionIds = Seq.empty,
    sku = sku,
    upc = upc,
    cost = cost,
    price = Some(price),
    unit = Some(unit),
    margin = margin,
    applyPricingToAllLocations = applyPricingToAllLocations,
    discountable = discountable,
    avatarBgColor = avatarBgColor,
    isService = isService,
    orderRoutingBar = orderRoutingBar,
    orderRoutingKitchen = orderRoutingKitchen,
    orderRoutingEnabled = orderRoutingEnabled,
    locationOverrides = Map.empty,
  )

  val genVariantProductUpdate: Gen[VariantProductUpdate] = genVariantArticleUpdate.map { update =>
    VariantProductUpdate(
      id = update.id,
      optionIds = update.optionIds,
      sku = update.sku,
      upc = update.upc,
      cost = update.cost,
      price = update.price,
      unit = update.unit,
      margin = update.margin,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      discountable = update.discountable,
      avatarBgColor = update.avatarBgColor,
      isService = update.isService,
      orderRoutingBar = update.orderRoutingBar,
      orderRoutingKitchen = update.orderRoutingKitchen,
      orderRoutingEnabled = update.orderRoutingEnabled,
      locationOverrides = Map.empty,
    )
  }

  val genVariantPartUpdate: Gen[VariantPartUpdate] = genVariantArticleUpdate.map { update =>
    VariantPartUpdate(
      id = update.id,
      optionIds = update.optionIds,
      sku = update.sku,
      upc = update.upc,
      cost = update.cost,
      unit = update.unit,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      locationOverrides = Map.empty,
    )
  }

  val genSendReceiptData: Gen[SendReceiptData] =
    for {
      email <- genEmail
    } yield SendReceiptData(email)

  val genOrderItemVariantOptionUpsertion: Gen[OrderItemVariantOptionUpsertion] =
    for {
      id <- Gen.option(Gen.uuid)
      optionName <- genWord
      optionTypeName <- genWord
      position <- genOptInt
    } yield OrderItemVariantOptionUpsertion(id, Some(optionName), Some(optionTypeName), position)

  val genPassUpsertion: Gen[PassUpsertion] =
    Gen.const(PassUpsertion())

  val genPaymentDetails: Gen[PaymentDetails] = for {
    amountWithoutTip <- Gen.posNum[Double]
    tipAmount <- Gen.posNum[Double]
    currency <- Gen.option(genCurrency)
    authCode <- Gen.option(genWord)
    maskPan <- Gen.option(genWord)
    cardHash <- Gen.option(genWord)
    cardReference <- Gen.option(genWord)
    cardType <- Gen.option(genCardType)
    terminalName <- Gen.option(genWord)
    terminalId <- Gen.option(genWord)
    transactionResult <- Gen.option(genCardTransactionResultType)
    transactionStatus <- Gen.option(genCardTransactionStatusType)
    transactionReference <- Gen.option(genWord)
    last4Digits <- Gen.option(genWord)
    paidInAmount <- Gen.option(genBigDecimal)
    paidOutAmount <- Gen.option(genBigDecimal)
    isStandalone <- Gen.option(genBoolean)
    customerId <- Gen.option(Gen.uuid)
    preauth <- genBoolean
  } yield PaymentDetails(
    amount = BigDecimal(amountWithoutTip + tipAmount).some,
    currency = currency,
    authCode = authCode,
    maskPan = maskPan,
    cardHash = cardHash,
    cardReference = cardReference,
    cardType = cardType,
    terminalName = terminalName,
    terminalId = terminalId,
    transactionResult = transactionResult,
    transactionStatus = transactionStatus,
    transactionReference = transactionReference,
    last4Digits = last4Digits,
    paidInAmount = paidInAmount,
    paidOutAmount = paidOutAmount,
    giftCardPassId = None,
    giftCardPassTransactionId = None,
    isStandalone = isStandalone,
    customerId = customerId,
    tipAmount = BigDecimal(tipAmount),
    preauth = preauth,
  )

  val genUserCreation: Gen[UserCreation] = for {
    userRoleId <- Gen.option(Gen.uuid)
    firstName <- genWord
    lastName <- genWord
    password <- genPassword(8)
    pin <- Gen.option(genNumericString(4))
    email <- genEmail
    dob <- Gen.option(genLocalDate)
    phoneNumber <- Gen.option(genWord)
    address <- genAddressUpsertion
    avatarBgColor <- Gen.option(genWord)
    active <- genBoolean
    hourlyRateAmount <- Gen.option(genBigDecimal)
    overtimeRateAmount <- Gen.option(genBigDecimal)
    paySchedule <- Gen.option(genPaySchedule)
    avatarImageId <- genResettableUUID
  } yield UserCreation(
    userRoleId = userRoleId,
    firstName = firstName,
    lastName = lastName,
    password = password,
    pin = pin,
    email = email,
    dob = dob,
    phoneNumber = phoneNumber,
    address = address,
    avatarBgColor = avatarBgColor,
    active = active,
    isOwner = None,
    hourlyRateAmount = hourlyRateAmount,
    overtimeRateAmount = overtimeRateAmount,
    paySchedule = paySchedule,
    avatarImageId = avatarImageId,
    locationIds = None,
  )

  val genUserUpdate: Gen[UserUpdate] = genUserCreation.map(_.asUpdate)

  val genAdminCreation: Gen[AdminCreation] = for {
    firstName <- genWord
    lastName <- genWord
    password <- genPassword(8)
    email <- genEmail
  } yield AdminCreation(
    firstName = firstName,
    lastName = lastName,
    password = password,
    email = email,
  )

  val genAdminUpdate: Gen[AdminUpdate] = genAdminCreation.map(_.asUpdate)

  val genOnlineCode: Gen[io.paytouch.GiftCardPass.OnlineCode] =
    Gen.resultOf[Unit, io.paytouch.GiftCardPass.OnlineCode](_ => GiftCardPassService.generateOnlineCode())

  // Resettable //

  val genResettableString: Gen[ResettableString] = Gen.option(genWord).map(ResettableString.fromOptT)
  val genResettableInt: Gen[ResettableInt] = Gen.option(genInt).map(ResettableInt.fromOptT)
  val genResettableBigDecimal: Gen[ResettableBigDecimal] = Gen.option(genBigDecimal).map(ResettableBigDecimal.fromOptT)
  val genResettableUUID: Gen[ResettableUUID] = Gen.const(ResettableUUID.ignore)
  val genResettableLocalDate: Gen[ResettableLocalDate] = Gen.option(genLocalDate).map(ResettableLocalDate.fromOptT)
  val genResettableZonedDateTime: Gen[ResettableZonedDateTime] =
    Gen.option(genZonedDateTime).map(ResettableZonedDateTime.fromOptT)

  // Enumeratum //

  val genAcceptanceStatus: Gen[AcceptanceStatus] =
    Gen.oneOf(AcceptanceStatus.values.filterNot(_ == AcceptanceStatus.Open))
  val genExposedName: Gen[ExposedName] = Gen.oneOf(ExposedName.values)
  val genModifierSetType: Gen[ModifierSetType] = Gen.oneOf(ModifierSetType.values)
  val genSource: Gen[Source] = Gen.oneOf(Source.values)
  val genOrderType: Gen[OrderType] = Gen.oneOf(OrderType.values)
  val genQuantityChangeReason: Gen[QuantityChangeReason] = Gen.oneOf(QuantityChangeReason.values)
  val genChangeReason: Gen[ChangeReason] = Gen.oneOf(ChangeReason.values)
  val genOrderPaymentType: Gen[OrderPaymentType] = Gen.oneOf(OrderPaymentType.values)
  val genPaymentStatus: Gen[PaymentStatus] = Gen.oneOf(PaymentStatus.values)
  val genPositivePaymentStatus: Gen[PaymentStatus] = Gen.oneOf(PaymentStatus.isPositive)
  val genOrderStatus: Gen[OrderStatus] = Gen.oneOf(OrderStatus.values)
  val genPositiveOrderStatus: Gen[OrderStatus] = Gen.oneOf(OrderStatus.positiveValues)
  val genFulfillmentStatus: Gen[FulfillmentStatus] = Gen.oneOf(FulfillmentStatus.values)
  val genTransactionPaymentType: Gen[TransactionPaymentType] = Gen.oneOf(TransactionPaymentType.values)
  val genTransactionType: Gen[TransactionType] = Gen.oneOf(TransactionType.values)
  val genBusinessType: Gen[BusinessType] = Gen.oneOf(BusinessType.values)
  val genRestaurantType: Gen[RestaurantType] = Gen.oneOf(RestaurantType.values)
  val genDiscountType: Gen[DiscountType] = Gen.oneOf(DiscountType.values)
  val genPaySchedule: Gen[PaySchedule] = Gen.oneOf(PaySchedule.values)
  val genShiftStatus: Gen[ShiftStatus] = Gen.oneOf(ShiftStatus.values)
  val genTicketStatus: Gen[TicketStatus] = Gen.oneOf(TicketStatus.values)
  val genTimeOffType: Gen[TimeOffType] = Gen.oneOf(TimeOffType.values)
  val genFrequencyInterval: Gen[FrequencyInterval] = Gen.oneOf(FrequencyInterval.values)
  val genUnitType: Gen[UnitType] = Gen.oneOf(UnitType.values)
  val genCashDrawerStatus: Gen[CashDrawerStatus] = Gen.oneOf(CashDrawerStatus.values)
  val genCashDrawerActivityType: Gen[CashDrawerActivityType] = Gen.oneOf(CashDrawerActivityType.values)
  val genLoyaltyProgramType: Gen[LoyaltyProgramType] = Gen.oneOf(LoyaltyProgramType.values)
  val genRewardType: Gen[RewardType] = Gen.oneOf(RewardType.values)
  val genCardType: Gen[CardType] = Gen.oneOf(CardType.values)
  val genCardTransactionResultType: Gen[CardTransactionResultType] = Gen.oneOf(CardTransactionResultType.values)
  val genCardTransactionStatusType: Gen[CardTransactionStatusType] = Gen.oneOf(CardTransactionStatusType.values)
  val genMerchantSetupSteps: Gen[MerchantSetupSteps] = Gen.oneOf(MerchantSetupSteps.values)
  val genPurchaseOrderPaymentStatus: Gen[PurchaseOrderPaymentStatus] = Gen.oneOf(PurchaseOrderPaymentStatus.values)
  val genReceivingOrderPaymentStatus: Gen[ReceivingOrderPaymentStatus] = Gen.oneOf(ReceivingOrderPaymentStatus.values)
  val genReceivingOrderPaymentMethod: Gen[ReceivingOrderPaymentMethod] = Gen.oneOf(ReceivingOrderPaymentMethod.values)
  val genReceivingObjectStatus: Gen[ReceivingObjectStatus] = Gen.oneOf(ReceivingObjectStatus.values)
  val genReceivingOrderStatus: Gen[ReceivingOrderStatus] = Gen.oneOf(ReceivingOrderStatus.values)
  val genTransferOrderType: Gen[TransferOrderType] = Gen.oneOf(TransferOrderType.values)
  val genReturnStatus: Gen[ReturnOrderStatus] = Gen.oneOf(ReturnOrderStatus.values)
  val genReturnReason: Gen[ReturnOrderReason] = Gen.oneOf(ReturnOrderReason.values)
  val genInventoryCountStatus: Gen[InventoryCountStatus] = Gen.oneOf(InventoryCountStatus.values)
  val genArticleScope: Gen[ArticleScope] = Gen.oneOf(ArticleScope.values)
  val genPaymentTransactionFeeType: Gen[PaymentTransactionFeeType] = Gen.oneOf(PaymentTransactionFeeType.values)
  val genKitchenType: Gen[KitchenType] = Gen.oneOf(KitchenType.values)
  val genRewardRedemptionStatus: Gen[RewardRedemptionStatus] = Gen.oneOf(RewardRedemptionStatus.values)
  val genPaymentProcessor: Gen[PaymentProcessor] = Gen.oneOf(PaymentProcessor.values)
  val genTransactionPaymentProcessor: Gen[TransactionPaymentProcessor] = Gen.oneOf(TransactionPaymentProcessor.values)

  val genOrderRoutingStatuses: Gen[OrderRoutingStatusesByType] =
    OrderRoutingStatusesByType(KitchenType.Bar -> None, KitchenType.Kitchen -> None)

  val genPasswordResetTokenKey: Gen[String] = Gen.listOfN(64, Gen.alphaNumChar).map(_.mkString)

  val genOrderUpsertion: Gen[OrderUpsertion] = for {
    `type` <- genOrderType
    paymentType <- genOrderPaymentType
    paymentStatus <- genPaymentStatus
    totalAmount <- genBigDecimal
    subtotalAmount <- genBigDecimal
    discountAmount <- genBigDecimal
    taxAmount <- genBigDecimal
    tipAmount <- genBigDecimal
    ticketDiscountAmount <- genBigDecimal
    deliveryFeeAmount <- genBigDecimal
    status <- genOrderStatus
  } yield randomOrderUpsertion().copy(
    `type` = `type`,
    paymentType = Some(paymentType),
    paymentStatus = paymentStatus,
    totalAmount = totalAmount,
    subtotalAmount = subtotalAmount,
    discountAmount = Some(discountAmount),
    taxAmount = taxAmount,
    tipAmount = Some(tipAmount),
    ticketDiscountAmount = Some(ticketDiscountAmount),
    deliveryFeeAmount = Some(deliveryFeeAmount),
    status = status,
    receivedAt = UtcTime.now,
    completedAt = Some(UtcTime.now),
  )

  val genPaymentTransactionUpsertion: Gen[PaymentTransactionUpsertion] = for {
    `type` <- genTransactionType
    paymentType <- genTransactionPaymentType
    paymentProcessor <- genTransactionPaymentProcessor
  } yield PaymentTransactionUpsertion(
    id = UUID.randomUUID,
    `type` = Some(`type`),
    paymentType = Some(paymentType),
    refundedPaymentTransactionId = None,
    paymentDetails = None,
    paidAt = None,
    orderItemIds = Seq.empty,
    fees = Seq.empty,
    paymentProcessorV2 = Some(paymentProcessor),
  )

  def randomModelOrderUpdate() =
    model.OrderUpdate(
      id = None,
      merchantId = UUID.randomUUID.some,
      locationId = UUID.randomUUID.some,
      deviceId = None,
      userId = None,
      customerId = None,
      deliveryAddressId = None,
      onlineOrderAttributeId = None,
      tag = None,
      source = genSource.instance.some,
      `type` = genOrderType.instance.some,
      paymentType = genOrderPaymentType.instance.some,
      totalAmount = genBigDecimal.instance.some,
      subtotalAmount = genBigDecimal.instance.some,
      discountAmount = None,
      taxAmount = genBigDecimal.instance.some,
      tipAmount = None,
      ticketDiscountAmount = None,
      deliveryFeeAmount = None,
      customerNotes = Seq.empty,
      merchantNotes = Seq.empty,
      paymentStatus = genPaymentStatus.instance.some,
      status = genOrderStatus.instance.some,
      fulfillmentStatus = None,
      statusTransitions = None,
      isInvoice = None,
      isFiscal = None,
      version = 1.some,
      seating = None,
      deliveryProvider = None,
      deliveryProviderId = None,
      deliveryProviderNumber = None,
      receivedAt = None,
      receivedAtTz = None,
      completedAt = None,
      completedAtTz = None,
    )

  def randomOrderUpsertion() =
    OrderUpsertion(
      creatorUserId = None,
      customerId = None,
      locationId = UUID.randomUUID,
      items = Seq.empty,
      merchantNotes = Seq.empty,
      paymentTransactions = Seq.empty,
      assignedUserIds = None,
      taxRates = Seq.empty,
      discounts = Seq.empty,
      rewards = Seq.empty,
      deliveryAddress = None,
      onlineOrderAttribute = None,
      source = Some(Source.Register),
      bundles = Seq.empty,
      deviceId = None,
      tag = None,
      `type` = genOrderType.instance,
      paymentType = None,
      paymentStatus = genPaymentStatus.instance,
      totalAmount = genBigDecimal.instance,
      subtotalAmount = genBigDecimal.instance,
      discountAmount = None,
      taxAmount = genBigDecimal.instance,
      tipAmount = None,
      ticketDiscountAmount = None,
      deliveryFeeAmount = None,
      fulfillmentStatus = None,
      isInvoice = false,
      isFiscal = None,
      status = genOrderStatus.instance,
      receivedAt = UtcTime.now,
      seating = None,
      deliveryProvider = None,
      deliveryProviderId = None,
      deliveryProviderNumber = None,
      completedAt = Some(UtcTime.now),
    )

  def randomOrderItemUpsertion() =
    OrderItemUpsertion(
      id = UUID.randomUUID,
      quantity = None,
      productId = None,
      productName = None,
      productDescription = None,
      productType = None,
      unit = None,
      paymentStatus = Some(genPaymentStatus.instance),
      priceAmount = Some(genBigDecimal.instance),
      costAmount = Some(genBigDecimal.instance),
      discountAmount = Some(genBigDecimal.instance),
      taxAmount = Some(genBigDecimal.instance),
      basePriceAmount = Some(genBigDecimal.instance),
      calculatedPriceAmount = Some(genBigDecimal.instance),
      totalPriceAmount = Some(genBigDecimal.instance),
      notes = None,
      discounts = Seq.empty,
      modifierOptions = Seq.empty,
      taxRates = Seq.empty,
      variantOptions = Seq.empty,
      giftCardPassRecipientEmail = genEmail.sample,
    )

  def randomOnlineOrderAttributeUpsertion() =
    OnlineOrderAttributeUpsertion(
      id = UUID.randomUUID(),
      email = Some(randomEmail),
      firstName = Some(genWord.instance),
      lastName = Some(genWord.instance),
      phoneNumber = Some(randomNumericString),
      prepareByTime = Gen.option(genLocalTime).instance,
      prepareByDateTime = None,
      estimatedPrepTimeInMins = None,
      acceptanceStatus = Some(genAcceptanceStatus.instance),
      cancellationStatus = None,
      cancellationReason = None,
    )

  def randomAuth0Registration() =
    Auth0Registration(
      token = genWord.instance,
      businessType = BusinessType.Restaurant,
      businessName = genWord.instance,
      address = genAddressUpsertion.instance,
      restaurantType = RestaurantType.CasualDining,
      currency = genCurrency.instance,
      zoneId = ZoneId.of("UTC"),
      pin = None,
      mode = MerchantMode.Production,
      setupType = SetupType.Paytouch,
      dummyData = false,
    )
}
