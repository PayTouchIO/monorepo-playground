package io.paytouch.core.resources.categories

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CategoriesUpdateFSpec extends CategoriesFSpec {

  abstract class CategoriesUpdateFSpecContext extends CategoryResourceFSpecContext with Fixtures

  trait Fixtures extends MultipleLocationFixtures {
    val category = Factory.systemCategory(defaultMenuCatalog).create
    val newYork = Factory.location(merchant).create
    val categoryNewYork = Factory.categoryLocation(category, newYork).create
  }

  "POST /v1/categories.update?category_id<category-id>" in {
    "if request has valid token" in {
      "if entity is a root category (parentCategoryId=None)" in {
        "if category exists" should {
          "if category belongs to current merchant" should {
            "update category and return 200" in new CategoriesUpdateFSpecContext {
              val categoryRome = Factory.categoryLocation(category, rome).create
              val categoryUpdate = random[SystemCategoryUpdate].copy(parentCategoryId = None, position = Some(0))

              Post(s"/v1/categories.update?category_id=${category.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(categoryUpdate, category.id)
                val categoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(categoryEntity, category.id)
                assertItemLocationExists(category.id, rome.id)
              }
            }
            "update category and return 200 with image upload" in new CategoriesUpdateFSpecContext {
              val categoryRome = Factory.categoryLocation(category, rome).create
              val imageUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.Category)).create
              val categoryUpdate = random[SystemCategoryUpdate].copy(
                parentCategoryId = None,
                position = Some(0),
                imageUploadIds = Some(Seq(imageUpload.id)),
              )

              Post(s"/v1/categories.update?category_id=${category.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(categoryUpdate, category.id)
                val categoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(categoryEntity, category.id, Seq(imageUpload))
                assertItemLocationExists(category.id, rome.id)
              }
            }
            "update category with subcategories and location availabilities and return 200" in new CategoriesUpdateFSpecContext {
              val categoryRome = Factory.categoryLocation(category, rome).create
              val categoryLondon = Factory.categoryLocation(category, london).create

              val subcategory1 = Factory.systemSubcategory(defaultMenuCatalog, category).create
              val subcategory2 = Factory.systemSubcategory(defaultMenuCatalog, category).create
              val subcategory3 = Factory.systemSubcategory(defaultMenuCatalog, category).create

              val randomSubcategoryUpdates = random[SubcategoryUpsertion](2)
              val subcategoryUpdate1 = randomSubcategoryUpdates(0).copy(id = subcategory1.id, position = Some(1))
              val subcategoryUpdate2 = randomSubcategoryUpdates(1).copy(id = subcategory2.id, position = Some(2))
              val subcategoryUpdates = Seq(subcategoryUpdate1, subcategoryUpdate2)

              val categoryUpdate = random[SystemCategoryUpdate].copy(
                parentCategoryId = None,
                position = Some(0),
                subcategories = subcategoryUpdates,
                locationOverrides = Map(
                  rome.id -> Some(
                    CategoryLocationUpdate(active = Some(true), availabilities = Some(buildAvailability)),
                  ),
                  london.id -> None,
                ),
              )

              Post(s"/v1/categories.update?category_id=${category.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(categoryUpdate, category.id)
                val categoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(categoryEntity, category.id)

                categoryDao.findById(subcategory3.id).await ==== None

                assertItemLocationExists(category.id, newYork.id)
              }
            }
          }
          "if category doesn't belong to current merchant" should {
            "not update category and return 404" in new CategoriesUpdateFSpecContext {
              val competitor = Factory.merchant.create
              val competitorDefaultMenuCatalog =
                Factory.defaultMenuCatalog(competitor).create
              val competitorCategory = Factory.systemCategory(competitorDefaultMenuCatalog).create
              val categoryUpdate = random[SystemCategoryUpdate].copy(parentCategoryId = None)

              Post(s"/v1/categories.update?category_id=${competitorCategory.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)

                categoryDao.findById(competitorCategory.id).await.get ==== competitorCategory
              }
            }
          }
        }
      }
      "if entity is a sub-category" in {
        "if sub-category exists" in {
          "if parent category belongs to current merchant" should {
            "update sub-category and return 200" in new CategoriesUpdateFSpecContext {
              val subCategory = Factory.systemCategory(defaultMenuCatalog, parentCategory = Some(category)).create
              val categoryUpdate = random[SystemCategoryUpdate].copy(parentCategoryId = Some(category.id))

              Post(s"/v1/categories.update?category_id=${subCategory.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(categoryUpdate, subCategory.id)
                val categoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(categoryEntity, subCategory.id)
              }
            }
          }
          "if parent category doesn't belong to current merchant" should {
            "not update sub-category and return 404" in new CategoriesUpdateFSpecContext {
              val competitor = Factory.merchant.create
              val competitorDefaultMenuCatalog =
                Factory.defaultMenuCatalog(competitor).create
              val competitorCategory = Factory.systemCategory(competitorDefaultMenuCatalog).create
              val categoryUpdate = random[SystemCategoryUpdate].copy(parentCategoryId = Some(competitorCategory.id))

              Post(s"/v1/categories.update?category_id=${category.id}", categoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)

                categoryDao.findById(competitorCategory.id).await.get ==== competitorCategory
              }
            }
          }
        }
      }
    }
  }
}
