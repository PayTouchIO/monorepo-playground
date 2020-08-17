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
import io.paytouch.ordering.utils.{ CommonArbitraries, FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class StoresCreateFSpec extends StoresFSpec with CommonArbitraries {
  abstract class StoreCreateFSpecContext extends StoreResourceFSpecContext {
    val merchantDao = daos.merchantDao

    implicit val uah: Authorization = userAuthorizationHeader

    val urls = random[ImageUrls](2)
    val heroUrls = Seq(urls.head.copy(imageUploadId = UUID.randomUUID))
    val logoUrls = Seq(urls(1).copy(imageUploadId = UUID.randomUUID))

    PtCoreStubData.recordImageIds(
      mutable.Map(
        ImageType.StoreHero -> heroUrls.map(_.imageUploadId),
        ImageType.StoreLogo -> logoUrls.map(_.imageUploadId),
      ),
    )

    val id = UUID.randomUUID
    val creation = random[StoreCreation].copy(
      locationId = newYorkId,
      catalogId = romeStore.catalogId,
      heroImageUrls = heroUrls,
      logoImageUrls = logoUrls,
    )
  }

  "POST /v1/stores.create?store_id=<store-id>" in {
    "if request has valid token" in {

      "if request is valid" should {
        "create a store" in new StoreCreateFSpecContext {

          Post(s"/v1/stores.create?store_id=$id", creation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)

            val entity = responseAs[ApiResponse[Store]].data
            assertCreation(id, creation)
            assertResponseById(id, entity)
          }

        }
      }

      "if location id is invalid" should {
        "reject the request" in new StoreCreateFSpecContext {
          val invalidId = UUID.randomUUID
          val invalidCreation = creation.copy(locationId = invalidId)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if catalog id is invalid" should {
        "reject the request" in new StoreCreateFSpecContext {
          val invalidId = UUID.randomUUID
          val invalidCreation = creation.copy(catalogId = invalidId)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if hero image id is invalid" should {
        "reject the request" in new StoreCreateFSpecContext {
          // hero urls swapped with logo urls
          val invalidCreation = creation.copy(heroImageUrls = logoUrls)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if logo image id is invalid" should {
        "reject the request" in new StoreCreateFSpecContext {
          // logo urls swapped with hero urls
          val invalidCreation = creation.copy(logoImageUrls = heroUrls)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)

            assertErrorCode("InternalServerError")
          }
        }
      }

      "if a store is already associated to that location" should {
        "reject the request" in new StoreCreateFSpecContext {
          val invalidCreation = creation.copy(locationId = londonId)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("InvalidStoreLocationAssociation")
          }
        }
      }

      "if the url slug is taken for that merchant" should {
        "reject the request" in new StoreCreateFSpecContext {
          val invalidCreation = creation.copy(urlSlug = londonStore.urlSlug)

          Post(s"/v1/stores.create?store_id=$id", invalidCreation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("UrlSlugAlreadyTaken")
          }
        }
      }

      "if merchant slug does not exist" should {
        "reject the request" in new StoreCreateFSpecContext {

          storeDao.deleteByIds(stores.map(_.id)).await
          merchantDao.deleteById(merchant.id).await

          Post(s"/v1/stores.create?store_id=$id", creation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertErrorCode("NonAccessibleMerchantIds")
          }
        }
      }

      "if id is already taken" should {
        "reject the request" in new StoreCreateFSpecContext {
          Post(s"/v1/stores.create?store_id=${londonStore.id}", creation)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertErrorCode("InvalidStoreIds")
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new StoreCreateFSpecContext {
        Post(s"/v1/stores.create?store_id=$id", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
