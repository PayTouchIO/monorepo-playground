package io.paytouch.ordering.resources.stores

import java.util.UUID

import scala.collection.mutable

import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageType
import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.CommonArbitraries

@scala.annotation.nowarn("msg=Auto-application")
class StoresUpdateFSpec extends StoresFSpec with CommonArbitraries {
  abstract class StoreUpdateFSpecContext extends StoreResourceFSpecContext {
    implicit val uah: Authorization = userAuthorizationHeader

    val store = londonStore

    val urls = random[ImageUrls](2)
    val heroUrls = Seq(urls.head.copy(imageUploadId = UUID.randomUUID))
    val logoUrls = Seq(urls(1).copy(imageUploadId = UUID.randomUUID))

    PtCoreStubData.recordImageIds(
      mutable.Map(
        ImageType.StoreHero -> heroUrls.map(_.imageUploadId),
        ImageType.StoreLogo -> logoUrls.map(_.imageUploadId),
      ),
    )

    val update = random[StoreUpdate].copy(
      locationId = Some(newYorkId),
      catalogId = romeStore.catalogId,
      heroImageUrls = Some(heroUrls),
      logoImageUrls = Some(logoUrls),
    )
  }

  "POST /v1/stores.update?store_id=<store-id>" in {
    "if request has valid token" in {

      "if request is valid" should {
        "update a store" in new StoreUpdateFSpecContext {

          Post(s"/v1/stores.update?store_id=${store.id}", update)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Store]].data
            assertUpdate(store.id, update)
            assertResponseById(store.id, entity)
          }

        }
      }

      "if location id is invalid" should {
        "reject the request" in new StoreUpdateFSpecContext {
          val invalidId = UUID.randomUUID
          val invalidUpdate = update.copy(locationId = Some(invalidId))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if catalog id is invalid" should {
        "reject the request" in new StoreUpdateFSpecContext {
          val invalidId = UUID.randomUUID
          val invalidUpdate = update.copy(catalogId = Some(invalidId))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if hero image id is invalid" should {
        "reject the request" in new StoreUpdateFSpecContext {
          // hero urls swapped with logo urls
          val invalidUpdate = update.copy(heroImageUrls = Some(logoUrls))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if logo image id is invalid" should {
        "reject the request" in new StoreUpdateFSpecContext {
          // logo urls swapped with hero urls
          val invalidUpdate = update.copy(logoImageUrls = Some(heroUrls))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if a store is already associated to that location" should {
        "reject the request" in new StoreUpdateFSpecContext {
          val invalidUpdate = update.copy(locationId = Some(romeStore.locationId))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("InvalidStoreLocationAssociation")
          }
        }
      }

      "if the url slug is taken for that merchant" should {
        "reject the request" in new StoreUpdateFSpecContext {
          val invalidUpdate = update.copy(urlSlug = Some(romeStore.urlSlug))

          Post(s"/v1/stores.update?store_id=${store.id}", invalidUpdate)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("UrlSlugAlreadyTaken")
          }
        }
      }

      "if id does not exist" should {
        "reject the request" in new StoreUpdateFSpecContext {
          val invalidId = UUID.randomUUID

          Post(s"/v1/stores.update?store_id=$invalidId", update)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertErrorCode("NonAccessibleStoreIds")
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new StoreUpdateFSpecContext {
        Post(s"/v1/stores.update?store_id=${store.id}", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
