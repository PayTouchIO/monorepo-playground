package io.paytouch.seeds

import java.util.Currency

import scala.concurrent._
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.db.DatabaseProvider
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.BusinessType
import io.paytouch.core.data.model.{ UserRecord, UserRoleRecord, UserRoleUpdate }
import io.paytouch.utils.{ FutureHelpers, Generators, TestExecutionContext }
import io.paytouch.core.{ ServiceConfigurations => Config }

object LoadSeeds extends App with LazyLogging with FutureHelpers with TestExecutionContext {
  val seeds = IdsProvider.users.map(Seeds.load)

  Await.result(Future.sequence(seeds), Duration.Inf)
  logger.info("All the seeds have been completed!")
}

object Seeds extends LazyLogging with TestExecutionContext {
  def loadUser(
      first: String,
      last: String,
      email: String,
    ): Future[(UserRecord, Seq[UserRoleRecord])] =
    for {
      merchant <- MerchantSeeds.load(email)
      userRoles <- UserRoleSeeds.load(email, merchant)
      adminUserRole = userRoles.find(_.name == UserRoleUpdate.Admin).get
      user <- UserSeeds.load(firstName = first, lastName = last, email = email, adminUserRole)
      _ <- AdminSeeds.load(first, last, email)
    } yield {
      logger.info(s"Basic data seeds for user $email completed")
      (user, userRoles)
    }

  def loadUserData(userRoles: Seq[UserRoleRecord])(implicit user: UserRecord): Future[Unit] =
    for {
      brands <- BrandSeeds.load
      suppliers <- SupplierSeeds.load
      mainCategories <- CategorySeeds.load
      subcategories <- SubcategorySeeds.load(mainCategories)
      categories = mainCategories ++ subcategories
      locations <- LocationSeeds.load
      locationSettings <- LocationSettingsSeeds.load(locations)
      _ <- LocationReceiptsSeeds.load(locations)
      employees <- EmployeeSeeds.load(userRoles)
      allUsers = employees :+ user
      userLocations <- UserLocationSeeds.load(locations, allUsers)
      simpleProducts <- SimpleProductSeeds.load(brands)
      templateProducts <- TemplateProductSeeds.load(brands)
      variantProducts <- VariantProductSeeds.load(templateProducts)
      products = simpleProducts ++ templateProducts ++ variantProducts
      storableProducts = simpleProducts ++ variantProducts
      mainProducts = simpleProducts ++ templateProducts
      variantOptionTypes <- VariantOptionTypeSeeds.load(templateProducts)
      variantOptions <- VariantOptionSeeds.load(variantOptionTypes)
      productCategories <- ProductCategorySeeds.load(mainProducts, categories)
      productLocations <- ProductLocationSeeds.load(products, locations)
      productVariantOptions <- ProductVariantOptionSeeds.load(variantOptions, variantProducts)
      stocks <- StockSeeds.load(productLocations)
      taxRates <- TaxRateSeeds.load
      taxRateLocations <- TaxRateLocationSeeds.load(taxRates, locations)
      productLocationTaxRates <- ProductLocationTaxRateSeeds.load(taxRateLocations, productLocations)
      categoryLocations <- CategoryLocationSeeds.load(categories, locations)
      categoryLocationAvailabilities <- CategoryLocationAvailabilitySeeds.load(categoryLocations)
      discounts <- DiscountSeeds.load
      discountLocations <- DiscountLocationSeeds.load(discounts, locations)
      discountAvailabilities <- DiscountAvailabilitySeeds.load(discounts)
      modifierSets <- ModifierSetSeeds.load
      modifierOptions <- ModifierOptionSeeds.load(modifierSets)
      modifierSetLocations <- ModifierSetLocationSeeds.load(modifierSets, locations)
      modifierSetProducts <- ModifierSetProductSeeds.load(modifierSets, mainProducts)
      groups <- GroupSeeds.load
      customers <- GlobalCustomerSeeds.load
      customerGroups <- CustomerGroupSeeds.load(customers, groups)
      customerLocations <- CustomerLocationSeeds.load(customers, locations)
      customerMerchants <- CustomerMerchantSeeds.load(customers)
      loyaltyPrograms <- LoyaltyProgramSeeds.load
      loyaltyRewards <- LoyaltyRewardSeeds.load(loyaltyPrograms)
      orders <- OrderSeeds.load(customerLocations, allUsers)
      orderItems <- OrderItemSeeds.load(orders, storableProducts)
      orderFeedbacks <- OrderFeedbackSeeds.load(orders)
      paymentTransactions <- PaymentTransactionSeeds.load(orders)
      productImageUploads <- ProductImageUploadSeeds.load(mainProducts)
      shifts <- ShiftSeeds.load(locations, allUsers)
      timeCards <- TimeCardSeeds.load(shifts)
      timeOffCards <- TimeOffCardSeeds.load(allUsers)
      orderUsers <- OrderUserSeeds.load(orders, allUsers)
      orderItemDiscounts <- OrderItemDiscountSeeds.load(orderItems, discounts)
      orderItemModifierOptions <-
        OrderItemModifierOptionSeeds
          .load(orderItems, modifierSetProducts, modifierSets, modifierOptions)
      orderItemVariantOptions <-
        OrderItemVariantOptionSeeds
          .load(orderItems, productVariantOptions, variantOptions, variantOptionTypes)
      productCostHistories <- ProductCostHistorySeeds.load(storableProducts, productLocations, employees)
      productPriceHistories <- ProductPriceHistorySeeds.load(storableProducts, productLocations, employees)
      productQuantityHistories <-
        ProductQuantityHistorySeeds
          .load(storableProducts, productLocations, employees, orders)
      supplierLocations <- SupplierLocationSeeds.load(suppliers, locations)
      supplierProducts <- SupplierProductSeeds.load(suppliers, mainProducts)
      tickets <- TicketSeeds.load(orders)
      ticketOrderItems <- TicketOrderItemSeeds.load(tickets, orderItems)
      cashDrawers <- CashDrawerSeeds.load(userLocations)
      cashDrawerActivities <- CashDrawerActivitySeeds.load(cashDrawers)
      loyaltyProgramLocations <- LoyaltyProgramLocationSeeds.load(loyaltyPrograms, locations)
      loyaltyRewardProducts <- LoyaltyRewardProductSeeds.load(loyaltyRewards, products)
      loyaltyMemberships <- LoyaltyMembershipSeeds.load(loyaltyPrograms, customerMerchants)
      _ <- AdminReportSeeds.load
    } yield logger.info(s"Extra data seeds for user ${user.email} completed")

