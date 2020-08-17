package io.paytouch.core.resources

import java.time.Duration
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.services.ProductService

trait ProductResource extends JsonResource {
  def productService: ProductService

  lazy val productSpecificRoutes: Route =
    path("products.create") {
      post {
        parameters("product_id".as[UUID]) { productId =>
          entity(as[ProductCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(productService.create(productId, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("products.list_popular") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
            ) { (locationId, locationIds) =>
              authenticate { implicit user =>
                onSuccess(
                  productService.getPopularProducts(
                    locationIds.combineWithOne(locationId),
                  )(Duration.ofDays(14)),
                )((products, count) => completeAsPaginatedApiResponse(products, count))
              }
            }
          }
        }
      } ~
      path("products.update") {
        post {
          parameters("product_id".as[UUID]) { productId =>
            entity(as[ProductUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(productService.update(productId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("bundles.create") {
        post {
          parameters("bundle_id".as[UUID]) { bundleId =>
            entity(as[BundleCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(productService.create(bundleId, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("bundles.update") {
        post {
          parameters("bundle_id".as[UUID]) { bundleId =>
            entity(as[BundleUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(productService.update(bundleId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
