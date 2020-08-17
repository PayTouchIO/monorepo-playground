package io.paytouch.core.entities

class VariantProductMappedToArticleSpec extends ConvertionSpec {

  "VariantProductCreation" should {
    "convert to VariantArticleCreation without information loss" ! prop { variantProduct: VariantProductCreation =>
      val variantArticle = VariantProductCreation.convert(variantProduct)

      variantProduct.sku ==== variantArticle.sku
      variantProduct.upc ==== variantArticle.upc
      variantProduct.cost ==== variantArticle.cost
      variantProduct.price ==== variantArticle.price
      variantProduct.unit ==== variantArticle.unit
      variantProduct.margin ==== variantArticle.margin
      variantProduct.applyPricingToAllLocations ==== variantArticle.applyPricingToAllLocations
      variantProduct.discountable ==== variantArticle.discountable
      variantProduct.avatarBgColor ==== variantArticle.avatarBgColor
      variantProduct.isService ==== variantArticle.isService
      variantProduct.orderRoutingBar ==== variantArticle.orderRoutingBar
      variantProduct.orderRoutingKitchen ==== variantArticle.orderRoutingKitchen
      variantProduct
        .locationOverrides
        .transform { (_, v) =>
          v.map(ProductLocationUpdate.convert)
        } ==== variantArticle.locationOverrides
    }
  }

  "VariantProductUpdate" should {
    "convert to VariantArticleUpdate without information loss" ! prop { variantProduct: VariantProductUpdate =>
      val variantArticle = VariantProductUpdate.convert(variantProduct)

      variantProduct.sku ==== variantArticle.sku
      variantProduct.upc ==== variantArticle.upc
      variantProduct.cost ==== variantArticle.cost
      variantProduct.price ==== variantArticle.price
      variantProduct.unit ==== variantArticle.unit
      variantProduct.margin ==== variantArticle.margin
      variantProduct.applyPricingToAllLocations ==== variantArticle.applyPricingToAllLocations
      variantProduct.discountable ==== variantArticle.discountable
      variantProduct.avatarBgColor ==== variantArticle.avatarBgColor
      variantProduct.isService ==== variantArticle.isService
      variantProduct.orderRoutingBar ==== variantArticle.orderRoutingBar
      variantProduct.orderRoutingKitchen ==== variantArticle.orderRoutingKitchen
      variantProduct
        .locationOverrides
        .transform { (_, v) =>
          v.map(ProductLocationUpdate.convert)
        } ==== variantArticle.locationOverrides
    }
  }
}
