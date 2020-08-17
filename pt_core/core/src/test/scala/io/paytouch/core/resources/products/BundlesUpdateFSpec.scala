package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class BundlesUpdateFSpec extends BundlesFSpec {

  abstract class BundlesUpdateFSpecContext extends BundleResourceFSpecContext {
    lazy val category1 = Factory.systemCategory(defaultMenuCatalog).create
    lazy val category2 = Factory.systemCategory(defaultMenuCatalog).create
    lazy val supplier = Factory.supplier(merchant).create
    lazy val categoryIds = Seq(category1.id, category2.id)
    lazy val supplierIds = Seq(supplier.id)
    lazy val locationOverrides: Map[UUID, Option[ProductLocationUpdate]] = Map(
      rome.id -> Some(random[ProductLocationUpdate]),
    )

    lazy val productForBundleOption = Factory.simpleProduct(merchant).create
    lazy val bundle = Factory.comboProduct(merchant, categories = Seq(category1)).create
    lazy val bundleSet = Factory.bundleSet(bundle).create
    lazy val bundleOption = Factory.bundleOption(bundleSet, productForBundleOption).create
    lazy val productLocation = Factory.productLocation(bundle, rome).create

    lazy val bundleSets = Seq(
      random[BundleSetUpdate]
        .copy(
          id = bundleSet.id,
          options =
            Some(Seq(random[BundleOptionUpdate].copy(id = bundleOption.id, articleId = productForBundleOption.id))),
        ),
    )

    lazy val update = random[BundleUpdate].copy(
      categoryIds = Some(categoryIds),
      description = Some("description"),
      locationOverrides = locationOverrides,
      supplierIds = Some(supplierIds),
      bundleSets = Some(bundleSets),
      variants = None,
    )
  }

  "POST /v1/bundles.update?bundle_id=<bundle-id>" in {

    "if request has valid token" in {

      "if bundle and its relations belong to same merchant" should {

        "update bundle, its relations and return 200" in new BundlesUpdateFSpecContext {
          override lazy val locationOverrides = Map.empty

          Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, bundle.id, Some(categoryIds), Some(supplierIds))
            assertComboType(bundle.id)
          }
        }

        "update bundle and update location settings, their relations and return 200" in new BundlesUpdateFSpecContext {
          Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, bundle.id, Some(categoryIds), Some(supplierIds))
            assertComboType(bundle.id)
          }
        }

        "update bundle and update location settings, location tax rates, their relations and return 200" in new BundlesUpdateFSpecContext {
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          override lazy val locationOverrides =
            Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

          Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, bundle.id, Some(categoryIds), Some(supplierIds))
            assertComboType(bundle.id)
          }
        }

        "update bundle with no location settings" should {
          "leave the bundle location data intact and return 200" in new BundlesUpdateFSpecContext {
            override lazy val locationOverrides = Map.empty
            productLocation // trigger creation of the lazy val

            Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, bundle.id)
              assertComboType(bundle.id)

              productLocationDao.findById(productLocation.id).await.get ==== productLocation
            }
          }
        }

        "update bundle with a null location overrides" should {
          "remove the bundle location data and return 200" in new BundlesUpdateFSpecContext {
            override lazy val locationOverrides = Map(rome.id -> None)
            productLocation // trigger creation of the lazy val

            Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, bundle.id)
              assertComboType(bundle.id)

              productLocationDao.findById(productLocation.id).await ==== None
            }
          }
        }
      }

      "if bundle doesn't belong to current user's merchant" should {

        "not update bundle, its relations and return 404" in new BundlesUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorBundle = Factory.comboProduct(competitor, categories = Seq(competitorCategory1)).create

          Post(s"/v1/bundles.update?bundle_id=${competitorBundle.id}", update)
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

      "if category doesn't belong to current user's merchant" should {

        "update bundle, update own categories, skip spurious categories and return 200" in new BundlesUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create

          override lazy val categoryIds = Seq(category1.id, competitorCategory1.id)

          Post(s"/v1/bundles.update?bundle_id=${bundle.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, bundle.id, categories = Some(Seq(category1.id)))
            assertComboType(bundle.id)
          }
        }
      }

      "if a location id does not belong to the merchant" should {
        "reject the request with a 404" in new BundlesUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorBundle = Factory.comboProduct(competitor).create

          override lazy val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate]))

          Post(s"/v1/bundles.update?bundle_id=${competitorBundle.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if bundle has been deleted" should {

      "reject the request with a 404" in new BundleResourceFSpecContext {
        val deletedBundle = Factory.comboProduct(merchant, deletedAt = Some(UtcTime.now)).create
        val update = random[BundleUpdate]
        Post(s"/v1/bundles.update?bundle_id=${deletedBundle.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new BundleResourceFSpecContext {
        val newBundleId = UUID.randomUUID
        val update = random[BundleUpdate]
        Post(s"/v1/bundles.update?bundle_id=$newBundleId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
