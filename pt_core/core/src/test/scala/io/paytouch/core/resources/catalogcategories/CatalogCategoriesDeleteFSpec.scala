package io.paytouch.core.resources.catalogcategories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CatalogCategoriesDeleteFSpec extends CatalogCategoriesFSpec {

  val catalogCategoryDao = daos.catalogCategoryDao
  val categoryAvailabilityDao = daos.categoryAvailabilityDao

  abstract class CatalogCategoriesDeleteResourceFSpecContext
      extends CatalogCategoryResourceFSpecContext
         with MultipleLocationFixtures

  "POST /v1/catalog_categories.delete" in {
    "if request has valid token" in {
      "delete catalog categories and theirs availabilities" in new CatalogCategoriesDeleteResourceFSpecContext {
        val catalogCategoryA = Factory.catalogCategory(catalog).create
        val categoryAvailabilityA = Factory.categoryAvailability(catalogCategoryA, Seq.empty).create

        val catalogCategoryB = Factory.catalogCategory(catalog).create
        val categoryAvailabilityB = Factory.categoryAvailability(catalogCategoryB, Seq.empty).create

        Post(s"/v1/catalog_categories.delete", Ids(ids = Seq(catalogCategoryA.id, catalogCategoryB.id)))
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          catalogCategoryDao.findById(catalogCategoryA.id).await must beEmpty
          categoryAvailabilityDao.findByItemId(catalogCategoryA.id).await must beEmpty
          catalogCategoryDao.findById(catalogCategoryB.id).await must beEmpty
          categoryAvailabilityDao.findByItemId(catalogCategoryB.id).await must beEmpty
        }
      }

      "do nothing if the catalog category doesn't exist" in new CatalogCategoriesDeleteResourceFSpecContext {
        val catalogCategoryId = UUID.randomUUID

        Post(s"/v1/catalog_categories.delete", Ids(ids = Seq(catalogCategoryId)))
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }

      "do not delete catalog categories that do not belong to the merchant" in new CatalogCategoriesDeleteResourceFSpecContext {
        val competitor = Factory.merchant.create
        val competitorCatalog = Factory.catalog(competitor).create
        val competitorCatalogCategory = Factory.catalogCategory(competitorCatalog).create
        val competitorCategoryAvailability = Factory.categoryAvailability(competitorCatalogCategory, Seq.empty).create

        Post(s"/v1/catalog_categories.delete", Ids(ids = Seq(competitorCatalogCategory.id)))
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          val catalogCategory = catalogCategoryDao.findById(competitorCatalogCategory.id).await
          catalogCategory ==== Some(competitorCatalogCategory)
          val catalotCategoryAvailabilities = categoryAvailabilityDao.findByItemId(competitorCatalogCategory.id).await
          catalotCategoryAvailabilities ==== Seq(competitorCategoryAvailability)
        }
      }

    }
  }
}
