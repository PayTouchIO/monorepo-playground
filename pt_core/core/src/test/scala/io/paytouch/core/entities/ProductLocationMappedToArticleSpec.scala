package io.paytouch.core.entities

class ProductLocationMappedToArticleSpec extends ConvertionSpec {

  "ProductLocationUpdate" should {
    "convert to ArticleLocationUpdate without information loss" ! prop { productLocation: ProductLocationUpdate =>
      val articleLocation = ProductLocationUpdate.convert(productLocation)

      productLocation.price ==== articleLocation.price
      productLocation.cost ==== articleLocation.cost
      productLocation.unit ==== articleLocation.unit
      productLocation.margin ==== articleLocation.margin
      productLocation.active ==== articleLocation.active
      productLocation.taxRateIds ==== articleLocation.taxRateIds
    }
  }

}
