package io.paytouch.core.utils

import java.time._
import java.util.{ Currency, UUID }

import scala.concurrent._

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import org.scalacheck.Gen

import io.paytouch._
import io.paytouch.core._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.Weekdays._
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.data.model.ExportUpdate
import io.paytouch.core.services.GiftCardPassService
import io.paytouch.core.validators._
import io.paytouch.utils._

object FixtureDaoFactory
    extends FutureHelpers
       with ConfiguredTestDatabase
       with EncryptionSupport
       with Generators
       with LazyLogging
       with JwtTokenGenerator { fixtures =>
  lazy val jwtSecret: String = ServiceConfigurations.JwtSecret
  lazy val bcryptRounds: Int = ServiceConfigurations.bcryptRounds

  lazy val daos = new Daos
  lazy val sessionDao = daos.sessionDao

  final case class Wrapper[E <: SlickRecord, U <: SlickUpdate[E]](
      update: U,
      dao: SlickDao {
        type Record = E; type Update = U
      },
      callback: E => Unit = (e: E) => {},
    ) {
    def create: E =
      doCreate(update)

    def createForceOverride(transform: U => U): E =
      doCreate(transform(update))

    private[this] def doCreate(u: U): E = {
      val (_, record) = dao.upsert(u).await
      callback(record)
      record
    }

    def createAndReload: E = {
      val result = create
      dao.findById(result.id).await.get
    }

    // alias to avoid semantic confusion between `val update` and `def create`
    def get = update

    def map[T](f: this.type => T) = f(this)

    def wrapperOnCreated[T <: SlickRecord, Z <: SlickUpdate[T]](f: E => Wrapper[T, Z]) = f(create)
  }

  def admin(
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      password: Option[String] = None,
      email: Option[String] = None,
    ) = {
    val id = UUID.randomUUID
    val encryptedPassword = bcryptEncrypt(password.getOrElse("aPassword"))
    val admin = AdminUpdate(
      id = Some(id),
      firstName = firstName.orElse(Some(s"$id-firstName")),
      lastName = lastName.orElse(Some(s"$id-lastName")),
      password = Some(encryptedPassword),
      email = email.orElse(Some(s"$id@paytouch.io")),
      lastLoginAt = None,
    )
    Wrapper(admin, daos.adminDao)
  }

  def brand(merchant: MerchantRecord, name: Option[String] = None) = {
    val id = UUID.randomUUID
    val brand = BrandUpdate(id = Some(id), merchantId = Some(merchant.id), name = name.orElse(Some(s"Brand $id")))

    Wrapper(brand, daos.brandDao)
  }

  def kitchenBar(location: LocationRecord) = kitchen(location, name = Some("Bar"), `type` = Some(KitchenType.Bar))
  def kitchenKitchen(location: LocationRecord) =
    kitchen(location, name = Some("Kitchen"), `type` = Some(KitchenType.Kitchen))

  def kitchen(
      location: LocationRecord,
      name: Option[String] = None,
      overrideNow: Option[ZonedDateTime] = None,
      deletedAt: Option[ZonedDateTime] = None,
      `type`: Option[KitchenType] = None,
      kdsEnabled: Option[Boolean] = None,
    ) = {
    val id = UUID.randomUUID
    val kitchen = new KitchenUpdate(
      id = Some(id),
      merchantId = Some(location.merchantId),
      name = name.orElse(Some(s"Kitchen $id")),
      locationId = Some(location.id),
      `type` = `type`.orElse(Some(genKitchenType.instance)),
      active = Some(true),
      kdsEnabled = kdsEnabled.orElse(Some(true)),
      deletedAt = deletedAt,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(kitchen, daos.kitchenDao)
  }

  def cashDrawer(
      user: UserRecord,
      location: LocationRecord,
      endedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
      status: Option[CashDrawerStatus] = None,
    ) = {
    val cashDrawer = new CashDrawerUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      userId = Some(user.id),
      employeeId = None,
      name = "Cash Drawer",
      startingCashAmount = Some(genBigDecimal.instance),
      endingCashAmount = genBigDecimal.instance,
      cashSalesAmount = genBigDecimal.instance,
      cashRefundsAmount = genBigDecimal.instance,
      paidInAndOutAmount = genBigDecimal.instance,
      paidInAmount = genBigDecimal.instance,
      paidOutAmount = genBigDecimal.instance,
      manualPaidInAmount = genBigDecimal.instance,
      manualPaidOutAmount = genBigDecimal.instance,
      tippedInAmount = genBigDecimal.instance,
      tippedOutAmount = genBigDecimal.instance,
      expectedAmount = genBigDecimal.instance,
      status = status.orElse(Some(genCashDrawerStatus.instance)),
      startedAt = Some(genZonedDateTime.instance),
      endedAt = endedAt,
      exportFilename = None,
      printerMacAddress = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(cashDrawer, daos.cashDrawerDao)
  }

  def cashDrawerActivity(cashDrawer: CashDrawerRecord, overrideNow: Option[ZonedDateTime] = None) = {
    val cashDrawerActivity = new CashDrawerActivityUpdate(
      id = None,
      merchantId = Some(cashDrawer.merchantId),
      userId = cashDrawer.userId,
      orderId = UUID.randomUUID.some,
      cashDrawerId = Some(cashDrawer.id),
      `type` = Some(genCashDrawerActivityType.instance),
      startingCashAmount = Some(genBigDecimal.instance),
      endingCashAmount = Some(genBigDecimal.instance),
      payInAmount = Some(genBigDecimal.instance),
      payOutAmount = Some(genBigDecimal.instance),
      tipInAmount = Some(genBigDecimal.instance),
      tipOutAmount = Some(genBigDecimal.instance),
      currentBalanceAmount = Some(genBigDecimal.instance),
      tipForUserId = None,
      timestamp = Some(genZonedDateTime.instance),
      notes = randomWord,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(cashDrawerActivity, daos.cashDrawerActivityDao)
  }

  def catalog(
      merchant: MerchantRecord,
      name: Option[String] = None,
      `type`: Option[CatalogType] = None,
    ) = {
    val id = UUID.randomUUID
    val catalog =
      CatalogUpdate(
        id = id.some,
        merchantId = merchant.id.some,
        name = name.orElse(s"Catalog $id".some),
        `type` = `type`.orElse(CatalogType.Menu.some),
      )

    Wrapper(catalog, daos.catalogDao)
  }

  def defaultMenuCatalog(merchant: MerchantRecord) =
    catalog(merchant, "Default Menu Test".some, `type` = CatalogType.DefaultMenu.some)

  def systemCategory(
      catalog: CatalogRecord,
      name: Option[String] = None,
      description: Option[String] = None,
      parentCategory: Option[CategoryRecord] = None,
      position: Option[Int] = None,
      id: Option[UUID] = None,
      active: Option[Boolean] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    require(catalog.`type` == CatalogType.DefaultMenu)
    category(
      catalog.merchantId,
      catalog = catalog.some,
      name,
      description,
      parentCategory,
      position,
      id,
      active,
      locations,
      overrideNow,
    )
  }

  def catalogAvailability(catalog: CatalogRecord, days: Seq[Day]) = {
    val availability = AvailabilityUpdate(
      id = None,
      merchantId = Some(catalog.merchantId),
      itemId = Some(catalog.id),
      itemType = Some(AvailabilityItemType.Catalog),
      sunday = Some(days.contains(Sunday)),
      monday = Some(days.contains(Monday)),
      tuesday = Some(days.contains(Tuesday)),
      wednesday = Some(days.contains(Wednesday)),
      thursday = Some(days.contains(Thursday)),
      friday = Some(days.contains(Friday)),
      saturday = Some(days.contains(Saturday)),
      start = Some(LocalTime.of(12, 34, 56)),
      end = Some(LocalTime.of(23, 59, 48)),
    )

    Wrapper(availability, daos.catalogAvailabilityDao)
  }

  def catalogCategory(
      catalog: CatalogRecord,
      name: Option[String] = None,
      description: Option[String] = None,
      parentCategory: Option[CategoryRecord] = None,
      position: Option[Int] = None,
      id: Option[UUID] = None,
      active: Option[Boolean] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      overrideNow: Option[ZonedDateTime] = None,
    ) =
    category(
      catalog.merchantId,
      catalog = catalog.some,
      name,
      description,
      parentCategory,
      position,
      id,
      active,
      locations,
      overrideNow,
    )

  private def category(
      merchantId: UUID,
      catalog: Option[CatalogRecord] = None,
      name: Option[String] = None,
      description: Option[String] = None,
      parentCategory: Option[CategoryRecord] = None,
      position: Option[Int] = None,
      id: Option[UUID] = None,
      active: Option[Boolean] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val category = new CategoryUpdate(
      id = id,
      merchantId = Some(merchantId),
      catalogId = catalog.map(_.id),
      parentCategoryId = parentCategory.map(_.id),
      name = name.orElse(Some(s"Category $id")),
      description = description.getOrElse[String](s"Category desc $id"),
      avatarBgColor = None,
      position = position.orElse(Gen.option(genInt).instance),
      active = active,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(category, daos.categoryDao, { record: CategoryRecord => locations.map(categoryLocation(record, _).create) })
  }

  def categoryLocation(
      category: CategoryRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
    ) = {
    val categoryLocation = CategoryLocationUpdate(
      id = None,
      merchantId = Some(category.merchantId),
      categoryId = Some(category.id),
      locationId = Some(location.id),
      active = active,
    )

    Wrapper(categoryLocation, daos.categoryLocationDao)
  }

  def categoryLocationAvailability(categoryLocation: CategoryLocationRecord, days: Seq[Day]) = {
    val availability = AvailabilityUpdate(
      id = None,
      merchantId = Some(categoryLocation.merchantId),
      itemId = Some(categoryLocation.id),
      itemType = Some(AvailabilityItemType.CategoryLocation),
      sunday = Some(days.contains(Sunday)),
      monday = Some(days.contains(Monday)),
      tuesday = Some(days.contains(Tuesday)),
      wednesday = Some(days.contains(Wednesday)),
      thursday = Some(days.contains(Thursday)),
      friday = Some(days.contains(Friday)),
      saturday = Some(days.contains(Saturday)),
      start = Some(LocalTime.of(12, 34, 56)),
      end = Some(LocalTime.of(23, 59, 48)),
    )

    Wrapper(availability, daos.categoryLocationAvailabilityDao)
  }

  def categoryAvailability(catalogCategory: CategoryRecord, days: Seq[Day]) = {
    val availability = AvailabilityUpdate(
      id = None,
      merchantId = Some(catalogCategory.merchantId),
      itemId = Some(catalogCategory.id),
      itemType = Some(AvailabilityItemType.Category),
      sunday = Some(days.contains(Sunday)),
      monday = Some(days.contains(Monday)),
      tuesday = Some(days.contains(Tuesday)),
      wednesday = Some(days.contains(Wednesday)),
      thursday = Some(days.contains(Thursday)),
      friday = Some(days.contains(Friday)),
      saturday = Some(days.contains(Saturday)),
      start = Some(LocalTime.of(12, 34, 56)),
      end = Some(LocalTime.of(23, 59, 48)),
    )

    Wrapper(availability, daos.categoryAvailabilityDao)
  }

  def comment(user: UserRecord, `object`: SlickRecord) = {
    val id = UUID.randomUUID
    val objectType = `object` match {
      case _: InventoryCountRecord => CommentType.InventoryCount
      case _: PurchaseOrderRecord  => CommentType.PurchaseOrder
      case _: ReceivingOrderRecord => CommentType.ReceivingOrder
      case _: ReturnOrderRecord    => CommentType.ReturnOrder
      case _                       => throw new RuntimeException("Unsupported `object` type")
    }
    val comment = CommentUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      objectType = Some(objectType),
      objectId = Some(`object`.id),
      userId = Some(user.id),
      body = Some(randomWords),
    )

    Wrapper(comment, daos.commentDao)
  }

  def customerGroup(
      customer: GlobalCustomerRecord,
      group: GroupRecord,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val customerGroup = new CustomerGroupUpdate(
      id = None,
      merchantId = Some(group.merchantId),
      customerId = Some(customer.id),
      groupId = Some(group.id),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(customerGroup, daos.customerGroupDao)
  }

  def customerLocation(
      customer: GlobalCustomerRecord,
      location: LocationRecord,
      totalVisits: Option[Int] = None,
      totalSpend: Option[BigDecimal] = None,
    ) = {
    val customerLocation = CustomerLocationUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      customerId = Some(customer.id),
      locationId = Some(location.id),
      totalVisits = totalVisits.orElse(Gen.option(genInt).instance),
      totalSpendAmount = totalSpend.orElse(Gen.option(genBigDecimal).instance),
    )

    Wrapper(customerLocation, daos.customerLocationDao)
  }

  def customerMerchant(
      merchant: MerchantRecord,
      customer: GlobalCustomerRecord,
      source: Option[CustomerSource] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val customerMerchant = new CustomerMerchantUpdate(
      _id = None,
      merchantId = Some(merchant.id),
      customerId = Some(customer.id),
      firstName = customer.firstName,
      lastName = customer.lastName,
      dob = customer.dob,
      anniversary = customer.anniversary,
      email = customer.email,
      phoneNumber = customer.phoneNumber,
      addressLine1 = customer.addressLine1,
      addressLine2 = customer.addressLine2,
      city = customer.city,
      state = customer.state,
      country = customer.country,
      stateCode = customer.stateCode,
      countryCode = customer.countryCode,
      postalCode = customer.postalCode,
      billingDetails = None,
      source = source.orElse(Some(CustomerSource.PtRegister)),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(customerMerchant, daos.customerMerchantDao)
  }

  def loyaltyMembership(
      customer: GlobalCustomerRecord,
      loyaltyProgram: LoyaltyProgramRecord,
      points: Option[Int] = None,
      iosPassPublicUrl: Option[String] = None,
      androidPassPublicUrl: Option[String] = None,
      customerOptInAt: Option[ZonedDateTime] = None,
      merchantOptInAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val loyaltyMembership = new LoyaltyMembershipUpdate(
      id = None,
      merchantId = Some(loyaltyProgram.merchantId),
      customerId = Some(customer.id),
      loyaltyProgramId = Some(loyaltyProgram.id),
      lookupId = Some(genLookupId.instance),
      iosPassPublicUrl = iosPassPublicUrl,
      androidPassPublicUrl = androidPassPublicUrl,
      points = points,
      customerOptInAt = customerOptInAt,
      merchantOptInAt = merchantOptInAt,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(loyaltyMembership, daos.loyaltyMembershipDao)
  }

  def loyaltyPointsHistory(
      loyaltyMembership: LoyaltyMembershipRecord,
      points: Int,
      `type`: LoyaltyPointsHistoryType,
      order: Option[OrderRecord] = None,
      objectId: Option[UUID] = None,
    ) = {
    val loyaltyPointsHistory = new LoyaltyPointsHistoryUpdate(
      id = None,
      merchantId = Some(loyaltyMembership.merchantId),
      loyaltyMembershipId = Some(loyaltyMembership.id),
      points = Some(points),
      `type` = Some(`type`),
      orderId = order.map(_.id),
      objectId = objectId,
      objectType = `type`.relatedType,
    )

    Wrapper(loyaltyPointsHistory, daos.loyaltyPointsHistoryDao)
  }

  def rewardRedemption(
      loyaltyMembership: LoyaltyMembershipRecord,
      loyaltyReward: LoyaltyRewardRecord,
      overrideLoyaltyRewardType: Option[RewardType] = None,
      orderId: Option[UUID] = None,
      points: Option[Int] = None,
      status: Option[RewardRedemptionStatus] = Some(RewardRedemptionStatus.Reserved),
      objectId: Option[UUID] = None,
      objectType: Option[RewardRedemptionType] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val rewardRedemption = new RewardRedemptionUpdate(
      id = None,
      merchantId = Some(loyaltyReward.merchantId),
      loyaltyRewardId = Some(loyaltyReward.id),
      loyaltyRewardType = overrideLoyaltyRewardType.orElse(Some(loyaltyReward.`type`)),
      loyaltyMembershipId = Some(loyaltyMembership.id),
      points = points.orElse(Some(genInt.instance)),
      status = status,
      orderId = orderId,
      objectId = objectId,
      objectType = objectType,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(rewardRedemption, daos.rewardRedemptionDao)
  }

  def discount(
      merchant: MerchantRecord,
      title: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      amount: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val discountType = genDiscountType.instance
    val discount = new DiscountUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      title = title.orElse(Some(s"Discount $id")),
      `type` = Some(discountType),
      amount = amount.orElse(Some(32.12)),
      requireManagerApproval = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(discount, daos.discountDao, { record: DiscountRecord => locations.map(discountLocation(record, _).create) })
  }

  def discountLocation(
      discount: DiscountRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
    ) = {
    val discountLocation = DiscountLocationUpdate(
      id = None,
      merchantId = Some(discount.merchantId),
      discountId = Some(discount.id),
      locationId = Some(location.id),
      active = active,
    )

    Wrapper(discountLocation, daos.discountLocationDao)
  }

  def discountAvailability(discount: DiscountRecord, days: Seq[Day]) = {
    val availability = AvailabilityUpdate(
      id = None,
      merchantId = Some(discount.merchantId),
      itemId = Some(discount.id),
      itemType = Some(AvailabilityItemType.Discount),
      sunday = Some(days.contains(Sunday)),
      monday = Some(days.contains(Monday)),
      tuesday = Some(days.contains(Tuesday)),
      wednesday = Some(days.contains(Wednesday)),
      thursday = Some(days.contains(Thursday)),
      friday = Some(days.contains(Friday)),
      saturday = Some(days.contains(Saturday)),
      start = Some(LocalTime.of(12, 34, 56)),
      end = Some(LocalTime.of(23, 59, 48)),
    )

    Wrapper(availability, daos.categoryLocationAvailabilityDao)
  }

  def event(
      merchant: MerchantRecord,
      action: TrackableAction,
      `object`: ExposedName,
      receivedAt: ZonedDateTime = UtcTime.now,
    ): Future[EventRecord] = {
    val event = EventRecord(
      id = UUID.randomUUID,
      merchantId = merchant.id,
      action = action,
      `object` = `object`,
      data = None,
      receivedAt = receivedAt,
    )
    daos.eventDao.insert(event)
  }

  def timeCard(
      user: UserRecord,
      location: LocationRecord,
      totalMins: Int = 154,
      shift: Option[ShiftRecord] = None,
      startAt: Option[ZonedDateTime] = None,
      endAt: Option[ZonedDateTime] = Some(UtcTime.now),
    ) = {
    val now = UtcTime.now
    val resultsInMins = entities.TimeCard.calculateResultsInMins(Some(totalMins), shift)
    val timeCard = TimeCardUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      locationId = Some(location.id),
      shiftId = shift.map(_.id),
      deltaMins = resultsInMins.map(_.deltaMins),
      totalMins = Some(totalMins),
      regularMins = resultsInMins.map(_.regularMins),
      overtimeMins = resultsInMins.map(_.overtimeMins),
      unpaidBreakMins = Gen.option(genInt).instance,
      notes = None,
      startAt = startAt.orElse(Some(now.minusMinutes(totalMins))),
      endAt = endAt,
    )

    Wrapper(timeCard, daos.timeCardDao)
  }

  def timeOffCard(
      user: UserRecord,
      startAt: Option[ZonedDateTime] = None,
      endAt: Option[ZonedDateTime] = Some(UtcTime.now),
    ) = {
    val timeOffCard = TimeOffCardUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      paid = None,
      `type` = None,
      notes = None,
      startAt = startAt,
      endAt = endAt,
    )

    Wrapper(timeOffCard, daos.timeOffCardDao)
  }

  def locationAvailability(location: LocationRecord, days: Seq[Day]) = {
    val availability = AvailabilityUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      itemId = Some(location.id),
      itemType = Some(AvailabilityItemType.Location),
      sunday = Some(days.contains(Sunday)),
      monday = Some(days.contains(Monday)),
      tuesday = Some(days.contains(Tuesday)),
      wednesday = Some(days.contains(Wednesday)),
      thursday = Some(days.contains(Thursday)),
      friday = Some(days.contains(Friday)),
      saturday = Some(days.contains(Saturday)),
      start = Some(LocalTime.of(12, 34, 56)),
      end = Some(LocalTime.of(23, 59, 48)),
    )

    Wrapper(availability, daos.locationAvailabilityDao)
  }

  def globalCustomer(
      merchant: Option[MerchantRecord] = None,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    globalCustomerWithEmail(
      merchant,
      firstName,
      lastName,
      email = Some(s"$randomWord-$id@$randomWord.$randomWord"),
      overrideNow = overrideNow,
    )
  }

  def globalCustomerWithEmail(
      merchant: Option[MerchantRecord] = None,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      email: Option[String],
      dob: Option[LocalDate] = None,
      anniversary: Option[LocalDate] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val globalCustomer = new GlobalCustomerUpdate(
      id = Some(id),
      firstName = firstName,
      lastName = lastName,
      dob = dob,
      anniversary = anniversary,
      email = email,
      phoneNumber = Some(s"+391231231234"),
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      stateCode = None,
      countryCode = None,
      country = None,
      postalCode = None,
      mobileStorefrontLastLogin = None,
      webStorefrontLastLogin = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(
      globalCustomer,
      daos.globalCustomerDao,
      { globalCustomer: GlobalCustomerRecord =>
        merchant.foreach(customerMerchant(_, globalCustomer, overrideNow = overrideNow).create)
      },
    )
  }

  def group(merchant: MerchantRecord, name: Option[String] = None) = {
    val id = UUID.randomUUID
    val group = GroupUpdate(id = Some(id), merchantId = Some(merchant.id), name = name.orElse(Some(s"Group $id")))

    Wrapper(group, daos.groupDao)
  }

  def imageUpload(
      merchant: MerchantRecord,
      objectId: Option[UUID] = None,
      urls: Option[Map[String, String]] = Some(Map("medium" -> "http://cdn/image.jpg")),
      imageUploadType: Option[ImageUploadType] = None,
    ) = {
    val imageUpload = ImageUploadUpdate(
      id = None,
      merchantId = Some(merchant.id),
      urls = urls,
      fileName = Some("my-file.jpg"),
      objectId = objectId,
      objectType = imageUploadType.orElse(Some(ImageUploadType.Product)),
    )
    Wrapper(imageUpload, daos.imageUploadDao)
  }

  def `import`(location: LocationRecord, filename: Option[String] = None) = {
    val `import` = ImportUpdate(
      id = None,
      `type` = Some(ImportType.Product),
      filename = filename.orElse(Some("my/filename")),
      merchantId = Some(location.merchantId),
      locationIds = Some(Seq(location.id)),
    )
    Wrapper(`import`, daos.importDao)
  }

  def ticket(
      order: OrderRecord,
      status: Option[TicketStatus] = None,
      show: Option[Boolean] = None,
      routeToKitchenId: UUID,
    ) = {
    assert(order.locationId.isDefined, "Cannot create an ticket for an order without a location id")
    val ticketStatus = status.getOrElse(genTicketStatus.instance)

    val ticket = TicketUpdate(
      id = None,
      merchantId = Some(order.merchantId),
      locationId = order.locationId,
      orderId = Some(order.id),
      status = Some(ticketStatus),
      show = show,
      routeToKitchenId = Some(routeToKitchenId),
      startedAt = if (ticketStatus.isNewOrInProgress) Some(UtcTime.now.minusMinutes(5)) else None,
      completedAt = if (ticketStatus.isCompleted) Some(UtcTime.now) else None,
    )

    Wrapper(ticket, daos.ticketDao)
  }

  def ticketOrderItem(ticket: TicketRecord, orderItem: OrderItemRecord) = {
    val ticketOrderItem = TicketOrderItemUpdate(
      id = None,
      merchantId = Some(orderItem.merchantId),
      ticketId = Some(ticket.id),
      orderItemId = Some(orderItem.id),
    )

    Wrapper(ticketOrderItem, daos.ticketOrderItemDao)
  }

  def location(
      merchant: MerchantRecord,
      name: Option[String] = None,
      zoneId: Option[String] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
      dummyData: Option[Boolean] = None,
    ) = {
    val id = UUID.randomUUID

    val location = new LocationUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      name = name.orElse(Some(s"Location $id")),
      email = Some(s"$id@locations.email"),
      phoneNumber = s"$id",
      website = s"https://www.paytouch.io/$id",
      addressLine1 = s"address line 1 $id",
      addressLine2 = None,
      city = "Rome",
      state = "Lazio".some,
      country = "Italy".some,
      stateCode = "IT-RM".some, // the concept of state codes does not map nicely to Italy
      countryCode = "IT".some,
      postalCode = "00100",
      timezone = Some(ZoneId.of(zoneId.getOrElse("Europe/Rome"))),
      active = None,
      dummyData = dummyData,
      latitude = None,
      longitude = None,
      deletedAt = deletedAt,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(location, daos.locationDao)
  }

  def loyaltyProgram(
      merchant: MerchantRecord,
      locations: Seq[LocationRecord] = Seq.empty,
      points: Option[Int] = None,
      `type`: Option[LoyaltyProgramType] = None,
      active: Option[Boolean] = None,
      appleWalletTemplateId: Option[String] = None,
      androidPayTemplateId: Option[String] = None,
      overrideNow: Option[ZonedDateTime] = None,
      pointsToReward: Option[Int] = None,
      minimumPurchaseAmount: Option[BigDecimal] = None,
      spendAmountForPoints: Option[BigDecimal] = None,
      signupRewardEnabled: Option[Boolean] = None,
      signupRewardPoints: Option[Int] = None,
    ) = {
    val id = UUID.randomUUID
    val loyaltyProgram = new LoyaltyProgramUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      `type` = `type`.orElse(Some(genLoyaltyProgramType.instance)),
      points = points.orElse(Some(genInt.instance)),
      spendAmountForPoints = spendAmountForPoints,
      pointsToReward = pointsToReward.orElse(Some(100)),
      minimumPurchaseAmount = minimumPurchaseAmount,
      signupRewardEnabled = signupRewardEnabled,
      signupRewardPoints = signupRewardPoints,
      active = active.orElse(Some(true)),
      appleWalletTemplateId = appleWalletTemplateId,
      androidPayTemplateId = androidPayTemplateId,
      businessName = Some(merchant.businessName),
      templateDetails = None,
      welcomeEmailSubject = None,
      welcomeEmailColor = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(
      loyaltyProgram,
      daos.loyaltyProgramDao,
      { loyaltyProgram: LoyaltyProgramRecord =>
        locations.foreach(l => loyaltyProgramLocation(loyaltyProgram, l).create)
      },
    )
  }

  def loyaltyProgramLocation(loyaltyProgram: LoyaltyProgramRecord, location: LocationRecord) = {
    val loyaltyProgramLocation = LoyaltyProgramLocationUpdate(
      id = None,
      merchantId = Some(loyaltyProgram.merchantId),
      loyaltyProgramId = Some(loyaltyProgram.id),
      locationId = Some(location.id),
    )

    Wrapper(loyaltyProgramLocation, daos.loyaltyProgramLocationDao)
  }

  def loyaltyRewardProduct(loyaltyReward: LoyaltyRewardRecord, product: ArticleRecord) = {
    val loyaltyProgramProduct = LoyaltyRewardProductUpdate(
      id = None,
      merchantId = Some(loyaltyReward.merchantId),
      loyaltyRewardId = Some(loyaltyReward.id),
      productId = Some(product.id),
    )

    Wrapper(loyaltyProgramProduct, daos.loyaltyRewardProductDao)
  }

  def loyaltyReward(
      loyaltyProgram: LoyaltyProgramRecord,
      rewardType: Option[RewardType] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val loyaltyReward = new LoyaltyRewardUpdate(
      id = Some(id),
      merchantId = Some(loyaltyProgram.merchantId),
      loyaltyProgramId = Some(loyaltyProgram.id),
      `type` = rewardType.orElse(Some(genRewardType.instance)),
      amount = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(loyaltyReward, daos.loyaltyRewardDao)
  }

  def merchant: Wrapper[MerchantRecord, MerchantUpdate] = merchant()

  def merchant(
      name: Option[String] = Some("My business"),
      currency: Option[Currency] = Some(USD),
      mode: Option[MerchantMode] = None,
      switchMerchantId: Option[UUID] = None,
      setupSteps: Option[Map[MerchantSetupSteps, entities.MerchantSetupStep]] = None,
      businessType: Option[BusinessType] = None,
      restaurantType: Option[RestaurantType] = None,
      paymentProcessor: Option[PaymentProcessor] = None,
      paymentProcessorConfig: Option[PaymentProcessorConfig] = None,
      legalDetails: Option[entities.LegalDetailsUpsertion] = None,
    ) =
    Wrapper(
      dao = daos.merchantDao,
      update = MerchantUpdate(
        id = None,
        active = None,
        businessType = businessType.getOrElse(BusinessType.Restaurant).some,
        businessName = name,
        restaurantType = restaurantType.getOrElse(RestaurantType.Default).some,
        paymentProcessor = paymentProcessor.getOrElse(PaymentProcessor.Worldpay).some,
        paymentProcessorConfig = paymentProcessorConfig.getOrElse(ServiceConfigurations.worldpay).some,
        currency = currency,
        mode = mode.getOrElse(MerchantMode.Production).some,
        switchMerchantId = switchMerchantId,
        defaultZoneId = genZoneId.instance.some,
        setupSteps = setupSteps,
        setupCompleted = None,
        features = None,
        legalDetails = legalDetails,
        setupType = SetupType.Paytouch.some,
      ),
    )

  def modifierOption(
      modifierSet: ModifierSetRecord,
      name: Option[String] = None,
      position: Option[Int] = None,
    ) = {
    val id = UUID.randomUUID
    val modifierOption = ModifierOptionUpdate(
      id = Some(id),
      merchantId = Some(modifierSet.merchantId),
      modifierSetId = Some(modifierSet.id),
      name = name.orElse(Some(s"Modifier option $id")),
      priceAmount = Some(10),
      position = position.orElse(Gen.option(genInt).instance),
      active = None,
    )

    Wrapper(modifierOption, daos.modifierOptionDao)
  }

  def modifierSet(
      merchant: MerchantRecord,
      name: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val modifierSet = new ModifierSetUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      `type` = Some(ModifierSetType.Hold),
      name = name.orElse(Some(s"Modifier set $id")),
      optionCount = None,
      maximumSingleOptionCount = None,
      hideOnReceipts = Some(genBoolean.instance),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(
      modifierSet,
      daos.modifierSetDao,
      { record: ModifierSetRecord =>
        locations.foreach(l => modifierSetLocation(record, l).create)
      },
    )
  }

  def modifierSetLocation(
      modifierSet: ModifierSetRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
    ) = {
    val modifierSetLocation = ModifierSetLocationUpdate(
      id = None,
      merchantId = Some(modifierSet.merchantId),
      modifierSetId = Some(modifierSet.id),
      locationId = Some(location.id),
      active = active,
    )

    Wrapper(modifierSetLocation, daos.modifierSetLocationDao)
  }

  def modifierSetProduct(
      modifierSet: ModifierSetRecord,
      product: ArticleRecord,
      position: Option[Int] = None,
    ) = {
    assert(product.`type`.isMain, "Modifier sets can only be associated with main products")
    val modifierSetProduct = ModifierSetProductUpdate(
      id = None,
      merchantId = Some(modifierSet.merchantId),
      modifierSetId = Some(modifierSet.id),
      productId = Some(product.id),
      position = position,
    )

    Wrapper(modifierSetProduct, daos.modifierSetProductDao)
  }

  def order(
      merchant: MerchantRecord,
      location: Option[LocationRecord] = None,
      customer: Option[CustomerMerchantRecord] = None,
      seating: Option[entities.Seating] = None,
      globalCustomer: Option[GlobalCustomerRecord] = None,
      user: Option[UserRecord] = None,
      orderDeliveryAddress: Option[OrderDeliveryAddressRecord] = None,
      onlineOrderAttribute: Option[OnlineOrderAttributeRecord] = None,
      receivedAt: Option[ZonedDateTime] = None,
      completedAt: Option[ZonedDateTime] = None,
      paymentType: Option[OrderPaymentType] = None,
      paymentStatus: Option[PaymentStatus] = None,
      status: Option[OrderStatus] = None,
      isInvoice: Option[Boolean] = None,
      isFiscal: Option[Boolean] = None,
      `type`: Option[OrderType] = None,
      statusTransitions: Option[Seq[StatusTransition]] = None,
      source: Option[Source] = None,
      taxAmount: Option[BigDecimal] = None,
      tipAmount: Option[BigDecimal] = None,
      subtotalAmount: Option[BigDecimal] = None,
      discountAmount: Option[BigDecimal] = None,
      ticketDiscountAmount: Option[BigDecimal] = None,
      deliveryFeeAmount: Option[BigDecimal] = None,
      totalAmount: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
      merchantNotes: Seq[MerchantNote] = Seq.empty,
      deliveryProvider: Option[DeliveryProvider] = None,
    ) = {
    val id = UUID.randomUUID
    val defaultedReceivedAt = receivedAt.orElse(Some(UtcTime.now))

    val deliveryProviderId =
      deliveryProvider.as(UUID.randomUUID.toString)

    val order = new OrderUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      locationId = location.map(_.id),
      deviceId = None,
      userId = user.map(_.id),
      customerId = customer.map(_.id).orElse(globalCustomer.map(_.id)),
      deliveryAddressId = orderDeliveryAddress.map(_.id),
      onlineOrderAttributeId = onlineOrderAttribute.map(_.id),
      tag = Gen.option(genWord).instance,
      source = source.orElse(Some(Source.Storefront)),
      `type` = `type`.orElse(Some(OrderType.InStore)),
      paymentType = paymentType.orElse(Some(OrderPaymentType.Cash)),
      totalAmount = totalAmount.orElse(Some(12.34)),
      subtotalAmount = subtotalAmount.orElse(Some(11.34)),
      discountAmount = discountAmount.orElse(Some(1.00)),
      taxAmount = taxAmount.orElse(Some(2.34)),
      tipAmount = tipAmount.orElse(Some(0.01)),
      ticketDiscountAmount = ticketDiscountAmount.orElse(Gen.option(genBigDecimal).instance),
      deliveryFeeAmount = deliveryFeeAmount.orElse(Gen.option(genBigDecimal).instance),
      customerNotes = Seq(CustomerNote(UUID.randomUUID, "This product is awesome!", UtcTime.now)),
      merchantNotes = merchantNotes,
      paymentStatus = paymentStatus.orElse(Some(PaymentStatus.Paid)),
      status = status.orElse(Some(OrderStatus.Delivered)),
      fulfillmentStatus = Some(FulfillmentStatus.Unfulfilled),
      statusTransitions = statusTransitions,
      isInvoice = isInvoice,
      isFiscal = isFiscal,
      version = Some(2),
      seating = seating,
      deliveryProvider = deliveryProvider,
      deliveryProviderId = deliveryProviderId,
      deliveryProviderNumber = None,
      receivedAt = defaultedReceivedAt,
      receivedAtTz = defaultedReceivedAt.map(_.toLocationTimezoneWithFallback(location.map(_.timezone))),
      completedAt = completedAt,
      completedAtTz = completedAt.map(_.toLocationTimezoneWithFallback(location.map(_.timezone))),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(order, daos.orderDao)
  }

  def orderDeliveryAddress(merchant: MerchantRecord, estimatedDrivingTimeInMins: Option[Int] = None) = {
    val address = genAddress.instance

    val orderDeliveryAddress = OrderDeliveryAddressUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(merchant.id),
      firstName = Some(randomWord),
      lastName = Some(randomWord),
      addressLine1 = address.line1,
      addressLine2 = address.line2,
      city = address.city,
      state = address.state,
      country = address.country,
      stateCode = address.stateData.map(_.code),
      countryCode = address.countryData.map(_.code),
      postalCode = address.postalCode,
      drivingDistanceInMeters = Gen.option(genBigDecimal).instance,
      estimatedDrivingTimeInMins = estimatedDrivingTimeInMins,
    )

    Wrapper(orderDeliveryAddress, daos.orderDeliveryAddressDao)
  }

  def onlineOrderAttribute(merchant: MerchantRecord, acceptanceStatus: Option[AcceptanceStatus] = None) = {
    val status = acceptanceStatus.getOrElse(genAcceptanceStatus.instance)

    val acceptedAt = if (status == AcceptanceStatus.Accepted) Some(UtcTime.now) else None
    val estimatedPrepTimeInMins = if (status == AcceptanceStatus.Accepted) Some(genInt.instance) else None
    val rejectedAt = if (status == AcceptanceStatus.Rejected) Some(UtcTime.now) else None
    val rejectionReason = if (status == AcceptanceStatus.Rejected) Some(randomWords) else None

    val onlineOrderAttribute = OnlineOrderAttributeUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(merchant.id),
      acceptanceStatus = Some(status),
      rejectionReason = rejectionReason,
      prepareByTime = Some(genLocalTime.instance),
      prepareByDateTime = Some(genZonedDateTime.instance),
      estimatedPrepTimeInMins = estimatedPrepTimeInMins,
      acceptedAt = acceptedAt,
      rejectedAt = rejectedAt,
      estimatedReadyAt = None,
      estimatedDeliveredAt = None,
      cancellationStatus = None,
      cancellationReason = None,
    )

    Wrapper(onlineOrderAttribute, daos.onlineOrderAttributeDao)
  }

  def orderDiscount(order: OrderRecord, discount: DiscountRecord) = {
    val orderDiscount = OrderDiscountUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      discountId = Some(discount.id),
      title = Some(randomWord),
      `type` = Some(discount.`type`),
      amount = Some(discount.amount),
      totalAmount = Some(discount.amount),
    )

    Wrapper(orderDiscount, daos.orderDiscountDao)
  }

  def orderWithStatusTransitions(merchant: MerchantRecord, location: LocationRecord) = {
    val statusTransitions = Seq(StatusTransition(status = OrderStatus.Completed))
    order(merchant, location = Some(location), statusTransitions = Some(statusTransitions))
  }

  def orderFeedback(
      order: OrderRecord,
      customer: GlobalCustomerRecord,
      read: Option[Boolean] = None,
      rating: Option[Int] = None,
      receivedAt: Option[ZonedDateTime] = None,
    ) = {
    val orderFeedback = OrderFeedbackUpdate(
      id = None,
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      locationId = order.locationId,
      customerId = Some(customer.id),
      rating = rating.orElse(Gen.chooseNum(0, 5).sample),
      body = Some(s"This is feedback for customer ${customer.id}"),
      read = read,
      receivedAt = receivedAt,
    )
    Wrapper(orderFeedback, daos.orderFeedbackDao)
  }

  def orderBundle(
      order: OrderRecord,
      bundleOrderItem: OrderItemRecord,
      articleOrderItem: OrderItemRecord,
      bundleSet: Option[BundleSetRecord] = None,
      bundleOption: Option[BundleOptionRecord] = None,
    ) = {
    val orderBundleOption = RecoveredOrderBundleOption(
      id = UUID.randomUUID,
      bundleOptionId = bundleOption.map(_.id),
      articleOrderItemId = Some(articleOrderItem.id),
      position = Some(genInt.instance),
      priceAdjustment = genBigDecimal.instance,
    )
    val orderBundleSet = RecoveredOrderBundleSet(
      id = UUID.randomUUID,
      bundleSetId = bundleSet.map(_.id),
      name = bundleSet.flatMap(_.name),
      position = Some(genInt.instance),
      orderBundleOptions = Seq(orderBundleOption),
    )

    val orderBundle = OrderBundleUpdate(
      id = None,
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      bundleOrderItemId = Some(bundleOrderItem.id),
      orderBundleSets = Some(Seq(orderBundleSet)),
    )
    Wrapper(orderBundle, daos.orderBundleDao)
  }

  def orderItem(
      order: OrderRecord,
      product: Option[ArticleRecord] = None,
      quantity: Option[BigDecimal] = None,
      unit: Option[UnitType] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      discountAmount: Option[BigDecimal] = None,
      taxAmount: Option[BigDecimal] = Some(2.34),
      basePriceAmount: Option[BigDecimal] = None,
      calculatedPriceAmount: Option[BigDecimal] = None,
      totalPriceAmount: Option[BigDecimal] = None,
      paymentStatus: Option[PaymentStatus] = None,
      giftCardPassRecipientEmail: Option[String] = None,
    ) = {
    val orderItem = OrderItemUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      productId = product.map(_.id),
      productName = product.map(_.name),
      productDescription = product.flatMap(_.description),
      productType = product.map(_.`type`),
      quantity = quantity.orElse(Some(1)),
      unit = unit.orElse(Some(UnitType.`Unit`)),
      paymentStatus = paymentStatus.orElse(Some(PaymentStatus.Pending)),
      priceAmount = priceAmount.orElse(Some(12.34)),
      costAmount = costAmount.orElse(Some(10.14)),
      discountAmount = discountAmount.orElse(Some(0.56)),
      taxAmount = taxAmount,
      basePriceAmount = basePriceAmount.orElse(Some(genBigDecimal.instance)),
      calculatedPriceAmount = calculatedPriceAmount.orElse(Some(genBigDecimal.instance)),
      totalPriceAmount = totalPriceAmount.orElse(Some(genBigDecimal.instance)),
      giftCardPassRecipientEmail = giftCardPassRecipientEmail,
      notes = None,
    )

    Wrapper(orderItem, daos.orderItemDao)
  }

  def orderItemDiscount(
      orderItem: OrderItemRecord,
      discount: DiscountRecord,
    ): Wrapper[OrderItemDiscountRecord, OrderItemDiscountUpdate] =
    orderItemDiscount(orderItem, Some(discount))

  def orderItemDiscount(
      orderItem: OrderItemRecord,
      discount: Option[DiscountRecord] = None,
    ): Wrapper[OrderItemDiscountRecord, OrderItemDiscountUpdate] = {
    val orderItemDiscount = OrderItemDiscountUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(orderItem.merchantId),
      orderItemId = Some(orderItem.id),
      discountId = discount.map(_.id),
      title = Some(randomWord),
      `type` = Some(genDiscountType.instance),
      amount = Some(genBigDecimal.instance),
      totalAmount = Some(genBigDecimal.instance),
    )

    Wrapper(orderItemDiscount, daos.orderItemDiscountDao)
  }

  def orderItemModifierOption(orderItem: OrderItemRecord, modifierOption: ModifierOptionRecord) = {
    val orderItemModifierOption = OrderItemModifierOptionUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(orderItem.merchantId),
      orderItemId = Some(orderItem.id),
      modifierOptionId = Some(modifierOption.id),
      name = Some(randomWord),
      modifierSetName = Some(randomWord),
      `type` = Some(genModifierSetType.instance),
      priceAmount = Some(genBigDecimal.instance),
      quantity = Some(genBigDecimal.instance),
    )

    Wrapper(orderItemModifierOption, daos.orderItemModifierOptionDao)
  }

  def orderItemVariantOption(orderItem: OrderItemRecord, variantOption: VariantOptionRecord) = {
    val orderItemVariantOption = OrderItemVariantOptionUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(orderItem.merchantId),
      orderItemId = Some(orderItem.id),
      variantOptionId = Some(variantOption.id),
      optionName = Some(randomWord),
      optionTypeName = Some(randomWord),
      position = Some(0),
    )

    Wrapper(orderItemVariantOption, daos.orderItemVariantOptionDao)
  }

  def orderUser(order: OrderRecord, user: UserRecord) = {
    val orderUser = OrderUserUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      userId = Some(user.id),
    )

    Wrapper(orderUser, daos.orderUserDao)
  }

  def paymentTransaction(
      order: OrderRecord,
      orderItems: Seq[OrderItemRecord] = Seq.empty,
      paymentDetails: Option[entities.PaymentDetails] = None,
      paymentType: Option[TransactionPaymentType] = None,
      `type`: Option[TransactionType] = None,
      refundedPaymentTransaction: Option[PaymentTransactionRecord] = None,
      paidAt: Option[ZonedDateTime] = None,
      paymentProcessor: Option[TransactionPaymentProcessor] = None,
    ) = {
    val paymentTransaction = PaymentTransactionUpdate(
      id = None,
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      customerId = None,
      `type` = `type`,
      refundedPaymentTransactionId = refundedPaymentTransaction.map(_.id),
      paymentType = paymentType,
      paymentDetails = paymentDetails.orElse(Gen.option(genPaymentDetails).instance),
      version = Some(2),
      paidAt = paidAt,
      paymentProcessor = paymentProcessor.orElse(Some(genTransactionPaymentProcessor.instance)),
    )
    Wrapper(
      paymentTransaction,
      daos.paymentTransactionDao,
      { paymentTransaction: PaymentTransactionRecord =>
        orderItems.foreach(oi => paymentTransactionOrderItem(paymentTransaction, oi).create)
      },
    )
  }

  def paymentTransactionFee(paymentTransaction: PaymentTransactionRecord, amount: Option[BigDecimal] = None) = {
    val paymentTransactionFee = PaymentTransactionFeeUpdate(
      id = None,
      merchantId = Some(paymentTransaction.merchantId),
      paymentTransactionId = Some(paymentTransaction.id),
      name = Some(randomWord),
      `type` = Some(genPaymentTransactionFeeType.instance),
      amount = amount.orElse(Some(genBigDecimal.instance)),
    )

    Wrapper(paymentTransactionFee, daos.paymentTransactionFeeDao)
  }

  def paymentTransactionOrderItem(paymentTransaction: PaymentTransactionRecord, orderItem: OrderItemRecord) = {
    val paymentTransactionOrderItem = PaymentTransactionOrderItemUpdate(
      id = None,
      merchantId = Some(paymentTransaction.merchantId),
      paymentTransactionId = Some(paymentTransaction.id),
      orderItemId = Some(orderItem.id),
    )

    Wrapper(paymentTransactionOrderItem, daos.paymentTransactionOrderItemDao)
  }

  private def product(
      merchant: MerchantRecord,
      `type`: ArticleType,
      isCombo: Boolean,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      isVariantOfProductId: Option[UUID] = None,
      id: Option[UUID] = Some(UUID.randomUUID),
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      trackInventoryParts: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) =
    article(
      merchant = merchant,
      `type` = `type`,
      name = name,
      categories = categories,
      locations = locations,
      isVariantOfProductId = isVariantOfProductId,
      id = id,
      active = active,
      upc = upc,
      priceAmount = priceAmount,
      costAmount = costAmount,
      sku = sku,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
      scope = ArticleScope.Product,
      isCombo = isCombo,
    )

  private def part(
      merchant: MerchantRecord,
      `type`: ArticleType,
      isCombo: Boolean,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      isVariantOfProductId: Option[UUID] = None,
      id: Option[UUID] = Some(UUID.randomUUID),
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      trackInventoryParts: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) =
    article(
      merchant = merchant,
      `type` = `type`,
      name = name,
      categories = categories,
      locations = locations,
      isVariantOfProductId = isVariantOfProductId,
      id = id,
      active = active,
      upc = upc,
      priceAmount = Some(0),
      costAmount = costAmount,
      sku = sku,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
      scope = ArticleScope.Part,
      isCombo = isCombo,
    )

  private def article(
      merchant: MerchantRecord,
      `type`: ArticleType,
      scope: ArticleScope,
      isCombo: Boolean,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      isVariantOfProductId: Option[UUID] = None,
      id: Option[UUID] = None,
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      trackInventoryParts: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val productId = id.getOrElse(UUID.randomUUID)
    val product = new ArticleUpdate(
      id = id,
      merchantId = Some(merchant.id),
      `type` = Some(`type`),
      isCombo = Some(isCombo),
      name = name.orElse(Some(s"aProductName $productId")),
      description = Some(s"a product description $productId"),
      brandId = None,
      priceAmount = priceAmount.orElse(Some(5.76)),
      costAmount = costAmount.getOrElse[BigDecimal](4.3),
      averageCostAmount = None,
      unit = Some(UnitType.`Unit`),
      margin = None,
      upc = if (`type`.isTemplate) None else Some(upc.orElse(Some(randomUpc))),
      sku = if (`type`.isTemplate) None else Some(sku.orElse(Some(s"$randomWord-$productId"))),
      isVariantOfProductId = isVariantOfProductId,
      hasVariant = Some(`type`.isTemplate),
      discountable = None,
      trackInventory = trackInventory,
      active = active,
      applyPricingToAllLocations = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      avatarBgColor = None,
      isService = None,
      trackInventoryParts = trackInventoryParts,
      hasParts = None,
      scope = Some(scope),
      deletedAt = deletedAt,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(
      product,
      daos.articleDao,
      { record: ArticleRecord =>
        categories.foreach(c => productCategory(record, c).create)
        locations.foreach(l => productLocation(record, l).create)
      },
    )
  }

  def simpleProduct(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      trackInventoryParts: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = Some(UUID.randomUUID)
    product(
      merchant,
      ArticleType.Simple,
      isCombo = false,
      name,
      categories,
      locations,
      isVariantOfProductId = id,
      id = id,
      active = active,
      upc = upc,
      sku = sku,
      priceAmount = priceAmount,
      costAmount = costAmount,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )
  }

  def templateProduct(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      deletedAt: Option[ZonedDateTime] = None,
      trackInventory: Option[Boolean] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) =
    product(
      merchant,
      ArticleType.Template,
      isCombo = false,
      name,
      categories,
      locations,
      isVariantOfProductId = None,
      active = active,
      upc = None,
      priceAmount = priceAmount,
      costAmount = costAmount,
      trackInventory = trackInventory,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )

  def variantProduct(
      merchant: MerchantRecord,
      templateProduct: ArticleRecord,
      name: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      upc: Option[String] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    assert(
      templateProduct.`type`.isTemplate && templateProduct.scope == ArticleScope.Product,
      "You can only create a variant of a template product",
    )
    product(
      merchant,
      ArticleType.Variant,
      isCombo = false,
      isVariantOfProductId = Some(templateProduct.id),
      locations = locations,
      upc = upc,
      sku = sku,
      name = name,
      priceAmount = priceAmount,
      costAmount = costAmount,
      trackInventory = trackInventory,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )
  }

  def variantProductWithTemplateId(merchant: MerchantRecord, templateProductId: Option[UUID]) = {
    require(templateProductId.isDefined, "templateProductId must exist")
    product(merchant, ArticleType.Variant, isCombo = false, isVariantOfProductId = templateProductId)
  }

  def comboProduct(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = Some(UUID.randomUUID)
    product(
      merchant,
      ArticleType.Simple,
      isCombo = true,
      name,
      categories,
      locations,
      isVariantOfProductId = id,
      id = id,
      active = active,
      upc = upc,
      sku = sku,
      priceAmount = priceAmount,
      costAmount = costAmount,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )
  }

  def simplePart(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      trackInventory: Option[Boolean] = None,
      trackInventoryParts: Option[Boolean] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = Some(UUID.randomUUID)
    part(
      merchant,
      ArticleType.Simple,
      isCombo = false,
      name,
      categories,
      locations,
      isVariantOfProductId = id,
      id = id,
      active = active,
      upc = upc,
      sku = sku,
      costAmount = costAmount,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )
  }

  def templatePart(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      costAmount: Option[BigDecimal] = None,
      deletedAt: Option[ZonedDateTime] = None,
    ) =
    part(
      merchant,
      ArticleType.Template,
      isCombo = false,
      name,
      categories,
      locations,
      isVariantOfProductId = None,
      active = active,
      upc = None,
      costAmount = costAmount,
      deletedAt = deletedAt,
    )

  def variantPart(
      merchant: MerchantRecord,
      templatePart: ArticleRecord,
      name: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      costAmount: Option[BigDecimal] = None,
      upc: Option[String] = None,
      sku: Option[String] = None,
    ) = {
    assert(
      templatePart.`type`.isTemplate && templatePart.scope == ArticleScope.Part,
      "You can only create a variant of a template part",
    )
    part(
      merchant,
      ArticleType.Variant,
      isCombo = false,
      isVariantOfProductId = Some(templatePart.id),
      locations = locations,
      upc = upc,
      sku = sku,
      name = name,
      costAmount = costAmount,
    )
  }

  def comboPart(
      merchant: MerchantRecord,
      name: Option[String] = None,
      categories: Seq[CategoryRecord] = Seq.empty,
      locations: Seq[LocationRecord] = Seq.empty,
      active: Option[Boolean] = None,
      upc: Option[String] = None,
      costAmount: Option[BigDecimal] = None,
      sku: Option[String] = None,
      deletedAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = Some(UUID.randomUUID)
    part(
      merchant,
      ArticleType.Simple,
      isCombo = true,
      name,
      categories,
      locations,
      id = id,
      active = active,
      upc = upc,
      sku = sku,
      costAmount = costAmount,
      deletedAt = deletedAt,
      overrideNow = overrideNow,
    )
  }

  def recipeDetail(recipe: ArticleRecord) = {
    require(
      recipe.isCombo && recipe.scope == ArticleScope.Part,
      "RecipeDetail can only be associated to a recipe (type = combo, scope = part)",
    )
    val recipeDetail = RecipeDetailUpdate(
      id = None,
      merchantId = Some(recipe.merchantId),
      productId = Some(recipe.id),
      makesQuantity = Some(genBigDecimal.instance),
    )
    Wrapper(recipeDetail, daos.recipeDetailDao)
  }

  def bundleSet(bundle: ArticleRecord) = {
    require(
      bundle.isCombo && bundle.scope == ArticleScope.Product,
      "BundleSet can only be associated to a recipe (type = combo, scope = product)",
    )
    val bundleSet = BundleSetUpdate(
      id = None,
      merchantId = Some(bundle.merchantId),
      bundleId = Some(bundle.id),
      name = Some(randomWord),
      position = Some(genInt.instance),
      minQuantity = Some(genInt.instance),
      maxQuantity = Some(genInt.instance),
    )
    Wrapper(bundleSet, daos.bundleSetDao)
  }

  def bundleOption(bundleSet: BundleSetRecord, article: ArticleRecord) = {
    val bundleOption = BundleOptionUpdate(
      id = None,
      merchantId = Some(bundleSet.merchantId),
      bundleSetId = Some(bundleSet.id),
      articleId = Some(article.id),
      priceAdjustment = Some(genBigDecimal.instance),
      position = Some(genInt.instance),
    )
    Wrapper(bundleOption, daos.bundleOptionDao)
  }

  def productCategory(
      product: ArticleRecord,
      category: CategoryRecord,
      position: Option[Int] = None,
    ) = {
    assert(!product.`type`.isVariant, "Categories can only be associated to non-variant products")
    val productCategory = ProductCategoryUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      categoryId = Some(category.id),
      position = position,
    )

    Wrapper(productCategory, daos.productCategoryDao)
  }

  def productCategoryOption(productCategory: ProductCategoryRecord) = {
    val productCategoryOption = ProductCategoryOptionUpdate(
      id = None,
      merchantId = Some(productCategory.merchantId),
      productCategoryId = Some(productCategory.id),
      deliveryEnabled = Some(genBoolean.instance),
      takeAwayEnabled = Some(genBoolean.instance),
    )

    Wrapper(productCategoryOption, daos.productCategoryOptionDao)
  }

  def productLocation(
      product: ArticleRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
      costAmount: Option[BigDecimal] = None,
      priceAmount: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
      routeToKitchen: Option[KitchenRecord] = None,
    ) = {
    val productLocation = new ProductLocationUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      locationId = Some(location.id),
      priceAmount = priceAmount.orElse(Some(10)),
      costAmount = costAmount,
      averageCostAmount = None,
      margin = None,
      unit = Some(UnitType.`Unit`),
      active = active,
      routeToKitchenId = routeToKitchen.map(_.id),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(productLocation, daos.productLocationDao)
  }

  def purchaseOrder(
      merchant: MerchantRecord,
      location: LocationRecord,
      user: UserRecord,
      status: Option[ReceivingObjectStatus] = None,
      sent: Option[Boolean] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ): Wrapper[PurchaseOrderRecord, PurchaseOrderUpdate] =
    supplier(merchant).wrapperOnCreated { supplier: SupplierRecord =>
      purchaseOrderWithSupplier(supplier, location, user, status, sent, overrideNow)
    }

  def purchaseOrderWithSupplier(
      supplier: SupplierRecord,
      location: LocationRecord,
      user: UserRecord,
      status: Option[ReceivingObjectStatus] = None,
      sent: Option[Boolean] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val purchaseOrder = new PurchaseOrderUpdate(
      id = None,
      merchantId = Some(supplier.merchantId),
      supplierId = Some(supplier.id),
      locationId = Some(location.id),
      userId = Some(user.id),
      paymentStatus = Some(genPurchaseOrderPaymentStatus.instance),
      expectedDeliveryDate = Some(genZonedDateTime.instance),
      status = status.orElse(Some(genReceivingObjectStatus.instance)),
      sent = sent.orElse(Some(false)),
      notes = Some("awesome notes here"),
      deletedAt = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(purchaseOrder, daos.purchaseOrderDao)
  }

  def purchaseOrderProduct(
      purchaseOrder: PurchaseOrderRecord,
      product: ArticleRecord,
      quantity: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
    ) = {
    val purchaseOrderProduct = PurchaseOrderProductUpdate(
      id = None,
      merchantId = Some(purchaseOrder.merchantId),
      purchaseOrderId = Some(purchaseOrder.id),
      productId = Some(product.id),
      quantity = quantity.orElse(Gen.option(genBigDecimal).instance),
      costAmount = costAmount.orElse(Gen.option(genBigDecimal).instance),
    )

    Wrapper(purchaseOrderProduct, daos.purchaseOrderProductDao)
  }

  def supplierLocation(
      supplier: SupplierRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
    ) = {
    val supplierLocation = SupplierLocationUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      supplierId = Some(supplier.id),
      active = active,
    )

    Wrapper(supplierLocation, daos.supplierLocationDao)
  }

  def supplierProduct(supplier: SupplierRecord, product: ArticleRecord) = {
    assert(product.`type`.isMain, "You can only assign supplier products to main products")
    val supplierProduct = SupplierProductUpdate(
      id = None,
      merchantId = Some(supplier.merchantId),
      productId = Some(product.id),
      supplierId = Some(supplier.id),
    )

    Wrapper(supplierProduct, daos.supplierProductDao)
  }

  def productLocationTaxRate(productLocation: ProductLocationRecord, taxRate: TaxRateRecord) = {
    val productLocationTaxRate = ProductLocationTaxRateUpdate(
      id = None,
      merchantId = Some(productLocation.merchantId),
      productLocationId = Some(productLocation.id),
      taxRateId = Some(taxRate.id),
    )

    Wrapper(productLocationTaxRate, daos.productLocationTaxRateDao)
  }

  def systemSubcategory(
      catalog: CatalogRecord,
      parent: CategoryRecord,
      name: Option[String] = None,
      position: Option[Int] = None,
      active: Option[Boolean] = None,
      locations: Seq[LocationRecord] = Seq.empty,
    ) = {
    val id = UUID.randomUUID
    systemCategory(
      id = Some(id),
      catalog = catalog,
      parentCategory = Some(parent),
      name = name.orElse(Some(s"Subcategory $id")),
      description = Some(s"Subcategory desc $id"),
      position = position.orElse(Gen.option(genInt).instance),
      active = active,
      locations = locations,
    )
  }

  def stock(
      productLocation: ProductLocationRecord,
      quantity: Option[BigDecimal] = None,
      reorderAmount: Option[BigDecimal] = None,
      minimumOnHand: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
      sellOutOfStock: Option[Boolean] = Some(true),
    ) = {
    val stock = new StockUpdate(
      id = None,
      merchantId = Some(productLocation.merchantId),
      productId = Some(productLocation.productId),
      locationId = Some(productLocation.locationId),
      quantity = quantity.orElse(Some(20L)),
      minimumOnHand = minimumOnHand.orElse(Some(10L)),
      reorderAmount = reorderAmount.orElse(Some(42L)),
      sellOutOfStock = sellOutOfStock,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }
    Wrapper(stock, daos.stockDao)
  }

  def supplier(
      merchant: MerchantRecord,
      name: Option[String] = None,
      deletedAt: Option[ZonedDateTime] = None,
      locations: Seq[LocationRecord] = Seq.empty,
    ) = {
    val id = UUID.randomUUID
    val supplier = SupplierUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      name = name.orElse(Some(s"Supplier $id")),
      contact = Some(randomWords),
      address = Some(randomWords),
      secondaryAddress = Some(randomWords),
      email = Some(s"$id@supplier.paytouch.io"),
      phoneNumber = Some("+391231231234"),
      secondaryPhoneNumber = Some("+391231231235"),
      accountNumber = Some(randomNumericString),
      notes = Some(randomWords(n = 10, allCapitalized = false)),
      deletedAt = deletedAt,
    )

    Wrapper(
      supplier,
      daos.supplierDao,
      { supplier: SupplierRecord =>
        locations.foreach(supplierLocation(supplier, _).create)
      },
    )
  }

  def taxRate(
      merchant: MerchantRecord,
      name: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      products: Seq[ArticleRecord] = Seq.empty,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val taxRate = new TaxRateUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      name = name.orElse(Some(s"Tax Rate $id")),
      value = Some(43.21),
      applyToPrice = None,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(
      taxRate,
      daos.taxRateDao,
      { taxRate: TaxRateRecord =>
        for {
          loc <- locations
          prod <- products
        } yield {
          val prodLocWrapper = productLocation(prod, loc)
          val prodLoc =
            daos.productLocationDao.findByRelIds(prodLocWrapper.get).await.getOrElse(prodLocWrapper.create)
          productLocationTaxRate(prodLoc, taxRate).create
        }
        locations.foreach(taxRateLocation(taxRate, _).create)
      },
    )
  }

  def taxRateLocation(
      taxRate: TaxRateRecord,
      location: LocationRecord,
      active: Option[Boolean] = None,
    ) = {
    val taxRateLocation = TaxRateLocationUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      taxRateId = Some(taxRate.id),
      locationId = Some(location.id),
      active = active,
    )

    Wrapper(taxRateLocation, daos.taxRateLocationDao)
  }

  def variantOptionType(product: ArticleRecord, name: Option[String] = None) = {
    assert(product.`type`.isTemplate, "You can only assign variant option types to Template products")
    val variantOptionType = VariantOptionTypeUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      name = name.orElse(Some("Variant Option Type")),
      position = Some(genInt.instance),
    )

    Wrapper(variantOptionType, daos.variantOptionTypeDao)
  }

  def variantOption(
      product: ArticleRecord,
      variantOptionType: VariantOptionTypeRecord,
      name: String,
    ) = {
    assert(product.`type`.isTemplate, "You can only assign variant option to Template products")
    val variantOption = VariantOptionUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      variantOptionTypeId = Some(variantOptionType.id),
      name = Some(name),
      position = Some(genInt.instance),
    )

    Wrapper(variantOption, daos.variantOptionDao)
  }

  def productVariantOption(product: ArticleRecord, variantOption: VariantOptionRecord) = {
    assert(product.`type`.isVariant, "You can only assign product variant options to Variants products")
    val productVariantOption = ProductVariantOptionUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      variantOptionId = Some(variantOption.id),
    )

    Wrapper(productVariantOption, daos.productVariantOptionDao)
  }

  def user(
      merchant: MerchantRecord,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      password: Option[String] = None,
      email: Option[String] = None,
      locations: Seq[LocationRecord] = Seq.empty,
      userRole: Option[UserRoleRecord] = None,
      active: Option[Boolean] = None,
      hourlyRateAmount: Option[BigDecimal] = None,
      overtimeRateAmount: Option[BigDecimal] = None,
      deletedAt: Option[ZonedDateTime] = None,
      isOwner: Option[Boolean] = None,
      pin: Option[String] = None,
      auth0UserId: Option[String] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val id = UUID.randomUUID
    val encryptedPassword = bcryptEncrypt(password.getOrElse("aPassword"))
    val user = new UserUpdate(
      id = Some(id),
      merchantId = Some(merchant.id),
      userRoleId = userRole.map(_.id),
      firstName = firstName.orElse(Some(s"$id-firstName")),
      lastName = lastName.orElse(Some(s"$id-lastName")),
      encryptedPassword = Some(encryptedPassword),
      pin = pin.map(sha1Encrypt),
      email = email.orElse(Some(s"$id@paytouch.io")),
      dob = None,
      phoneNumber = None,
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
      avatarBgColor = None,
      active = active,
      hourlyRateAmount = hourlyRateAmount,
      overtimeRateAmount = overtimeRateAmount,
      paySchedule = None,
      auth0UserId = auth0UserId.map(id => Auth0UserId(id)),
      isOwner = isOwner,
      deletedAt = deletedAt,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(user, daos.userDao, { user: UserRecord => locations.foreach(l => userLocation(user, l).create) })
  }

  def userLocation(user: UserRecord, location: LocationRecord) = {
    val userLocation = UserLocationUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(location.id),
      userId = Some(user.id),
    )

    Wrapper(userLocation, daos.userLocationDao)
  }

  def userRole(
      merchant: MerchantRecord,
      name: Option[String] = None,
      permissions: Option[entities.PermissionsUpdate] = None,
      hasDashboardAccess: Option[Boolean] = None,
      hasRegisterAccess: Option[Boolean] = None,
      hasTicketsAccess: Option[Boolean] = None,
    ) = {
    val userRole =
      UserRoleUpdate
        .Paytouch
        .admin(merchant.id)
        .copy(
          hasDashboardAccess = hasDashboardAccess,
          hasRegisterAccess = hasRegisterAccess,
          hasTicketsAccess = hasTicketsAccess,
        )

    val userRoleWithPermissions = userRole.copy(
      name = name.orElse(userRole.name),
      dashboard = permissions.orElse(userRole.dashboard),
      register = permissions.orElse(userRole.register),
    )

    Wrapper(userRoleWithPermissions, daos.userRoleDao)
  }

  def productQuantityHistory(
      product: ArticleRecord,
      location: LocationRecord,
      user: Option[UserRecord] = None,
      order: Option[OrderRecord] = None,
      date: Option[ZonedDateTime] = None,
    ) = {
    val change = ProductQuantityHistoryUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      locationId = Some(location.id),
      userId = user.map(_.id),
      orderId = order.map(_.id),
      date = date.orElse(Some(UtcTime.now)),
      prevQuantityAmount = Some(1.0),
      newQuantityAmount = Some(2.0),
      newStockValueAmount = Some(1.5),
      reason = Some(genQuantityChangeReason.instance),
      notes = Some(randomWords),
    )

    Wrapper(change, daos.productQuantityHistoryDao)
  }

  def productCostHistory(
      product: ArticleRecord,
      location: LocationRecord,
      user: UserRecord,
      date: Option[ZonedDateTime] = None,
    ) = {
    val change = ProductCostHistoryUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      locationId = Some(location.id),
      userId = Some(user.id),
      date = date.orElse(Some(UtcTime.now)),
      prevCostAmount = Some(1.0),
      newCostAmount = Some(2.0),
      reason = Some(genChangeReason.instance),
      notes = Some(randomWords),
    )

    Wrapper(change, daos.productCostHistoryDao)
  }

  def productPriceHistory(
      product: ArticleRecord,
      location: LocationRecord,
      user: UserRecord,
      date: Option[ZonedDateTime] = None,
    ) = {
    val change = ProductPriceHistoryUpdate(
      id = None,
      merchantId = Some(product.merchantId),
      productId = Some(product.id),
      locationId = Some(location.id),
      userId = Some(user.id),
      date = date.orElse(Some(UtcTime.now)),
      prevPriceAmount = Some(1.0),
      newPriceAmount = Some(2.0),
      reason = Some(genChangeReason.instance),
      notes = Some(randomWords),
    )

    Wrapper(change, daos.productPriceHistoryDao)
  }

  def locationSettings(
      location: LocationRecord,
      onlineStoreFrontEnabled: Option[Boolean] = None,
      deliveryProvidersEnabled: Option[Boolean] = None,
      rapidoEnabled: Option[Boolean] = None,
      orderAutocomplete: Option[Boolean] = None,
      nextOrderNumberScopeType: Option[ScopeType] = None,
    ) = {
    val locationSettings = LocationSettingsUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      orderRoutingAuto = genOptBoolean.instance,
      orderTypeDineIn = genOptBoolean.instance,
      orderTypeTakeOut = genOptBoolean.instance,
      orderTypeDeliveryRestaurant = genOptBoolean.instance,
      orderTypeInStore = genOptBoolean.instance,
      orderTypeInStorePickUp = genOptBoolean.instance,
      orderTypeDeliveryRetail = genOptBoolean.instance,
      webStorefrontActive = genOptBoolean.instance,
      mobileStorefrontActive = genOptBoolean.instance,
      facebookStorefrontActive = genOptBoolean.instance,
      invoicesActive = genOptBoolean.instance,
      discountBelowCostActive = genOptBoolean.instance,
      cashDrawerManagementActive = Some(true),
      cashDrawerManagement = Some(CashDrawerManagementMode.Unlocked),
      giftCardsActive = genOptBoolean.instance,
      paymentTypeCreditCard = genOptBoolean.instance,
      paymentTypeCash = genOptBoolean.instance,
      paymentTypeDebitCard = genOptBoolean.instance,
      paymentTypeCheck = genOptBoolean.instance,
      paymentTypeGiftCard = genOptBoolean.instance,
      paymentTypeStoreCredit = genOptBoolean.instance,
      paymentTypeEbt = genOptBoolean.instance,
      paymentTypeApplePay = genOptBoolean.instance,
      tipsHandling = Some(TipsHandlingMode.TipJar),
      tipsOnDeviceEnabled = genOptBoolean.instance,
      bypassSignatureAmount = Gen.option(genBigDecimal).instance,
      onlineStorefrontEnabled = onlineStoreFrontEnabled.orElse(genOptBoolean.instance),
      deliveryProvidersEnabled = deliveryProvidersEnabled.orElse(genOptBoolean.instance),
      maxDrivingDistanceInMeters = Gen.option(genBigDecimal).instance,
      orderAutocomplete = orderAutocomplete.orElse(genOptBoolean.instance),
      preauthEnabled = genOptBoolean.instance,
      nextOrderNumberScopeType = nextOrderNumberScopeType,
      cfd = None,
      onlineOrder = None,
      rapidoEnabled = onlineStoreFrontEnabled.orElse(genOptBoolean.instance),
    )

    Wrapper(
      locationSettings,
      daos.locationSettingsDao,
      { lsr: LocationSettingsRecord =>
        locationEmailReceipt(location).create
        locationPrintReceipt(location).create
        locationReceipt(location).create
      },
    )
  }

  def locationEmailReceipt(location: LocationRecord) = {
    val receipt = LocationEmailReceiptUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      headerColor = None,
      locationName = None,
      locationAddressLine1 = None,
      locationAddressLine2 = None,
      locationCity = None,
      locationState = None,
      locationCountry = None,
      locationStateCode = None,
      locationCountryCode = None,
      locationPostalCode = None,
      includeItemDescription = None,
      websiteUrl = None,
      facebookUrl = None,
      twitterUrl = None,
    )

    Wrapper(receipt, daos.locationEmailReceiptDao)
  }

  def locationPrintReceipt(location: LocationRecord) = {
    val receipt = LocationPrintReceiptUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      headerColor = None,
      locationName = None,
      locationAddressLine1 = None,
      locationAddressLine2 = None,
      locationCity = None,
      locationState = None,
      locationCountry = None,
      locationStateCode = None,
      locationCountryCode = None,
      locationPostalCode = None,
      includeItemDescription = None,
    )

    Wrapper(receipt, daos.locationPrintReceiptDao)
  }

  def locationReceipt(location: LocationRecord, locationName: Option[String] = None) = {
    val receipt = LocationReceiptUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      locationName = locationName.orElse(Some(location.name)),
      headerColor = None,
      addressLine1 = location.addressLine1,
      addressLine2 = location.addressLine2,
      city = location.city,
      state = location.state,
      country = location.country,
      codes = ResettableCodes(
        stateCode = location.stateCode,
        countryCode = location.countryCode,
      ),
      postalCode = location.postalCode,
      phoneNumber = location.phoneNumber,
      websiteUrl = location.website,
      facebookUrl = Gen.option(genWord).instance,
      twitterUrl = Gen.option(genWord).instance,
      showCustomText = genOptBoolean.instance,
      customText = Gen.option(genWord).instance,
      showReturnPolicy = genOptBoolean.instance,
      returnPolicyText = Gen.option(genWord).instance,
    )

    Wrapper(receipt, daos.locationReceiptDao)
  }

  def shift(
      user: UserRecord,
      location: LocationRecord,
      status: Option[ShiftStatus] = None,
      startDate: Option[LocalDate] = None,
      endDate: Option[LocalDate] = None,
      startTime: Option[LocalTime] = None,
      endTime: Option[LocalTime] = None,
      days: Seq[DayOfWeek] = Seq.empty,
      repeat: Option[Boolean] = None,
    ) = {
    import DayOfWeek._

    val shift = ShiftUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      locationId = Some(location.id),
      startDate = startDate.orElse(Some(LocalDate.of(2016, 7, 3))),
      endDate = endDate.orElse(Some(LocalDate.of(2016, 7, 10))),
      startTime = startTime.orElse(Some(LocalTime.of(12, 34, 56))),
      endTime = endTime.orElse(Some(LocalTime.of(23, 45, 7))),
      unpaidBreakMins = None,
      repeat = repeat,
      frequencyInterval = None,
      frequencyCount = None,
      sunday = Some(days.contains(SUNDAY)),
      monday = Some(days.contains(MONDAY)),
      tuesday = Some(days.contains(TUESDAY)),
      wednesday = Some(days.contains(WEDNESDAY)),
      thursday = Some(days.contains(THURSDAY)),
      friday = Some(days.contains(FRIDAY)),
      saturday = Some(days.contains(SATURDAY)),
      status = status,
      bgColor = None,
      sendShiftStartNotification = None,
      notes = None,
    )

    Wrapper(shift, daos.shiftDao)
  }

  def receivingOrder(
      location: LocationRecord,
      user: UserRecord,
      overrideNow: Option[ZonedDateTime] = None,
      status: Option[ReceivingOrderStatus] = None,
      synced: Option[Boolean] = None,
    ) =
    genericReceivingOrder(
      location,
      user,
      overrideNow = overrideNow,
      status = status,
      synced = synced,
    )

  def receivingOrderOfTransfer(
      location: LocationRecord,
      user: UserRecord,
      transfer: TransferOrderRecord,
      status: Option[ReceivingOrderStatus] = None,
    ) =
    genericReceivingOrder(location, user, Some(ReceivingOrderObjectType.Transfer), Some(transfer.id), status)

  def receivingOrderOfPurchaseOrder(
      location: LocationRecord,
      user: UserRecord,
      purchaseOrder: PurchaseOrderRecord,
      status: Option[ReceivingOrderStatus] = None,
      paymentStatus: Option[ReceivingOrderPaymentStatus] = None,
    ) =
    genericReceivingOrder(
      location,
      user,
      Some(ReceivingOrderObjectType.PurchaseOrder),
      Some(purchaseOrder.id),
      status,
      paymentStatus,
    )

  private def genericReceivingOrder(
      location: LocationRecord,
      user: UserRecord,
      `type`: Option[ReceivingOrderObjectType] = None,
      receivingObjectId: Option[UUID] = None,
      status: Option[ReceivingOrderStatus] = None,
      paymentStatus: Option[ReceivingOrderPaymentStatus] = None,
      overrideNow: Option[ZonedDateTime] = None,
      synced: Option[Boolean] = None,
    ) = {
    val receivingOrder = new ReceivingOrderUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      userId = Some(user.id),
      receivingObjectType = `type`,
      receivingObjectId = receivingObjectId,
      status = status.orElse(Some(genReceivingOrderStatus.instance)),
      synced = synced.orElse(Some(false)),
      invoiceNumber = Gen.option(genWord).instance,
      paymentMethod = Gen.option(genReceivingOrderPaymentMethod).instance,
      paymentStatus = paymentStatus.orElse(Gen.option(genReceivingOrderPaymentStatus).instance),
      paymentDueDate = Gen.option(genZonedDateTime).instance,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(receivingOrder, daos.receivingOrderDao)
  }

  def receivingOrderProduct(
      receivingOrder: ReceivingOrderRecord,
      product: ArticleRecord,
      quantity: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
    ) = {
    val receivingOrderProduct = ReceivingOrderProductUpdate(
      id = None,
      merchantId = Some(receivingOrder.merchantId),
      receivingOrderId = Some(receivingOrder.id),
      productId = Some(product.id),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      quantity = quantity.orElse(Gen.option(genBigDecimal).instance),
      costAmount = costAmount,
    )

    Wrapper(receivingOrderProduct, daos.receivingOrderProductDao)
  }

  def returnOrder(
      user: UserRecord,
      supplier: SupplierRecord,
      location: LocationRecord,
      purchaseOrder: Option[PurchaseOrderRecord] = None,
      synced: Option[Boolean] = None,
      status: Option[ReturnOrderStatus] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val returnOrder = new ReturnOrderUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      supplierId = Some(supplier.id),
      locationId = Some(location.id),
      purchaseOrderId = purchaseOrder.map(_.id),
      notes = Some(randomWords),
      status = status.orElse(Some(genReturnStatus.instance)),
      synced = synced,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(returnOrder, daos.returnOrderDao)
  }

  def returnOrderProduct(
      returnOrder: ReturnOrderRecord,
      product: ArticleRecord,
      quantity: Option[BigDecimal] = None,
    ) = {
    val returnOrderProduct = ReturnOrderProductUpdate(
      id = None,
      merchantId = Some(returnOrder.merchantId),
      returnOrderId = Some(returnOrder.id),
      productId = Some(product.id),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      reason = Some(genReturnReason.instance),
      quantity = quantity.orElse(Gen.option(genBigDecimal).instance),
    )

    Wrapper(returnOrderProduct, daos.returnOrderProductDao)
  }

  def transferOrder(
      fromLocation: LocationRecord,
      toLocation: LocationRecord,
      user: UserRecord,
      status: Option[ReceivingObjectStatus] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val transferOrder = new TransferOrderUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      fromLocationId = Some(fromLocation.id),
      toLocationId = Some(toLocation.id),
      userId = Some(user.id),
      notes = Some(randomWords),
      status = status.orElse(Some(genReceivingObjectStatus.instance)),
      `type` = Some(genTransferOrderType.instance),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(transferOrder, daos.transferOrderDao)
  }

  def transferOrderProduct(
      transferOrder: TransferOrderRecord,
      product: ArticleRecord,
      quantity: Option[BigDecimal] = None,
    ) = {
    val transferOrderProduct = TransferOrderProductUpdate(
      id = None,
      merchantId = Some(transferOrder.merchantId),
      transferOrderId = Some(transferOrder.id),
      productId = Some(product.id),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      quantity = quantity.orElse(Gen.option(genBigDecimal).instance),
    )

    Wrapper(transferOrderProduct, daos.transferOrderProductDao)
  }

  def createValidTokenWithSession(user: UserRecord) = {
    val source = LoginSource.PtDashboard
    val session = this.session(user, source)
    generateUserJsonWebToken(session)
  }

  def valitTokenForSession(session: SessionRecord) =
    generateUserJsonWebToken(session)

  def session(
      user: UserRecord,
      source: LoginSource = LoginSource.PtDashboard,
      adminId: Option[UUID] = None,
    ) =
    daos
      .sessionDao
      .create(user.id, source, adminId)
      .await

  def orderTaxRate(
      order: OrderRecord,
      taxRate: TaxRateRecord,
      totalAmount: Option[BigDecimal] = None,
    ) = {
    val orderTaxRate = OrderTaxRateUpdate(
      id = None,
      merchantId = Some(order.merchantId),
      orderId = Some(order.id),
      taxRateId = Some(taxRate.id),
      name = Some(randomWord),
      value = Some(genBigDecimal.instance),
      totalAmount = totalAmount.orElse(Some(genBigDecimal.instance)),
    )

    Wrapper(orderTaxRate, daos.orderTaxRateDao)
  }

  def orderItemTaxRate(orderItem: OrderItemRecord, taxRate: TaxRateRecord) = {
    val orderItemTaxRate = OrderItemTaxRateUpdate(
      id = None,
      merchantId = Some(orderItem.merchantId),
      orderItemId = Some(orderItem.id),
      taxRateId = Some(taxRate.id),
      name = Some(randomWord),
      value = Some(genBigDecimal.instance),
      totalAmount = Some(genBigDecimal.instance),
      applyToPrice = Gen.option(genBoolean).instance,
      active = Gen.option(genBoolean).instance,
    )

    Wrapper(orderItemTaxRate, daos.orderItemTaxRateDao)
  }

  def inventoryCount(
      location: LocationRecord,
      user: UserRecord,
      valueChangeAmount: Option[BigDecimal] = None,
      status: Option[InventoryCountStatus] = None,
      synced: Option[Boolean] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val inventoryCount = new InventoryCountUpdate(
      id = None,
      merchantId = Some(location.merchantId),
      locationId = Some(location.id),
      userId = Some(user.id),
      valueChangeAmount = valueChangeAmount.orElse(Some(genBigDecimal.instance)),
      status = status.orElse(Some(genInventoryCountStatus.instance)),
      synced = synced.orElse(Some(false)),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(inventoryCount, daos.inventoryCountDao)
  }

  def inventoryCountProduct(
      inventoryCount: InventoryCountRecord,
      product: ArticleRecord,
      countedQuantity: Option[BigDecimal] = None,
      expectedQuantity: Option[BigDecimal] = None,
    ) = {
    val inventoryCountProduct = InventoryCountProductUpdate(
      id = None,
      merchantId = Some(inventoryCount.merchantId),
      inventoryCountId = Some(inventoryCount.id),
      productId = Some(product.id),
      productName = Some(product.name),
      expectedQuantity = expectedQuantity.orElse(Gen.option(genBigDecimal).instance),
      countedQuantity = countedQuantity.orElse(Gen.option(genBigDecimal).instance),
      valueAmount = Gen.option(genBigDecimal).instance,
      costAmount = product.costAmount,
      valueChangeAmount = Gen.option(genBigDecimal).instance,
    )

    Wrapper(inventoryCountProduct, daos.inventoryCountProductDao)
  }

  def productPart(
      product: ArticleRecord,
      part: ArticleRecord,
      quantityNeeded: Option[BigDecimal] = None,
    ) = {
    require(part.scope == ArticleScope.Part, "part must have scope Part")
    val productPart = ProductPartUpdate(
      id = None,
      productId = Some(product.id),
      merchantId = Some(product.merchantId),
      partId = Some(part.id),
      quantityNeeded = quantityNeeded.orElse(Some(genBigDecimal.instance)),
    )

    Wrapper(productPart, daos.productPartDao)
  }

  def export(
      merchant: MerchantRecord,
      status: Option[ExportStatus] = None,
      baseUrl: Option[String] = None,
    ) = {
    val export = ExportUpdate(
      id = None,
      merchantId = Some(merchant.id),
      `type` = Some(randomWord),
      status = status.orElse(Some(ExportStatus.Completed)),
      baseUrl = baseUrl,
    )
    Wrapper(export, daos.exportDao)
  }

  def giftCard(
      giftCardProduct: ArticleRecord,
      active: Option[Boolean] = None,
      appleWalletTemplateId: Option[String] = None,
      androidPayTemplateId: Option[String] = None,
      amounts: Option[Seq[BigDecimal]] = None,
      overrideNow: Option[ZonedDateTime] = None,
      businessName: Option[String] = None,
    ) = {
    require(giftCardProduct.`type` == ArticleType.GiftCard, "gift card need a GiftCard article type")

    val update = new GiftCardUpdate(
      id = None,
      merchantId = Some(giftCardProduct.merchantId),
      productId = Some(giftCardProduct.id),
      amounts = amounts.orElse(Some(Gen.listOf(genBigDecimal).instance)),
      businessName = businessName.orElse(Some("Business Name")),
      templateDetails = Gen.option(genWord).instance,
      appleWalletTemplateId = appleWalletTemplateId,
      androidPayTemplateId = androidPayTemplateId,
      active = active.orElse(genOptBoolean.instance),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(update, daos.giftCardDao)
  }

  def giftCardPass(
      giftCard: GiftCardRecord,
      orderItem: OrderItemRecord,
      lookupId: Option[String] = None,
      originalAmount: Option[BigDecimal] = None,
      balanceAmount: Option[BigDecimal] = None,
      recipientEmail: Option[String] = None,
      androidPassPublicUrl: Option[String] = None,
      iosPassPublicUrl: Option[String] = None,
      passInstalledAt: Option[ZonedDateTime] = None,
      overrideNow: Option[ZonedDateTime] = None,
      isCustomAmount: Option[Boolean] = None,
    ) = {
    val anOriginalAmount = genBigDecimal.instance
    val aBalance = anOriginalAmount - Gen.oneOf(1, 2, 3, 4, 5).instance
    val update = new GiftCardPassUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(giftCard.merchantId),
      lookupId = lookupId.orElse(Some(genLookupId.instance)),
      giftCardId = Some(giftCard.id),
      orderItemId = Some(orderItem.id),
      originalAmount = Some(originalAmount.getOrElse(anOriginalAmount)),
      balanceAmount = Some(balanceAmount.getOrElse(aBalance)),
      iosPassPublicUrl = iosPassPublicUrl,
      androidPassPublicUrl = androidPassPublicUrl,
      isCustomAmount = isCustomAmount,
      passInstalledAt = passInstalledAt,
      recipientEmail = recipientEmail,
      onlineCode = GiftCardPassService.generateOnlineCode().some,
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(update, daos.giftCardPassDao)
  }

  def giftCardPassTransaction(
      giftCardPass: GiftCardPassRecord,
      totalAmount: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) = {
    val update = new GiftCardPassTransactionUpdate(
      id = None,
      merchantId = Some(giftCardPass.merchantId),
      giftCardPassId = Some(giftCardPass.id),
      totalAmount = totalAmount.orElse(Some(genBigDecimal.instance)),
    ) {
      override def now = overrideNow.getOrElse(UtcTime.now)
    }

    Wrapper(update, daos.giftCardPassTransactionDao)
  }

  def giftCardProduct(merchant: MerchantRecord) =
    product(
      merchant,
      ArticleType.GiftCard,
      isCombo = false,
    )

  def tipsAssignment(
      merchant: MerchantRecord,
      location: LocationRecord,
      user: Option[UserRecord] = None,
      order: Option[OrderRecord] = None,
      amount: Option[BigDecimal] = None,
      handledVia: Option[HandledVia] = None,
      handledViaCashDrawerActivity: Option[CashDrawerActivityRecord] = None,
      cashDrawerActivity: Option[CashDrawerActivityRecord] = None,
      overrideNow: Option[ZonedDateTime] = None,
    ) =
    Wrapper(
      dao = daos.tipsAssignmentDao,
      update = new TipsAssignmentUpdate(
        id = None,
        merchantId = Some(merchant.id),
        locationId = Some(location.id),
        userId = user.map(_.id),
        orderId = order.map(_.id),
        amount = amount.orElse(Some(genBigDecimal.instance)),
        handledVia = handledVia.orElse(Some(HandledVia.Unhandled)),
        handledViaCashDrawerActivityId = handledViaCashDrawerActivity.map(_.id),
        cashDrawerActivityId = cashDrawerActivity.map(_.id),
        paymentType = None,
        assignedAt = Some(genZonedDateTime.instance),
        deletedAt = None,
      ) {
        override def now = overrideNow.getOrElse(UtcTime.now)
      },
    )

  def oauthApp(name: Option[String] = None, redirectUris: Option[String] = None) = {
    val update = OAuthAppUpdate(
      id = None,
      clientId = Some(generateUuid),
      clientSecret = Some(generateUuid),
      name = name.orElse(Some(randomWord)),
      redirectUris = redirectUris.orElse(Some(randomWord)),
    )

    Wrapper(update, daos.oauthAppDao)
  }

  def oauthAppSession(
      merchant: MerchantRecord,
      oauthApp: OAuthAppRecord,
      session: SessionRecord,
    ) = {
    val update = OAuthAppSessionUpdate(
      id = None,
      merchantId = Some(merchant.id),
      oauthAppId = Some(oauthApp.id),
      sessionId = Some(session.id),
    )

    Wrapper(update, daos.oauthAppSessionDao)
  }

  def oauthCode(
      merchant: MerchantRecord,
      user: UserRecord,
      oauthApp: OAuthAppRecord,
    ) = {
    val update = OAuthCodeUpdate(
      id = None,
      merchantId = Some(merchant.id),
      userId = Some(user.id),
      oauthAppId = Some(oauthApp.id),
      code = Some(generateUuid),
    )

    Wrapper(update, daos.oauthCodeDao)
  }

  def passwordResetToken(user: UserRecord, expiresAt: Option[ZonedDateTime] = None) = {
    val now = UtcTime.now

    val update = PasswordResetTokenUpdate(
      id = None,
      userId = Some(user.id),
      key = Some(genPasswordResetTokenKey.instance),
      expiresAt = expiresAt.orElse(Some(now.plusMinutes(10))),
    )

    Wrapper(update, daos.passwordResetTokenDao)
  }
}
