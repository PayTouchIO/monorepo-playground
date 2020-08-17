package io.paytouch.seeds

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.paytouch.core.data.model.enums.BusinessType

object IdsProvider {
  import SeedsQuantityProvider._

  val namesAndDomains = Seq(
    ("Carlo", "Mallone", None),
    ("Gabriele", "Salvini", None),
    ("Andrey", "Konenko", None),
    ("Ted", "Kaminski", None),
    ("Kostyantyn", "Danylchenko", None),
    ("Daniela", "Sfregola", None),
    ("Francesco", "Levorato", None),
    ("Marco", "Campana", None),
    ("David", "Bozin", None),
    ("Brian", "Plemons", None),
    ("Michael", "Mendlowitz", None),
    ("Vish", "Talreja", None),
    ("Nisha", "Talreja", Some("xiimo.com")),
  )

  val users = namesAndDomains.cartesian(EmailHelper.businessTypeSuffixes).map {
    case ((first, last, domain), businessTypeSuffix) =>
      val emailDomain = domain.getOrElse(EmailHelper.defaultDomain)
      val email = s"${first.toLowerCase}$businessTypeSuffix@$emailDomain"
      (first, last, email)
  }

  val emails = users.map { case (_, _, email) => email }

  val merchantIdPerEmail: Map[String, UUID] = asMapPerEmail(e => s"Merchant $e".toUUID)

  val businessTypePerEmail: Map[String, BusinessType] = asMapPerEmail(EmailHelper.getBusinessType)

  val userIdPerEmail: Map[String, UUID] = asMapPerEmail(e => e.toUUID)

  val adminIdPerEmail: Map[String, UUID] = asMapPerEmail(e => s"Admin $e".toUUID)

  val brandIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotBrands).map(idx => s"Brand $idx $e".toUUID))

  val cashDrawerIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotCashDrawers).map(idx => s"CashDrawer $idx $e".toUUID))

  val categoryIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotCategories).map(idx => s"Category $idx $e".toUUID))

  val customersPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotCustomers).map(idx => s"Customer $idx $e".toUUID))

  val discountIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotDiscounts).map(idx => s"Discount $idx $e".toUUID))

  val employeeIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotUsers).map(idx => s"Employee $idx $e".toUUID))

  val groupIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotGroups).map(idx => s"Group $idx $e".toUUID))

  val locationIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotLocations).map(idx => s"Location $idx $e".toUUID))

  val loyaltyProgramIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotLoyaltyPrograms).map(idx => s"Loyalty Program $idx $e".toUUID))

  val modifierSetIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotModifierSets).map(idx => s"Modifier Set $idx $e".toUUID))

  val orderIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotOrders).map(idx => s"Order $idx $e".toUUID))

  val orderFeedbackIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotOrderFeedbacks).map(idx => s"Order $idx $e".toUUID))

  val paymentTransactionIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotPaymentTransactions).map(idx => s"Payment Transaction $idx $e".toUUID))

  val simpleProductIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotSimpleProducts).map(idx => s"Simple Product $idx $e".toUUID))

  val supplierIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotSuppliers).map(idx => s"Supplier $idx $e".toUUID))

  val taxRateIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotTaxRates).map(idx => s"Tax Rate $idx $e".toUUID))

  val templateProductIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotTemplateProducts).map(idx => s"Template Product $idx $e".toUUID))

  val ticketIdsPerEmail: Map[String, Seq[UUID]] =
    asMapPerEmail(e => (1 to TotTickets).map(idx => s"Ticket $idx $e".toUUID))

  private def asMapPerEmail[V](f: String => V): Map[String, V] =
    emails.map(email => (email, f(email))).toMap
}

object SeedsQuantityProvider {

  private val config = ConfigFactory.load

  val TotBrands = config.getInt("tot.brands")

  val TotCashDrawers = config.getInt("tot.cashDrawers")
  val TotCategories = config.getInt("tot.categories")
  val TotCategoriesWithSubcategories = config.getInt("tot.categoriesWithSubcategories")
  val SubcategoriesPerCategory = config.getInt("tot.subcategoriesPerCategory")

  val TotCustomers = config.getInt("tot.customers")
  val TotDiscounts = config.getInt("tot.discounts")
  val TotUsers = config.getInt("tot.users")
  val TotGroups = config.getInt("tot.groups")
  val TotLocations = config.getInt("tot.locations")
  val TotLoyaltyPrograms = config.getInt("tot.loyaltyPrograms")
  val TotModifierSets = config.getInt("tot.modifierSets")
  val TotOrders = config.getInt("tot.orders")
  val TotOrderFeedbacks = config.getInt("tot.orderFeedbacks")
  val TotPaymentTransactions = config.getInt("tot.paymentTransactions")
  val TotTickets = config.getInt("tot.tickets")

  val TotSimpleProducts = config.getInt("tot.simpleProducts")
  val TotTemplateProducts = config.getInt("tot.templateProducts")

  val TotSuppliers = config.getInt("tot.suppliers")
  val TotTaxRates = config.getInt("tot.taxRates")

  val AvailabilitiesPerCategoryLocation = config.getInt("tot.availabilitiesPerCategoryLocation")
  val AvailabilitiesPerDiscount = config.getInt("tot.availabilitiesPerDiscount")

  val ProductsWithHistory = config.getInt("tot.productsWithHistory")
  val ChangesPerProduct = config.getInt("tot.changesPerProduct")

  val CustomersPerGroup = config.getInt("tot.customersPerGroup")
  val CustomersPerMerchant = config.getInt("tot.customersPerMerchant")

  val ItemsPerOrder = config.getInt("tot.itemsPerOrder")
  val ItemsWithDiscount = config.getInt("tot.itemsWithDiscount")
  val ItemsWithDiscountManual = config.getInt("tot.itemsWithDiscountManual")
  val ItemsWithModifierOption = config.getInt("tot.itemsWithModifierOption")

  val LocationsPerCategory = config.getInt("tot.locationsPerCategory")
  val LocationsPerCustomer = config.getInt("tot.locationsPerCustomer")
  val LocationsPerDiscount = config.getInt("tot.locationsPerDiscount")
  val LocationsPerModifierSet = config.getInt("tot.locationsPerModifierSet")
  val LocationsPerSupplier = config.getInt("tot.locationsPerSupplier")
  val LocationsPerTaxRate = config.getInt("tot.locationsPerTaxRate")
  val LocationsPerUser = config.getInt("tot.locationsPerUser")

  val OrdersWithExtraUsers = config.getInt("tot.ordersWithExtraUsers")
  val OrdersWithNoCustomer = config.getInt("tot.ordersWithNoCustomer")
  val OptionsPerModifierSet = config.getInt("tot.optionsPerModifierSet")

  val ProductsWithCategory = config.getInt("tot.productsWithCategory")
  val ProductsPerTaxRate = config.getInt("tot.productsPerTaxRate")
  val ProductsWithSuppliers = config.getInt("tot.productsWithSuppliers")
  val ProductsWithModifierSets = config.getInt("tot.productsWithModifierSets")

  val ShiftsPerEmployee = config.getInt("tot.shiftsPerEmployee")
  val TimeCardsPerShift = config.getInt("tot.timeCardsPerShift")
  val TimeOffCardsPerEmployee = config.getInt("tot.timeOffCardsPerEmployee")

  val VariantOptionTypePerTemplate = config.getInt("tot.variantOptionTypesPerTemplate")
  val VariantOptionsPerVariantType = config.getInt("tot.variantOptionsPerVariantType")
}
