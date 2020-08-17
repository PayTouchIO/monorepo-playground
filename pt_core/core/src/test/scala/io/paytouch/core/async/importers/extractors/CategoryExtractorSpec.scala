package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CategoryExtractorSpec extends ExtractorSpec {

  abstract class CategoryExtractSpecContext extends ExtractorSpecContext with CategoryExtractor

  "CategoryExtractor" should {

    "extract categories with no duplicates" in new CategoryExtractSpecContext {
      val mainCategory = Factory.systemCategory(defaultMenuCatalog, Some("Main")).create
      val row1 = Map(Keys.Category -> List("Main"), Keys.Subcategory -> List("Pasta"))
      val row2 = Map(Keys.Category -> List("Dessert"), Keys.Subcategory -> List("IceCream"))
      val row3 =
        Map(Keys.Category -> List("Main"), Keys.Subcategory -> List("Pasta"), Keys.Subcategory -> List("Burger"))

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractCategories(dataWithLineCount)(importRecord).await.success
      response.updates.flatMap(_.name) should containTheSameElementsAs(Seq("Main", "Dessert"))
      response.updates.find(_.name.contains("Main")).get.id ==== Some(mainCategory.id)
      response.toAdd ==== 1
      response.toUpdate ==== 1
    }

    "extract no categories if there is no category key" in new CategoryExtractSpecContext {
      val row1 = Map(Keys.Subcategory -> List("Pasta"))
      val row2 = Map(Keys.Subcategory -> List("IceCream"))
      val row3 = Map(Keys.Subcategory -> List("Pasta"), Keys.Subcategory -> List("Burger"))

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractCategories(dataWithLineCount)(importRecord).await.success
      response.updates ==== Seq.empty
      response.toAdd ==== 0
      response.toUpdate ==== 0
    }
  }
}
