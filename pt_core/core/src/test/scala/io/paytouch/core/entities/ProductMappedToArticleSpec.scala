package io.paytouch.core.entities

import io.paytouch.core.data.model.enums.ArticleScope

class ProductMappedToArticleSpec extends ConvertionSpec {

  "ProductCreation" should {
    "convert to ArticleCreation without information loss" ! prop { product: ProductCreation =>
      val article = ProductCreation.convert(product)

      article.scope ==== ArticleScope.Product
      article.`type` ==== None
      article.isCombo should beFalse

      product.name ==== article.name
      product.description ==== article.description
      product.categoryIds ==== article.categoryIds
      product.brandId ==== article.brandId
      product.supplierIds ==== article.supplierIds
      product.sku ==== article.sku
      product.upc ==== article.upc
      product.cost ==== article.cost
      product.price ==== article.price
      product.unit ==== article.unit
      product.margin ==== article.margin
      product.trackInventory ==== article.trackInventory
      product.active ==== article.active
      product.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      product.discountable ==== article.discountable
      product.avatarBgColor ==== article.avatarBgColor
      product.isService ==== article.isService
      product.orderRoutingBar ==== article.orderRoutingBar
      product.orderRoutingKitchen ==== article.orderRoutingKitchen
      product.variants ==== article.variants
      product.variantProducts === article.variantProducts
      product.locationOverrides.transform { (_, v) =>
        v.map(ProductLocationUpdate.convert)
      } ==== article.locationOverrides
      product.imageUploadIds ==== article.imageUploadIds
    }
  }

  "ProductUpdate" should {
    "convert to ArticleUpdate without information loss" ! prop { product: ProductUpdate =>
      val article = ProductUpdate.convert(product)

      article.scope ==== Some(ArticleScope.Product)
      article.`type` ==== None
      article.isCombo ==== None

      product.name ==== article.name
      product.description ==== article.description
      product.categoryIds ==== article.categoryIds
      product.brandId ==== article.brandId
      product.supplierIds ==== article.supplierIds
      product.sku ==== article.sku
      product.upc ==== article.upc
      product.cost ==== article.cost
      product.price ==== article.price
      product.unit ==== article.unit
      product.margin ==== article.margin
      product.trackInventory ==== article.trackInventory
      product.active ==== article.active
      product.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      product.discountable ==== article.discountable
      product.avatarBgColor ==== article.avatarBgColor
      product.isService ==== article.isService
      product.orderRoutingBar ==== article.orderRoutingBar
      product.orderRoutingKitchen ==== article.orderRoutingKitchen
      product.variants ==== article.variants
      product.variantProducts === article.variantProducts
      product.locationOverrides.transform { (_, v) =>
        v.map(ProductLocationUpdate.convert)
      } ==== article.locationOverrides
      product.imageUploadIds ==== article.imageUploadIds
      product.reason ==== article.reason
      product.notes ==== article.notes
    }
  }

}
