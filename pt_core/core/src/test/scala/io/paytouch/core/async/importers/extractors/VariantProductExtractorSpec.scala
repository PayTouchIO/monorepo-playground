package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.data.model.BrandUpdate
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class VariantProductExtractorSpec extends ExtractorSpec {

  abstract class VariantProductExtractSpecContext extends ExtractorSpecContext with VariantProductExtractor

  "VariantProductExtractor" should {

    "extract variant products" in new VariantProductExtractSpecContext {

      val tShirtName = "T-Shirt"
      val tieName = "Tie"

      val tShirtTemplate = Factory.templateProduct(merchant, name = Some(tShirtName)).create
      val tShirtVariant = Factory.variantProduct(merchant, tShirtTemplate).create

      val brandName = "Adidas"

      val row1 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("98765432101235"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
        Keys.Brand -> List(brandName),
      )
      val row2 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("98765432101236"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
        Keys.Brand -> List(brandName),
      )
      val row3 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Upc -> List("98765432101237"),
        Keys.Sku -> List("tie-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("72.59"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row4 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Upc -> List("98765432101238"),
        Keys.Sku -> List("tie-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("15.04"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )
      val row5 = Map(Keys.ProductName -> List("Jacket"), Keys.Unit -> List("unit"), Keys.Price -> List("43.21"))

      val rows = List(row1, row2, row3, row4, row5)

      val brandUpdate = random[BrandUpdate].copy(name = Some(brandName))
      val brands = Seq(brandUpdate)

      val template1 = Factory.templateProduct(merchant, name = Some(tShirtName)).get.copy(id = Some(tShirtTemplate.id))
      val template2 = Factory.templateProduct(merchant, name = Some(tieName)).get

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response =
        extractVariantProducts(dataWithLineCount, Seq(template1, template2), brands)(importRecord, locations)
          .await
          ._1
          .success
      val updates = response.updates
      updates.flatMap(_.name) should containTheSameElementsAs(Seq(tShirtName, tShirtName, tieName, tieName))
      updates.flatMap(_.`type`).distinct ==== Seq(ArticleType.Variant)
      updates.flatMap(_.applyPricingToAllLocations).distinct ==== Seq(true)
      updates.filter(_.name.contains(tieName)).map(_.isVariantOfProductId).distinct ==== Seq(template2.id)

      val tShirtUpdates = updates.filter(_.name.contains(tShirtName))
      tShirtUpdates.map(_.isVariantOfProductId).distinct ==== Seq(template1.id)
      tShirtUpdates.map(_.brandId.toOption).distinct ==== Seq(brandUpdate.id)

      response.toAdd ==== 4
      response.toUpdate ==== 0
    }

    "reject extraction if some product data is missing or not well formed" in new VariantProductExtractSpecContext {

      val row1 = Map(
        Keys.Upc -> List("my-upc"),
        Keys.Price -> List("non-numeric-amount"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.Upc -> List("my-upc"),
        Keys.Cost -> List("non-numeric-amount"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )

      val rows = List(row1, row2)
      val brands = Seq.empty[BrandUpdate]

      val template = Factory.templateProduct(merchant, name = Some("my-product-name")).get

      val dataWithLineCount = buildDataWithLineCount(rows)
      val response = extractVariantProducts(dataWithLineCount, Seq(template), brands)(importRecord, locations).await._1
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Product name is missing"),
          ValidationError(Some(1), "Price can't be alphanumeric (was non-numeric-amount)"),
          ValidationError(Some(2), "Product name is missing"),
          ValidationError(Some(2), "Price is missing"),
        ),
      )
    }
  }
}
