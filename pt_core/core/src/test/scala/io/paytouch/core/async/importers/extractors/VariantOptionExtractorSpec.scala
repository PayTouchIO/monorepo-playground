package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.data.model.VariantOptionTypeUpdate
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class VariantOptionExtractorSpec extends ExtractorSpec {

  abstract class VariantOptionExtractSpecContext extends ExtractorSpecContext with VariantOptionExtractor

  "VariantOptionExtractor" should {

    "extract variant options with no duplicated and associated to the correct variant option type" in new VariantOptionExtractSpecContext {
      val name = "T-Shirt"
      val typeName = "Colour"

      val row1 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("123456789653"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOptionType -> List(typeName),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("123456789654"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOptionType -> List(typeName),
        Keys.VariantOption -> List("Yellow"),
      )

      val template = Factory.templateProduct(merchant, name = Some(name)).get

      val variantOptionType = VariantOptionTypeUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(merchant.id),
        productId = template.id,
        name = Some(typeName),
        position = Some(0),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
      )
      val response =
        extractVariantOptions(dataWithPreParsedReferences, Seq(variantOptionType))(importRecord).await.success
      response.updates.flatMap(_.name) should containTheSameElementsAs(Seq("Blue", "Yellow"))
      response.updates.map(_.variantOptionTypeId).distinct ==== Seq(variantOptionType.id)
      response.toAdd ==== 2
      response.toUpdate ==== 0
    }

    "extract the variant option position" in new VariantOptionExtractSpecContext {
      val name = "T-Shirt"
      val typeName = "Colour"

      val row1 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc1"),
        Keys.Sku -> List("sku1"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc2"),
        Keys.Sku -> List("sku2"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName),
        Keys.VariantOption -> List("Yellow"),
      )
      val row3 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc3"),
        Keys.Sku -> List("sku3"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName),
        Keys.VariantOption -> List("Red"),
      )

      val template = Factory.templateProduct(merchant, name = Some(name)).get

      val variantOptionType = VariantOptionTypeUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(merchant.id),
        productId = template.id,
        name = Some(typeName),
        position = Some(0),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(2).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
      )
      val response =
        extractVariantOptions(dataWithPreParsedReferences, Seq(variantOptionType))(importRecord).await.success
      response.updates.map(o => (o.name.get, o.position.get)) ==== Seq(("Blue", 0), ("Yellow", 1), ("Red", 2))
    }

    "extract the variant option position with multiple options" in new VariantOptionExtractSpecContext {
      val name = "T-Shirt"
      val typeName1 = "Colour"
      val typeName2 = "Size"

      val row1 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc1"),
        Keys.Sku -> List("sku1"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName1),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc2"),
        Keys.Sku -> List("sku2"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName1, typeName2),
        Keys.VariantOption -> List("Yellow", "Big"),
      )
      val row3 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc3"),
        Keys.Sku -> List("sku3"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName1, typeName2),
        Keys.VariantOption -> List("Red", "Small"),
      )
      val row4 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("upc3"),
        Keys.Sku -> List("sku3"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("10.00"),
        Keys.VariantOptionType -> List(typeName1, typeName2),
        Keys.VariantOption -> List("Yellow", "Small"),
      )

      val template = Factory.templateProduct(merchant, name = Some(name)).get

      val variantOptionType1 = VariantOptionTypeUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(merchant.id),
        productId = template.id,
        name = Some(typeName1),
        position = Some(0),
      )

      val variantOptionType2 = VariantOptionTypeUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(merchant.id),
        productId = template.id,
        name = Some(typeName2),
        position = Some(1),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3, row4))
      val dataWithPreParsedReferences = Seq(
        dataWithLineCount(0).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(1).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(2).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
        dataWithLineCount(3).withArticleUpdate(
          ArticleUpdateWithIdentifier(
            Factory.variantProductWithTemplateId(merchant, template.id).get,
            "random-identifier",
          ),
        ),
      )
      val response =
        extractVariantOptions(dataWithPreParsedReferences, Seq(variantOptionType1, variantOptionType2))(importRecord)
          .await
          .success

      val type1Options = response.updates.filter(_.variantOptionTypeId == variantOptionType1.id)
      type1Options.map(o => (o.name.get, o.position.get)) ==== Seq(("Blue", 0), ("Yellow", 1), ("Red", 2))

      val type2Options = response.updates.filter(_.variantOptionTypeId == variantOptionType2.id)
      type2Options.map(o => (o.name.get, o.position.get)) ==== Seq(("Big", 0), ("Small", 1))
    }

    "reject extraction if variant option cannot be associate to a variant option type" in new VariantOptionExtractSpecContext {
      val name = "T-Shirt"

      val row1 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("87654321054"),
        Keys.Sku -> List("t-shirt-blue-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("12.34"),
        Keys.VariantOption -> List("Blue"),
      )
      val row2 = Map(
        Keys.ProductName -> List(name),
        Keys.Upc -> List("87654321055"),
        Keys.Sku -> List("t-shirt-yellow-sku"),
        Keys.Unit -> List("unit"),
        Keys.Price -> List("23.44"),
        Keys.VariantOption -> List("Yellow"),
      )

      val template = Factory.templateProduct(merchant, name = Some(name)).get

      val variantOptionType = VariantOptionTypeUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(merchant.id),
        productId = template.id,
        name = Some("some-type"),
        position = Some(0),
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2))
      val response =
        extractVariantOptions(dataWithLineCount, Seq(variantOptionType))(importRecord).await
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Variant name not found for variant option Blue"),
          ValidationError(Some(2), "Variant name not found for variant option Yellow"),
        ),
      )
    }
  }
}
