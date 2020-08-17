package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class VariantOptionTypeExtractorSpec extends ExtractorSpec {

  abstract class VariantOptionTypeExtractSpecContext extends ExtractorSpecContext with VariantOptionTypeExtractor

  "VariantOptionTypeExtractor" should {

    "extract variant option types without duplicating existing types" in new VariantOptionTypeExtractSpecContext {
      val tShirtName = "t-shirt-name"
      val tieName = "tie-name"

      val row1 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("09876543210987"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("09876543210988"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )
      val row3 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Upc -> List("09876543210981"),
        Keys.Sku -> List("tie-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("72.59"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row4 = Map(
        Keys.ProductName -> List(tieName),
        Keys.Upc -> List("09876543210982"),
        Keys.Sku -> List("tie-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("15.04"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )
      val row5 = Map(
        Keys.ProductName -> List("Jacket"),
        Keys.Upc -> List("098765432109333"),
        Keys.Sku -> List("jacket"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("43.21"),
      )

      val tShirtTemplate = Factory.templateProduct(merchant, name = Some(tShirtName)).create
      val tShirtVariantOptionType =
        Factory.variantOptionType(tShirtTemplate, name = Some("Colour")).create
      val tieTemplate = Factory.templateProduct(merchant, name = Some(tieName)).get

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(Factory.variantProduct(merchant, tShirtTemplate).get, "random-identifier"),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(Factory.variantProduct(merchant, tShirtTemplate).get, "random-identifier"),
        ),
        dataWithLineCount(2).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, tieTemplate.id).get,
            "random-identifier",
          ),
        ),
      )
      val response = extractVariantOptionTypes(dataWithPreParsedReferences)(importRecord).await.success
      response.updates.flatMap(_.name) should containTheSameElementsAs(Seq("Colour", "Colour"))
      val extractedVariantOptionType =
        response.updates.find(vot => vot.name.contains("Colour") && vot.productId.contains(tShirtTemplate.id)).get
      extractedVariantOptionType.id ==== Some(tShirtVariantOptionType.id)
      response.toAdd ==== 1
      response.toUpdate ==== 1
    }

    "extract variant option type position" in new VariantOptionTypeExtractSpecContext {
      val tShirtName = "t-shirt-name"

      val tShirtTemplate = Factory.templateProduct(merchant, name = Some(tShirtName)).get

      val row1 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("09876543210987"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("09876543210988"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Size", "Colour"),
        Keys.VariantOption -> List("Large", "Yellow"),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, tShirtTemplate.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, tShirtTemplate.id).get,
            "random-identifier",
          ),
        ),
      )
      val response = extractVariantOptionTypes(dataWithPreParsedReferences)(importRecord).await.success
      response.updates.map(t => t.name.get) ==== Seq("Colour", "Size")

      val colourType = response.updates.find(_.name == Some("Colour")).get
      colourType.position ==== Some(0)

      val sizeType = response.updates.find(_.name == Some("Size")).get
      sizeType.position ==== Some(1)
    }

    "extract variant option type position when the same type is used for different products" in new VariantOptionTypeExtractSpecContext {
      val tShirtName = "t-shirt-name"
      val jeansName = "jeans-name"

      val tShirtTemplate = Factory.templateProduct(merchant, name = Some(tShirtName)).get
      val jeansTemplate = Factory.templateProduct(merchant, name = Some(jeansName)).get

      val row1 = Map(
        Keys.ProductName -> List(tShirtName),
        Keys.Upc -> List("09876543210987"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(jeansName),
        Keys.Upc -> List("09876543210988"),
        Keys.Sku -> List("jeans-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List("Colour"),
        Keys.VariantOption -> List("Yellow"),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, tShirtTemplate.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, jeansTemplate.id).get,
            "random-identifier",
          ),
        ),
      )
      val response = extractVariantOptionTypes(dataWithPreParsedReferences)(importRecord).await.success
      response.updates.map(t => (t.name.get, t.position.get, t.productId)) ==== Seq(
        ("Colour", 0, tShirtTemplate.id),
        ("Colour", 0, jeansTemplate.id),
      )
    }

    "extract no variant option types if there is no variant option type key" in new VariantOptionTypeExtractSpecContext {
      val row1 = Map(Keys.ProductName -> List("Jacket"), Keys.Unit -> List("unit"), Keys.Price -> List("43.21"))

      val template = Factory.templateProduct(merchant, name = Some("a-product-name")).get

      val dataWithLineCount = buildDataWithLineCount(List(row1))
      val response = extractVariantOptionTypes(dataWithLineCount)(importRecord).await.success
      response.updates ==== Seq.empty
      response.toAdd ==== 0
      response.toUpdate ==== 0
    }
  }
}
