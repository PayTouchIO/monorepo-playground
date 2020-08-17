package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.data.model.CategoryUpdate
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SubcategoryExtractorSpec extends ExtractorSpec {

  abstract class SubcategoryExtractSpecContext
      extends ExtractorSpecContext
         with CategoryExtractor
         with SubcategoryExtractor

  "SubcategoryExtractor" should {

    "extract subcategories with no duplicated and associated to the correct parent" in new SubcategoryExtractSpecContext {
      val mainCategory = Factory.systemCategory(defaultMenuCatalog, Some("Main")).create
      val iceCreamCategory = Factory.systemCategory(defaultMenuCatalog, Some("IceCream")).create
      val row1 = Map(Keys.Category -> List("Main"), Keys.Subcategory -> List("Pasta"))
      val row2 = Map(Keys.Category -> List("Dessert"), Keys.Subcategory -> List("IceCream"))
      val row3 =
        Map(Keys.Category -> List("Main"), Keys.Subcategory -> List("Pasta"), Keys.Subcategory -> List("Burger"))

      val category1 = CategoryUpdate(
        id = Some(mainCategory.id),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Main"),
        parentCategoryId = None,
      )
      val category2 = CategoryUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Dessert"),
        parentCategoryId = None,
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractSubcategories(dataWithLineCount, Seq(category1, category2))(importRecord).await.success
      response.updates.flatMap(_.name) should containTheSameElementsAs(Seq("Pasta", "IceCream", "Burger"))
      response.updates.find(_.name.contains("Pasta")).get.parentCategoryId ==== category1.id
      response.updates.find(_.name.contains("IceCream")).get.id ==== Some(iceCreamCategory.id)
      response.updates.find(_.name.contains("IceCream")).get.parentCategoryId ==== category2.id
      response.updates.find(_.name.contains("Burger")).get.parentCategoryId ==== category1.id
      response.toAdd ==== 2
      response.toUpdate ==== 1
    }

    "extract no subcategories if there is no subcategory key" in new SubcategoryExtractSpecContext {
      val row1 = Map(Keys.Category -> List("Main"))
      val row2 = Map(Keys.Category -> List("Dessert"))
      val row3 = Map(Keys.Category -> List("Main"))

      val category1 = CategoryUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Main"),
        parentCategoryId = None,
      )
      val category2 = CategoryUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Dessert"),
        parentCategoryId = None,
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractSubcategories(dataWithLineCount, Seq(category1, category2))(importRecord).await.success
      response.updates.flatMap(_.name) ==== Seq.empty
      response.toAdd ==== 0
      response.toUpdate ==== 0
    }

    "reject extraction if subcategory cannot be associate to a main category" in new SubcategoryExtractSpecContext {
      val row1 = Map(Keys.Subcategory -> List("Pasta"))
      val row2 = Map(Keys.Subcategory -> List("IceCream"))
      val row3 = Map(Keys.Subcategory -> List("Pasta"), Keys.Subcategory -> List("Burger"))

      val category1 = CategoryUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Main"),
        parentCategoryId = None,
      )
      val category2 = CategoryUpdate(
        id = Some(UUID.randomUUID),
        merchantId = Some(importRecord.merchantId),
        catalogId = None,
        name = Some("Dessert"),
        parentCategoryId = None,
      )

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractSubcategories(dataWithLineCount, Seq(category1, category2))(importRecord).await
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(line = Some(1), "Category not found for sub category Pasta"),
          ValidationError(line = Some(2), "Category not found for sub category IceCream"),
          ValidationError(line = Some(3), "Category not found for sub category Burger"),
        ),
      )
    }
  }
}
