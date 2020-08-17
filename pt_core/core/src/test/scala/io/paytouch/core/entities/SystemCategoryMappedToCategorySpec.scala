package io.paytouch.core.entities

class SystemCategoryMappedToCategorySpec extends ConvertionSpec {

  "SystemCategoryCreation" should {
    "convert to CategoryCreation without information loss" ! prop { systemCategory: SystemCategoryCreation =>
      val category = SystemCategoryCreation.convert(systemCategory)

      category.name ==== systemCategory.name
      category.description ==== systemCategory.description
      category.avatarBgColor ==== systemCategory.avatarBgColor
      category.imageUploadIds ==== systemCategory.imageUploadIds
      category.position ==== systemCategory.position
      category.catalogId ==== None
      category.parentCategoryId ==== systemCategory.parentCategoryId
      category.subcategories ==== systemCategory.subcategories
      category.locationOverrides ==== systemCategory.locationOverrides
    }
  }

  "SystemCategoryUpdate" should {
    "convert to CategoryUpdate without information loss" ! prop { systemCategory: SystemCategoryUpdate =>
      val category = SystemCategoryUpdate.convert(systemCategory)

      category.name ==== systemCategory.name
      category.description ==== systemCategory.description
      category.avatarBgColor ==== systemCategory.avatarBgColor
      category.imageUploadIds ==== systemCategory.imageUploadIds
      category.position ==== systemCategory.position
      category.parentCategoryId ==== systemCategory.parentCategoryId
      category.catalogId ==== None
      category.subcategories ==== systemCategory.subcategories
      category.locationOverrides ==== systemCategory.locationOverrides
    }
  }

}
