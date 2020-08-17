package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.data.model.BrandUpdate
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SimpleProductExtractorSpec extends ExtractorSpec {

  abstract class SimpleProductExtractSpecContext extends ExtractorSpecContext with SimpleProductExtractor

  "SimpleProductExtractor" should {

    "extract simple products" in new SimpleProductExtractSpecContext {

      val jacketName = "Jacket"
      val jacketUpc = "12345678901234"
      val jacketProduct = Factory.simpleProduct(merchant, name = Some(jacketName), upc = Some(jacketUpc)).create

      val shoesName = "Shoes"

      val brandName = "Adidas"

      val row1 = Map(
        Keys.ProductName -> List("T-Shirt"),
        Keys.Upc -> List("t-shirt-upc"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List("T-Shirt"),
        Keys.Upc -> List("896745230101234"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )
      val row3 = Map(
        Keys.ProductName -> List(jacketName),
        Keys.Upc -> List(jacketUpc),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("43.21"),
      )
      val row4 = Map(
        Keys.ProductName -> List(shoesName),
        Keys.Upc -> List("987654321012345"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("43.21"),
        Keys.Brand -> List(brandName),
      )

      val rows = List(row1, row2, row3, row4)

      val brandUpdate = random[BrandUpdate].copy(name = Some(brandName))
      val brands = Seq(brandUpdate)

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response = extractSimpleProducts(dataWithLineCount, brands)(importRecord, locations).await._1.success
      response.updates.flatMap(_.name) should containTheSameElementsAs(Seq(jacketName, shoesName))
      response.updates.flatMap(_.applyPricingToAllLocations).distinct ==== Seq(true)
      response.updates.flatMap(_.`type`).distinct ==== Seq(ArticleType.Simple)
      response.updates.find(_.name.contains(jacketName)).get.id ==== Some(jacketProduct.id)
      response.updates.find(_.name.contains(shoesName)).get.brandId ==== brandUpdate.id
      response.toAdd ==== 1
      response.toUpdate ==== 1
    }

    "reject extraction if some product data is missing or not well formed" in new SimpleProductExtractSpecContext {

      val row1 = Map(Keys.Upc -> List("a-upc"), Keys.Price -> List("non-numeric-amount"))
      val row2 = Map(Keys.Upc -> List("a-upc"), Keys.Cost -> List("non-numeric-amount"))

      val rows = List(row1, row2)
      val brands = Seq.empty[BrandUpdate]

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response = extractSimpleProducts(dataWithLineCount, brands)(importRecord, locations).await._1
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Product name is missing"),
          ValidationError(Some(1), "Price can't be alphanumeric (was non-numeric-amount)"),
          ValidationError(Some(2), "Product name is missing"),
          ValidationError(Some(2), "Price is missing"),
        ),
      )
    }

    "reject extraction if upc contains spaces" in new SimpleProductExtractSpecContext {
      val row1 = Map(
        Keys.ProductName -> List("T-Shirt Blue"),
        Keys.Upc -> List("23223 3232"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
      )
      val row2 = Map(
        Keys.ProductName -> List("T-Shirt Yellow"),
        Keys.Upc -> List("896745230101234"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
      )

      val rows = List(row1, row2)

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response = extractSimpleProducts(dataWithLineCount, Seq.empty)(importRecord, locations).await._1

      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Invalid UPC '23223 3232'. UPCs must not contain spaces."),
        ),
      )
    }

    "reject extraction if sku contains spaces" in new SimpleProductExtractSpecContext {
      val row1 = Map(
        Keys.ProductName -> List("T-Shirt Blue"),
        Keys.Upc -> List("t-shirt-upc"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
      )
      val row2 = Map(
        Keys.ProductName -> List("T-Shirt Yellow"),
        Keys.Upc -> List("896745230101234"),
        Keys.Sku -> List("2222222 444"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
      )

      val rows = List(row1, row2)

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response = extractSimpleProducts(dataWithLineCount, Seq.empty)(importRecord, locations).await._1

      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(2), "Invalid SKU '2222222 444'. SKUs must not contain spaces."),
        ),
      )
    }
  }
}
