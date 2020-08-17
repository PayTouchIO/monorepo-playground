package io.paytouch.core.resources.categories

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CategoriesDeleteFSpec extends FSpec {

  val categoryDao = daos.categoryDao
  val categoryLocationAvailabilityDao = daos.categoryLocationAvailabilityDao

  abstract class CategoryResourceFSpecContext extends FSpecContext with MultipleLocationFixtures

  "POST /v1/categories.delete" in {
    "if request has valid token" in {
      "delete a category and its subcategories" in new CategoryResourceFSpecContext {
        val category = Factory.systemCategory(defaultMenuCatalog).create
        Factory.systemSubcategory(defaultMenuCatalog, category).create
        Factory.systemSubcategory(defaultMenuCatalog, category).create
        Factory.systemSubcategory(defaultMenuCatalog, category).create
        Factory.systemSubcategory(defaultMenuCatalog, category).create

        val categoryLocation = Factory.categoryLocation(category, rome).create
        Factory.categoryLocationAvailability(categoryLocation, Seq.empty).create

        Post(s"/v1/categories.delete", Ids(ids = Seq(category.id))).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          categoryDao.findById(category.id).await must beEmpty
          categoryDao.findByParentId(category.id).await must beEmpty
          categoryLocationAvailabilityDao.findByItemId(categoryLocation.id).await must beEmpty
        }
      }
    }
  }
}
