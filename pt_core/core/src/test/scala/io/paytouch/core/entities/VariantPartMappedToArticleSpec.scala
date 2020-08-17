package io.paytouch.core.entities

class VariantPartMappedToArticleSpec extends ConvertionSpec {

  "VariantPartCreation" should {
    "convert to VariantArticleCreation without information loss" ! prop { variantPart: VariantPartCreation =>
      val variantArticle = VariantPartCreation.convert(variantPart)

      variantPart.sku ==== variantArticle.sku
      variantPart.upc ==== variantArticle.upc
      variantPart.cost ==== variantArticle.cost
      variantPart.unit ==== variantArticle.unit
      variantPart.applyPricingToAllLocations ==== variantArticle.applyPricingToAllLocations
      variantPart.locationOverrides.transform { (_, v) =>
        v.map(PartLocationUpdate.convert)
      } ==== variantArticle.locationOverrides
    }
  }

  "VariantPartUpdate" should {
    "convert to VariantArticleUpdate without information loss" ! prop { variantPart: VariantPartUpdate =>
      val variantArticle = VariantPartUpdate.convert(variantPart)

      variantPart.sku ==== variantArticle.sku
      variantPart.upc ==== variantArticle.upc
      variantPart.cost ==== variantArticle.cost
      variantPart.unit ==== variantArticle.unit
      variantPart.applyPricingToAllLocations ==== variantArticle.applyPricingToAllLocations
      variantPart.locationOverrides === variantArticle.locationOverrides
    }
  }

}
