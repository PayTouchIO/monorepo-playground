package io.paytouch.core.entities

import io.paytouch.core.data.model.enums.ArticleScope

class PartMappedToArticleSpec extends ConvertionSpec {

  "PartCreation" should {
    "convert to ArticleCreation without information loss" ! prop { part: PartCreation =>
      val article = PartCreation.convert(part)

      article.scope ==== ArticleScope.Part
      article.`type` ==== None
      article.isCombo should beFalse

      part.name ==== article.name
      part.description ==== article.description
      part.categoryIds ==== article.categoryIds
      part.brandId ==== article.brandId
      part.supplierIds ==== article.supplierIds
      part.sku ==== article.sku
      part.upc ==== article.upc
      part.cost ==== article.cost
      part.unit ==== article.unit
      part.trackInventory ==== article.trackInventory
      part.active ==== article.active
      part.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      part.variants ==== article.variants
      part.variantProducts === article.variantProducts
      part.locationOverrides.transform { (_, v) =>
        v.map(PartLocationUpdate.convert)
      } ==== article.locationOverrides
    }
  }

  "PartUpdate" should {
    "convert to ArticleUpdate without information loss" ! prop { part: PartUpdate =>
      val article = PartUpdate.convert(part)

      article.scope ==== Some(ArticleScope.Part)
      article.`type` ==== None
      article.isCombo ==== None

      part.name ==== article.name
      part.description ==== article.description
      part.categoryIds ==== article.categoryIds
      part.brandId ==== article.brandId
      part.supplierIds ==== article.supplierIds
      part.sku ==== article.sku
      part.upc ==== article.upc
      part.cost ==== article.cost
      part.unit ==== article.unit
      part.trackInventory ==== article.trackInventory
      part.active ==== article.active
      part.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      part.variants ==== article.variants
      part.variantProducts === article.variantProducts
      part.locationOverrides.transform { (_, v) =>
        v.map(PartLocationUpdate.convert)
      } ==== article.locationOverrides
      part.reason ==== article.reason
      part.notes ==== article.notes
    }
  }

}
