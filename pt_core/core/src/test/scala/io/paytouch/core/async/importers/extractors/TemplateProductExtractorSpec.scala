package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.data.model.BrandUpdate
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TemplateProductExtractorSpec extends ExtractorSpec {

  abstract class TemplateProductExtractSpecContext extends ExtractorSpecContext with TemplateProductExtractor

  "TemplateProductExtractor" should {

    "extract template products" in new TemplateProductExtractSpecContext {
      val tShirtName = "T-Shirt"
      val tieName = "Tie"

      val tShirtTemplate = Factory.templateProduct(merchant, name = Some(tShirtName)).create

      val brandName = "Adidas"

      val row1 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("12345678901234"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
        Keys.Brand -> List(brandName),
      )
      val row2 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("12345678901235"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
        Keys.StockQuantity -> List("12"),
        Keys.Brand -> List(brandName),
      )
      val row3 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Sku -> List("tie-blue-sku-2"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("72.59"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row4 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Sku -> List("tie-yellow-sku-1"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("15.04"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )
      val row5 = Map(Keys.ProductName -> List("Jacket"), Keys.Unit -> List("unit"), Keys.Price -> List("43.21"))

      val rows = List(row1, row2, row3, row4, row5)

      val brandUpdate = random[BrandUpdate].copy(name = Some(brandName))
      val brands = Seq(brandUpdate)

      val dataWithLineCount = buildDataWithLineCount(rows)
      val (updatesWithCount, updatedRows) =
        extractTemplateProducts(dataWithLineCount, brands)(importRecord, locations).await
      val response = updatesWithCount.success
      val templates = response.updates
      templates.flatMap(_.id).distinct.size ==== 2
      templates.flatMap(_.isVariantOfProductId).size ==== 0
      templates.flatMap(_.`type`).distinct ==== Seq(ArticleType.Template)
      templates.flatMap(_.applyPricingToAllLocations).distinct ==== Seq(false)
      templates.flatMap(_.name) should containTheSameElementsAs(Seq(tShirtName, tieName))
      templates.find(_.name.contains(tieName)).get.trackInventory ==== Some(false)

      val tShirtUpdate = templates.find(_.name.contains(tShirtName)).get
      tShirtUpdate.trackInventory ==== Some(true)
      tShirtUpdate.brandId ==== brandUpdate.id

      response.toAdd ==== 1
      response.toUpdate ==== 1

      templates.toSet ==== updatedRows.flatMap(_.articleUpdateByType(ArticleType.Template).map(_.update)).toSet
    }

    "reject extraction if some product data is missing or not well formed" in new TemplateProductExtractSpecContext {

      val row1 = Map(
        Keys.ProductName -> List("My first product name"),
        Keys.Upc -> List("my-upc"),
        Keys.Price -> List("non-numeric-amount"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List("My second product name"),
        Keys.Upc -> List("a-upc"),
        Keys.Cost -> List("non-numeric-amount"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )

      val rows = List(row1, row2)
      val brands = Seq.empty[BrandUpdate]

      val dataWithLineCount = buildDataWithLineCount(rows)
      val (response, _) = extractTemplateProducts(dataWithLineCount, brands)(importRecord, locations).await
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Price can't be alphanumeric (was non-numeric-amount)"),
          ValidationError(Some(2), "Price is missing"),
        ),
      )
    }
  }
}
