package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UpcExtractorSpec extends ExtractorSpec {

  abstract class UpcExtractorSpecContext extends ExtractorSpecContext with UpcExtractor {
    val productDao = daos.productDao
  }

  "UpcExtractor" should {

    "extract upcs" in new UpcExtractorSpecContext {
      val upc1 = "09876543214321"
      val upc2 = "09876543214322"
      val upc3 = "09876543214323"

      val row1 = Map(Keys.Upc -> List(upc1))
      val row2 = Map(Keys.Upc -> List(upc2))
      val row3 = Map(Keys.Upc -> List(upc3))

      val simpleProduct = Factory.simpleProduct(merchant, upc = Some(upc1)).get
      val templateProduct = Factory.templateProduct(merchant).create
      val variantProduct1 = Factory.variantProduct(merchant, templateProduct, upc = Some(upc2)).get
      val variantProduct2 = Factory.variantProduct(merchant, templateProduct, upc = Some(upc3)).get

      val products = Seq(simpleProduct, variantProduct1, variantProduct2)

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractUpcs(dataWithLineCount, products)(importRecord).await.success
      response should containTheSameElementsAs(Seq(upc1, upc2, upc3))
    }

    "reject upcs that are duplicated or already exist for other products" in new UpcExtractorSpecContext {
      val upc1 = "09876543214321"
      val upc2 = "09876543214322"
      val upc3 = "09876543214323"

      val row1 = Map(Keys.Upc -> List(upc1))
      val row2 = Map(Keys.Upc -> List(upc2))
      val row3 = Map(Keys.Upc -> List(upc1, upc3))

      Factory.simpleProduct(merchant = merchant, upc = Some(upc2)).create

      val simpleProduct = Factory.simpleProduct(merchant, upc = Some(upc1)).get
      val templateProduct = Factory.templateProduct(merchant).create
      val variantProduct1 = Factory.variantProduct(merchant, templateProduct, upc = Some(upc2)).get
      val variantProduct2 = Factory.variantProduct(merchant, templateProduct, upc = Some(upc3)).get

      val products = Seq(simpleProduct, variantProduct1, variantProduct2)

      val dataWithLineCount = buildDataWithLineCount(List(row1, row2, row3))
      val response = extractUpcs(dataWithLineCount, products)(importRecord).await
      response.failures should containTheSameElementsAs(
        Seq(
          ValidationError(Some(1), "Duplicated UPC '09876543214321' (see line 3)."),
          ValidationError(
            Some(2),
            "Another product with UPC '09876543214322' already exists. Please, provide a different UPC.",
          ),
          ValidationError(Some(3), "Duplicated UPC '09876543214321' (see line 1)."),
        ),
      )
    }
  }
}
