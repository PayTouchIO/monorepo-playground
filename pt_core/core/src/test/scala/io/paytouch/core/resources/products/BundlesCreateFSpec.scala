package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class BundlesCreateFSpec extends BundlesFSpec {

  abstract class BundlesCreateFSpecContext extends BundleResourceFSpecContext {
    lazy val productForBundleOption = Factory.simpleProduct(merchant).create
    lazy val category1 = Factory.systemCategory(defaultMenuCatalog).create
    lazy val category2 = Factory.systemCategory(defaultMenuCatalog).create
    lazy val supplier = Factory.supplier(merchant).create
    lazy val categoryIds = Seq(category1.id, category2.id)
    lazy val supplierIds = Seq(supplier.id)
    lazy val newBundleId = UUID.randomUUID
    lazy val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate]))
    lazy val bundleSets = Seq(
      random[BundleSetCreation].copy(
        id = UUID.randomUUID,
        options =
          Some(Seq(random[BundleOptionUpdate].copy(id = UUID.randomUUID, articleId = productForBundleOption.id))),
      ),
    )
    lazy val creation = random[BundleCreation].copy(
      categoryIds = categoryIds,
      locationOverrides = locationOverrides,
      supplierIds = supplierIds,
      bundleSets = bundleSets,
      variants = Seq.empty,
    )
  }

  "POST /v1/bundles.create" in {

    "if request has valid token" in {

      "if relations belong to same merchant" should {

        "create simple bundle without variants, its relations and return 201" in new BundlesCreateFSpecContext {
          override lazy val locationOverrides = Map.empty
          Post(s"/v1/bundles.create?bundle_id=$newBundleId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newBundleId, Some(categoryIds), Some(supplierIds))
            assertComboType(newBundleId)
          }
        }

        "create bundle with location settings, their relations and return 201" in new BundlesCreateFSpecContext {
          Post(s"/v1/bundles.create?bundle_id=$newBundleId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newBundleId, Some(categoryIds), Some(supplierIds))
            assertComboType(newBundleId)
          }
        }

        "create bundle with location settings, location tax rates, their relations and return 201" in new BundlesCreateFSpecContext {
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          override lazy val locationOverrides =
            Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

          Post(s"/v1/bundles.create?bundle_id=$newBundleId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newBundleId, Some(categoryIds), Some(supplierIds))
            assertComboType(newBundleId)
          }
        }
      }

      "if bundle doesn't belong to current user's merchant" should {

        "not create bundle, its relations and return 404" in new BundlesCreateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorBundle = Factory.comboProduct(competitor, categories = Seq(competitorCategory1)).create

          val bundleUpdate = random[BundleCreation].copy(categoryIds = categoryIds)

          Post(s"/v1/bundles.create?bundle_id=${competitorBundle.id}", bundleUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedBundle = productDao.findById(competitorBundle.id).await.get
            updatedBundle ==== competitorBundle

            productCategoryDao.findByProductId(competitorBundle.id).await.map(_.categoryId) ==== Seq(
              competitorCategory1.id,
            )
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new BundleResourceFSpecContext {
        val newBundleId = UUID.randomUUID
        val creation = random[BundleCreation]
        Post(s"/v1/bundles.create?bundle_id=$newBundleId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
