package io.paytouch.core.resources.categories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import cats.implicits._

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CategoriesCreateFSpec extends CategoriesFSpec {
  abstract class CategoryCreateFSpecContext extends CategoryResourceFSpecContext {
    Factory.catalog(merchant, `type` = CatalogType.DefaultMenu.some).create
  }

  "POST /v1/categories.create?category_id" in {
    "if request has valid token" in {
      "if entity is a root category (parentCategoryId=None)" in {
        "if category doesn't exist" should {
          "create category and return 201" in new CategoryCreateFSpecContext {
            val newCategoryId = UUID.randomUUID
            val creation = random[SystemCategoryCreation].copy(parentCategoryId = None, position = None)

            Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val categoryEntity = responseAs[ApiResponse[Category]].data
              val newCategoryId = categoryEntity.id
              assertCreation(creationWithAppliedDefault, newCategoryId)
              assertResponse(categoryEntity, newCategoryId)
            }
          }
          "create category and return 201 with image upload" in new CategoryCreateFSpecContext {
            val newCategoryId = UUID.randomUUID
            val imageUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.Category)).create
            val creation = random[SystemCategoryCreation].copy(
              parentCategoryId = None,
              position = None,
              imageUploadIds = Seq(imageUpload.id),
            )

            Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val categoryEntity = responseAs[ApiResponse[Category]].data
              val newCategoryId = categoryEntity.id
              assertCreation(creationWithAppliedDefault, newCategoryId)
              assertResponse(categoryEntity, newCategoryId, Seq(imageUpload))
            }
          }
          "create category with location overrides and return 201" in new CategoryCreateFSpecContext {
            val newCategoryId = UUID.randomUUID
            val creation = random[SystemCategoryCreation].copy(
              parentCategoryId = None,
              position = None,
              locationOverrides =
                Map(rome.id -> Some(CategoryLocationUpdate(active = Some(true), availabilities = None))),
            )

            Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val categoryEntity = responseAs[ApiResponse[Category]].data
              val newCategoryId = categoryEntity.id
              assertCreation(creationWithAppliedDefault, newCategoryId)
              assertResponse(categoryEntity, newCategoryId)
              assertItemLocationExists(newCategoryId, rome.id)
            }
          }
          "create category with subcategories and return 201" in new CategoryCreateFSpecContext {
            val randomSubcreations = random[SubcategoryUpsertion](2)
            val subcreation1 = randomSubcreations(0).copy(id = UUID.randomUUID, position = Some(1))
            val subcreation2 = randomSubcreations(1).copy(id = UUID.randomUUID, position = Some(2))
            val subcreations = Seq(subcreation1, subcreation2)

            val newCategoryId = UUID.randomUUID
            val creation =
              random[SystemCategoryCreation].copy(
                parentCategoryId = None,
                position = None,
                subcategories = subcreations,
              )

            Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val categoryEntity = responseAs[ApiResponse[Category]].data
              val newCategoryId = categoryEntity.id
              assertCreation(creationWithAppliedDefault, newCategoryId)
              assertResponse(categoryEntity, newCategoryId)
            }
          }
          "create category with location availabilities and return 201" in new CategoryCreateFSpecContext {
            val newCategoryId = UUID.randomUUID
            val creation = random[SystemCategoryCreation].copy(
              parentCategoryId = None,
              position = None,
              locationOverrides =
                Map(rome.id -> Some(CategoryLocationUpdate(active = None, availabilities = Some(buildAvailability)))),
            )

            Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val categoryEntity = responseAs[ApiResponse[Category]].data
              val newCategoryId = categoryEntity.id
              assertCreation(creationWithAppliedDefault, newCategoryId)
              assertResponse(categoryEntity, newCategoryId)
              assertItemLocationExists(newCategoryId, rome.id)
            }
          }
          "if location for a location availability is not accessible" should {
            "reject the request" in new CategoryCreateFSpecContext {
              val newYork = Factory.location(merchant).create

              val newCategoryId = UUID.randomUUID
              val creation = random[SystemCategoryCreation].copy(
                parentCategoryId = None,
                position = None,
                locationOverrides = Map(
                  newYork.id -> Some(CategoryLocationUpdate(active = None, availabilities = Some(buildAvailability))),
                ),
              )

              Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }

          "if location for a location override is not accessible" should {
            "reject the request" in new CategoryCreateFSpecContext {
              val newYork = Factory.location(merchant).create

              val newCategoryId = UUID.randomUUID
              val creation = random[SystemCategoryCreation].copy(
                parentCategoryId = None,
                position = None,
                locationOverrides =
                  Map(newYork.id -> Some(CategoryLocationUpdate(active = None, availabilities = None))),
              )

              Post(s"/v1/categories.create?category_id=$newCategoryId", creation)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }
        }
      }
    }
  }
}
