package io.paytouch.core.resources.inventories

import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

abstract class GenericListInventoryFSpec extends FSpec {

  abstract class BaseProductResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    def assertInventoryMatchesExpectedValues(
        productInventory: Inventory,
        quantity: Double,
        stockValue: MonetaryAmount,
      ): Unit = {
      productInventory.totalQuantity.amount ==== quantity
      productInventory.stockValue ==== stockValue
    }
  }

  abstract class GenericListInventoryFSpecContext extends BaseProductResourceFSpecContext with Fixtures

  trait Fixtures extends MultipleLocationFixtures {
    val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
    Factory.userLocation(user, deletedLocation).create

    val tShirts = Factory.systemCategory(defaultMenuCatalog).create
    val jeans = Factory.systemCategory(defaultMenuCatalog).create

    val templateProduct = Factory.templateProduct(merchant).create
    val variantProduct = Factory.variantProduct(merchant, templateProduct).create
    val simpleProduct = Factory.simpleProduct(merchant, name = Some("d123aniel321a"), categories = Seq(tShirts)).create
    val simplePart = Factory.simplePart(merchant, name = Some("d123aniel321a"), categories = Seq(tShirts)).create
    val deletedProduct = Factory
      .simpleProduct(merchant, name = Some("d123aniel321z"), categories = Seq(tShirts), deletedAt = Some(UtcTime.now))
      .create

    val variantProductLondon =
      Factory.productLocation(variantProduct, london, costAmount = Some(2.12)).create
    val simpleProductLondon = Factory.productLocation(simpleProduct, london, costAmount = Some(3.15)).create
    val simpleProductDeletedLocation =
      Factory.productLocation(simpleProduct, deletedLocation, costAmount = Some(3.15)).create
    val simplePartLondon = Factory.productLocation(simplePart, london, costAmount = Some(4.15)).create

    Factory.stock(variantProductLondon, quantity = Some(7), minimumOnHand = Some(2)).create
    Factory.stock(simpleProductLondon, quantity = Some(3), minimumOnHand = Some(10)).create
    Factory.stock(simpleProductDeletedLocation, quantity = Some(3), minimumOnHand = Some(10)).create
    Factory.stock(simplePartLondon, quantity = Some(5), minimumOnHand = Some(20)).create

    val supplier = Factory.supplier(merchant).create
    Factory.supplierProduct(supplier, simpleProduct).create

    val orderPaid = Factory.order(merchant, location = Some(london), paymentStatus = Some(PaymentStatus.Paid)).create
    Factory
      .orderItem(
        orderPaid,
        product = Some(variantProduct),
        quantity = Some(16),
        priceAmount = Some(BigDecimal(8)),
        paymentStatus = Some(PaymentStatus.Paid),
      )
      .create
    Factory
      .orderItem(
        orderPaid,
        product = Some(simpleProduct),
        quantity = Some(32),
        priceAmount = Some(BigDecimal(4)),
        paymentStatus = Some(PaymentStatus.Paid),
      )
      .create

    val orderPending =
      Factory.order(merchant, location = Some(london), paymentStatus = Some(PaymentStatus.Pending)).create
    Factory
      .orderItem(
        orderPending,
        product = Some(variantProduct),
        quantity = Some(8),
        priceAmount = Some(BigDecimal(4)),
        paymentStatus = Some(PaymentStatus.Pending),
      )
      .create
    Factory
      .orderItem(
        orderPending,
        product = Some(simpleProduct),
        quantity = Some(4),
        priceAmount = Some(BigDecimal(32)),
        paymentStatus = Some(PaymentStatus.Pending),
      )
      .create
  }
}
