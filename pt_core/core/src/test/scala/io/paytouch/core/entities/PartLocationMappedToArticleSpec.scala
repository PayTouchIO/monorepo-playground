package io.paytouch.core.entities

class PartLocationMappedToArticleSpec extends ConvertionSpec {

  "PartLocationUpdate" should {
    "convert to ArticleLocationUpdate without information loss" ! prop { partLocation: PartLocationUpdate =>
      val articleLocation = PartLocationUpdate.convert(partLocation)

      partLocation.cost ==== articleLocation.cost
      partLocation.unit ==== articleLocation.unit
      partLocation.active ==== articleLocation.active
    }
  }

}
