package io.paytouch.core.entities

class CatalogCategoryMappedToCategorySpec extends ConvertionSpec {

  "CatalogCategoryCreation" should {
    "convert to CategoryCreation without information loss" ! prop { catalogCategory: CatalogCategoryCreation =>
      val category = CatalogCategoryCreation.convert(catalogCategory)

      category.name ==== catalogCategory.name
      category.description ==== catalogCategory.description
      category.avatarBgColor ==== None
      category.imageUploadIds ==== Seq.empty
      category.position ==== catalogCategory.position
      category.catalogId ==== Some(catalogCategory.catalogId)
      category.parentCategoryId ==== None
      category.subcategories ==== Seq.empty
      category.locationOverrides ==== Map.empty
    }
  }

  "CatalogCategoryUpdate" should {
    "convert to CategoryUpdate without information loss" ! prop { catalogCategory: CatalogCategoryUpdate =>
      val category = CatalogCategoryUpdate.convert(catalogCategory)

      category.name ==== catalogCategory.name
      category.description ==== catalogCategory.description
      category.avatarBgColor ==== None
      category.imageUploadIds ==== None
      category.position ==== catalogCategory.position
      category.catalogId ==== catalogCategory.catalogId
      category.parentCategoryId ==== None
      category.subcategories ==== Seq.empty
      category.locationOverrides ==== Map.empty
    }
  }

}