  def load(user: (String, String, String)): Future[Unit] =
    user match {
      case (first, last, email) =>
        logger.info(s"Loading seeds for user $email")
        for {
          (user, userRoles) <- loadUser(first = first, last = last, email = email)
          _ <- if (EmailHelper.hasUserData(email)) loadUserData(userRoles)(user) else Future.unit
        } yield logger.info(s"Seeds for user $email completed")
    }

  def loadByEmail(email: String) = IdsProvider.users.filter { case (_, _, eml) => eml == email }.map(Seeds.load)
}

trait Seeds extends FutureHelpers with LazyLogging with Generators {

  val bcryptRounds = Config.bcryptRounds

  implicit lazy val db = SeedsDatabase.db

  implicit lazy val daos = new Daos

  val USD = Currency.getInstance("USD")
}

object SeedsDatabase extends DatabaseProvider {

  val db = {
    val seedsConf = ConfigFactory.load
    Database.forConfig("postgres", seedsConf)
  }
}

object EmailHelper {
  private val Empty = ""
  private val QSR = "+qsr"
  private val NoData = "+nodata"

  val businessTypeSuffixes = Seq(Empty, QSR, NoData)

  val defaultDomain = "paytouch.io"

  def getBusinessType(email: String) =
    email match {
      case e if e.contains(QSR)    => BusinessType.Restaurant
      case e if e.contains(NoData) => BusinessType.Restaurant
      case _                       => BusinessType.Retail
    }

  def hasUserData(email: String): Boolean = !email.contains(NoData)

  def isRegularAccount(email: String) = !isSpecialAccount(email)

  def isSpecialAccount(email: String) = email.contains(QSR) || email.contains(NoData)
}
