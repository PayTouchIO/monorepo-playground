package io.paytouch.core.async.importers.parsers

import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.expansions.ArticleExpansions
import io.paytouch.core.filters.ArticleFilters
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductImportParserFSpec extends ParserSpec {
  abstract class ProductImportParserFSpecContext extends ParserSpecContext {
    def findByArticleType(articleType: ArticleType) =
      productDao
        .findAllByArticleTypes(Seq(articleType), merchant.id)
        .await

    def assertImportWorkedAsExpected(filename: String, expectedSimpleTemplatesVariants: (Int, Int, Int)) = {
      val importRecord = Factory.`import`(location, filename = Some(filename)).create

      val (_, data) = parser.parse(importRecord).await.success
      loader.load(importRecord, data).await

      val simple = findByArticleType(ArticleType.Simple)
      simple.size ==== expectedSimpleTemplatesVariants._1
      val templates = findByArticleType(ArticleType.Template)
      templates.size ==== expectedSimpleTemplatesVariants._2
      val variants = findByArticleType(ArticleType.Variant)
      variants.size ==== expectedSimpleTemplatesVariants._3

      (simple, templates, variants)
    }

    def assertVariantWithoutUpcPrice(expectedVariantWithoutUpcPrice: BigDecimal) = {
      val variants = findByArticleType(ArticleType.Variant)
      val variantWithoutUpc = variants.filter(_.upc.isEmpty).head
      val productLocations = daos.productLocationDao.findByItemId(variantWithoutUpc.id).await
      productLocations.head.priceAmount ==== expectedVariantWithoutUpcPrice
    }
  }

  "ProductImportParser" should {
    "properly handle missing upcs" in new ProductImportParserFSpecContext {
      assertImportWorkedAsExpected(
        filename = s"$resources/imports/missing-upc.csv",
        expectedSimpleTemplatesVariants = (2, 1, 5),
      )

      assertVariantWithoutUpcPrice(128)
    }

    "properly handle missing upcs-2" in new ProductImportParserFSpecContext {
      assertImportWorkedAsExpected(
        filename = s"$resources/imports/missing-upc-2.csv",
        expectedSimpleTemplatesVariants = (2, 1, 5),
      )

      assertVariantWithoutUpcPrice(130)
    }

    "properly handle this file" in new ProductImportParserFSpecContext {
      private val filename1 = s"$resources/imports/issue-with-stocks.csv"
      assertImportWorkedAsExpected(filename = filename1, expectedSimpleTemplatesVariants = (0, 3, 28))

      val variants1 = findByArticleType(ArticleType.Variant)
      val stocksBefore = stockDao.findByProductIdsAndMerchantId(variants1.map(_.id), merchant.id).await
      stocksBefore.map(_.quantity).distinct ==== Seq(8)
    }

    "properly handle this file-2" in new ProductImportParserFSpecContext {
      private val filename2 = s"$resources/imports/issue-with-stocks-2.csv"
      assertImportWorkedAsExpected(filename = filename2, expectedSimpleTemplatesVariants = (0, 3, 28))

      val variants2 = findByArticleType(ArticleType.Variant)
      val stocksAfter = stockDao.findByProductIdsAndMerchantId(variants2.map(_.id), merchant.id).await
      stocksAfter.map(_.quantity).distinct ==== Seq(16)
    }

    "import variant options and types in the order given in the file" in new ProductImportParserFSpecContext {
      private val filename1 = s"$resources/imports/valid-product-import.csv"
      val (_, templates, _) =
        assertImportWorkedAsExpected(filename = filename1, expectedSimpleTemplatesVariants = (1, 2, 4))

      val templateRecord1 = templates.find(t => t.name == "VariantProduct").get
      val template1 =
        articleService.findById(templateRecord1.id)(ArticleExpansions.empty.copy(withVariants = true)).await.get
      template1.variants.get.length ==== 1
      val variantOption1 = template1.variants.get.head
      variantOption1.name ==== "Colour"
      variantOption1.options.length ==== 2
      variantOption1.options.map(o => (o.name, o.position)) ==== Seq(("Blue", 0), ("Yellow", 1))

      val templateRecord2 = templates.find(t => t.name == "ProductWithVariants").get
      val template2 =
        articleService.findById(templateRecord2.id)(ArticleExpansions.empty.copy(withVariants = true)).await.get
      template2.variants.get.length ==== 1
      val variantOption2 = template2.variants.get.head
      variantOption2.name ==== "Colour"
      variantOption2.options.length ==== 2
      variantOption2.options.map(o => (o.name, o.position)) ==== Seq(("Yellow", 0), ("Red", 1))
    }
  }
}
