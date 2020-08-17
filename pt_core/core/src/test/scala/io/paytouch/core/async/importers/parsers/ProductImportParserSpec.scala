package io.paytouch.core.async.importers.parsers

import scala.concurrent._
import java.util.UUID

import io.paytouch.core.async.importers.ProductImportData
import io.paytouch.core.data.model.VariantOptionTypeUpdate
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ProductImportParserSpec extends ParserSpec {
  abstract class ProductImportParserSpecContext extends ParserSpecContext {
    def parse(filename: String): Future[MultipleExtraction.ErrorsOr[(ProductImportResult, ProductImportData)]] = {
      val importRecord = Factory.`import`(location, filename = Some(filename)).create
      parser.parse(importRecord)
    }
  }

  "brands" should {
    "parse new brands from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.brandsToAdd ==== 4
      result.brandsToUpdate ==== 0

      data.brands.map(b => b.name.get).sorted ==== Seq("Barilla", "Burger King", "Donught", "McDonald")
    }
  }

  "categories" should {
    "parse new categories from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.categoriesToAdd ==== 2
      result.categoriesToUpdate ==== 0

      data.categories.map(b => b.name.get).sorted ==== Seq("Desert", "Food")
    }
  }

  "subcategories" should {
    "parse new subcategories from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.subcategoriesToAdd ==== 3
      result.subcategoriesToUpdate ==== 0

      data.subcategories.map(b => b.name.get).sorted ==== Seq("Burger", "IceScream", "Pasta")
    }

    "assign subcategories to categories" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val foodCat = data.categories.find(c => c.name.get == "Food").get
      val desertCat = data.categories.find(c => c.name.get == "Desert").get

      val burgerSub = data.subcategories.find(c => c.name.get == "Burger").get
      burgerSub.parentCategoryId ==== foodCat.id

      val iceScreamSub = data.subcategories.find(c => c.name.get == "IceScream").get
      iceScreamSub.parentCategoryId ==== desertCat.id

      val pastaSub = data.subcategories.find(c => c.name.get == "Pasta").get
      pastaSub.parentCategoryId ==== foodCat.id
    }
  }

  "simple products" should {
    "parse new products from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.simpleProductsToAdd ==== 1
      result.simpleProductsToUpdate ==== 0

      data.simpleProducts.map(p => p.name.get).sorted ==== Seq("SimpleProduct")
    }

    "assign products to brands" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val burgerKingBrand = data.brands.find(c => c.name.get == "Burger King").get

      val simpleProduct = data.simpleProducts.find(p => p.name.get == "SimpleProduct").get
      simpleProduct.brandId ==== burgerKingBrand.id
    }
  }

  "templates products" should {
    "parse new products from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.templateProductsToAdd ==== 2
      result.templateProductsToUpdate ==== 0

      data.templateProducts.map(p => p.name.get).sorted ==== Seq("ProductWithVariants", "VariantProduct")
    }
  }

  "variant products" should {
    "parse new products from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      result.variantProductsToAdd ==== 4
      result.variantProductsToUpdate ==== 0

      data.variantProducts.map(p => p.name.get).sorted ==== Seq(
        "ProductWithVariants",
        "ProductWithVariants",
        "VariantProduct",
        "VariantProduct",
      )
    }

    "assign variants to template products" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val template1 = data.templateProducts.find(p => p.name.get == "ProductWithVariants").get
      val template2 = data.templateProducts.find(p => p.name.get == "VariantProduct").get

      val variant1 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(21.34)).get // row 5
      variant1.isVariantOfProductId ==== template1.id

      val variant2 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(65.44)).get // row 6
      variant2.isVariantOfProductId ==== template1.id

      val variant3 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(12.34)).get // row 2
      variant3.isVariantOfProductId ==== template2.id

      val variant4 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(55.44)).get // row 3
      variant4.isVariantOfProductId ==== template2.id
    }

    "assign variants to brands" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val barillaBrand = data.brands.find(c => c.name.get == "Barilla").get
      val donughtBrand = data.brands.find(c => c.name.get == "Donught").get
      val mcDonaldBrand = data.brands.find(c => c.name.get == "McDonald").get

      val variant1 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(21.34)).get // row 5
      variant1.brandId ==== mcDonaldBrand.id

      val variant2 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(65.44)).get // row 6
      variant2.brandId ==== donughtBrand.id

      val variant3 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(12.34)).get // row 2
      variant3.brandId ==== barillaBrand.id

      val variant4 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(55.44)).get // row 3
      variant4.brandId ==== barillaBrand.id
    }
  }

  "product locations" should {
    "parse product locations from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success
      data.productLocations.length ==== 7
    }

    "assign simple products to locations" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val simpleProduct = data.simpleProducts.find(p => p.name.get == "SimpleProduct").get

      val productLocation = data.productLocations.find(l => l.productId == simpleProduct.id).get
      productLocation.locationId.get ==== location.id
    }

    "assign template products to locations" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val template1 = data.templateProducts.find(p => p.name.get == "ProductWithVariants").get
      val template2 = data.templateProducts.find(p => p.name.get == "VariantProduct").get

      val productLocation1 = data.productLocations.find(l => l.productId == template1.id).get
      productLocation1.locationId.get ==== location.id

      val productLocation2 = data.productLocations.find(l => l.productId == template2.id).get
      productLocation2.locationId.get ==== location.id
    }

    "assign variants to locations" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success

      val variant1 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(21.34)).get // row 5
      val variant2 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(65.44)).get // row 6
      val variant3 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(12.34)).get // row 2
      val variant4 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(55.44)).get // row 3

      val productLocation1 = data.productLocations.find(l => l.productId == variant1.id).get
      productLocation1.locationId.get ==== location.id

      val productLocation2 = data.productLocations.find(l => l.productId == variant2.id).get
      productLocation2.locationId.get ==== location.id

      val productLocation3 = data.productLocations.find(l => l.productId == variant3.id).get
      productLocation3.locationId.get ==== location.id

      val productLocation4 = data.productLocations.find(l => l.productId == variant4.id).get
      productLocation4.locationId.get ==== location.id
    }
  }

  "kitchens" should {
    "assign simple products to kitchens" in new ProductImportParserSpecContext {
      val bar = Factory.kitchen(location, name = Some("Bar")).create
      val kitchen = Factory.kitchen(location, name = Some("Kitchen")).create
      val grill = Factory.kitchen(location, name = Some("Grill")).create

      val (result, data) = parse(s"$resources/imports/kitchens-import.csv").await.success

      val simpleProduct = data.simpleProducts.find(p => p.name.get == "SimpleProduct").get

      val productLocation = data.productLocations.find(l => l.productId == simpleProduct.id).get
      productLocation.routeToKitchenId.get ==== kitchen.id
    }

    "assign variants to kitchens" in new ProductImportParserSpecContext {
      val bar = Factory.kitchen(location, name = Some("Bar")).create
      val kitchen = Factory.kitchen(location, name = Some("Kitchen")).create
      val grill = Factory.kitchen(location, name = Some("Grill")).create

      val (result, data) = parse(s"$resources/imports/kitchens-import.csv").await.success

      val variant1 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(21.34)).get // row 5
      val variant2 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(65.44)).get // row 6
      val variant3 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(12.34)).get // row 2
      val variant4 = data.variantProducts.find(p => p.priceAmount.get == BigDecimal(55.44)).get // row 3

      val productLocation1 = data.productLocations.find(l => l.productId == variant1.id).get
      productLocation1.routeToKitchenId.get ==== bar.id

      val productLocation2 = data.productLocations.find(l => l.productId == variant2.id).get
      productLocation2.routeToKitchenId.get ==== kitchen.id

      val productLocation3 = data.productLocations.find(l => l.productId == variant3.id).get
      productLocation3.routeToKitchenId.get ==== grill.id

      val productLocation4 = data.productLocations.find(l => l.productId == variant4.id).get
      productLocation4.routeToKitchenId ==== None
    }

    "reject import when the kitchen cannot be found" in new ProductImportParserSpecContext {
      parse(s"$resources/imports/kitchens-import.csv").await.failures
    }
  }

  "variant options" should {
    "parse variant option types from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success
      result.variantOptionTypesToAdd ==== 2
      result.variantOptionTypesToUpdate ==== 0

      val template1 = data.templateProducts.find(p => p.name.get == "VariantProduct").get
      val template2 = data.templateProducts.find(p => p.name.get == "ProductWithVariants").get

      data.variantOptionTypes.map(t => (t.name.get, t.productId)) ==== Seq(
        ("Colour", template1.id),
        ("Colour", template2.id),
      )
    }

    "parse variant options from CSV" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/valid-product-import.csv").await.success
      result.variantOptionsToAdd ==== 4
      result.variantOptionsToUpdate ==== 0

      data.variantOptions.map(o => (o.name.get, o.position.get)) ==== Seq(
        ("Blue", 0),
        ("Yellow", 1),
        ("Yellow", 0),
        ("Red", 1),
      )
    }
  }

  "merchant Piwis" should {
    "should parse simplified piwis properly" in new ProductImportParserSpecContext {
      val (result, data) = parse(s"$resources/imports/merchant-piwis-simplified.csv").await.success

      data.variantOptionTypes.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("Pieces", 0),
        ("Wing Size", 1),
      )
      data.variantOptions.map(t => (t.name.get, t.position.get)).toSet ==== Set(
        ("4 Pieces", 0),
        ("6 Pieces", 1),
        ("10 Pieces", 2),
        ("20 Pieces", 3),
        ("30 Pieces", 4),
        ("Regular", 0),
        ("Large", 1),
      )
    }

    "should parse piwis properly" in new ProductImportParserSpecContext {
      import io.paytouch.implicits._

      val (result, data) = parse(s"$resources/imports/merchant-piwis.csv").await.success
      def getVariantOptionTypes(id: Option[UUID]) = data.variantOptionTypes.filter(_.productId == id)
      def getVariantOptions(optionTypes: Seq[VariantOptionTypeUpdate]) =
        data.variantOptions.filter(vo => optionTypes.map(_.id).contains(vo.variantOptionTypeId))

      val buffaloTenders = data.templateProducts.find(p => p.name.get == "Buffalo Tenders").get
      val boneInBuffaloWings = data.templateProducts.find(p => p.name.get == "Bone-In Buffalo Wings").get
      val cajunWings = data.templateProducts.find(p => p.name.get == "Cajun Wings").get
      val BBQChickenPizza = data.templateProducts.find(p => p.name.get == "BBQ Chicken Pizza").get

      val buffaloTenderOptionTypes = getVariantOptionTypes(buffaloTenders.id)
      val buffaloTenderOptions = getVariantOptions(buffaloTenderOptionTypes)

      val boneInBuffaloWingsOptionTypes = getVariantOptionTypes(boneInBuffaloWings.id)
      val boneInBuffaloWingsOptions = getVariantOptions(buffaloTenderOptionTypes)

      val cajunWingsOptionTypes = getVariantOptionTypes(cajunWings.id)
      val cajunWingsOptions = getVariantOptions(cajunWingsOptionTypes)

      val BBQChickenPizzaOptionTypes = getVariantOptionTypes(BBQChickenPizza.id)
      val BBQChickenPizzaOptions = getVariantOptions(BBQChickenPizzaOptionTypes)

      buffaloTenderOptionTypes.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("Size", 0),
      )

      buffaloTenderOptions
        .map(t => (t.name.get, t.position.get)) ==== Seq(
        ("3 Pieces", 0),
        ("6 Pieces", 1),
      )

      boneInBuffaloWingsOptionTypes.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("Size", 0),
      )

      boneInBuffaloWingsOptions
        .map(t => (t.name.get, t.position.get)) ==== Seq(
        ("3 Pieces", 0),
        ("6 Pieces", 1),
      )

      cajunWingsOptionTypes.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("Pieces", 0),
        ("Wing Size", 1),
      )

      cajunWingsOptions.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("4 Pieces", 0),
        ("6 Pieces", 1),
        ("10 Pieces", 2),
        ("20 Pieces", 3),
        ("30 Pieces", 4),
        ("Regular", 0),
        ("Large", 1),
      )

      BBQChickenPizzaOptionTypes.map(t => (t.name.get, t.position.get)) ==== Seq(
        ("Size", 0),
      )

      BBQChickenPizzaOptions
        .map(t => (t.name.get, t.position.get)) ==== Seq(
        ("10\" Pizza", 0),
        ("12\" Pizza", 1),
        ("14\" Pizza", 2),
        ("16\" Pizza", 3),
      )
    }
  }
}
