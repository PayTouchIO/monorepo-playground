package io.paytouch.ordering.resources.stores

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.entities.UpdateActiveItem
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class StoresUpdateActiveFSpec extends StoresFSpec {

  "POST /v1/stores.update_active" in {
    "if request has valid token" in {
      "if stores belong to the user locations" should {

        "disable/enable stores " in new StoreResourceFSpecContext {
          val storeA =
            Factory.store(merchant, catalogId = newYorkCatalogId, locationId = newYorkId, active = Some(true)).create
          val storeB = Factory
            .store(merchant, catalogId = sanFranciscoCatalogId, locationId = sanFranciscoId, active = Some(false))
            .create
          val storeC =
            Factory.store(merchant, catalogId = torontoCatalogId, locationId = torontoId, active = Some(true)).create
          val storeD =
            Factory.store(merchant, catalogId = parisCatalogId, locationId = parisId, active = Some(false)).create

          val updates = Seq(
            UpdateActiveItem(storeA.id, false),
            UpdateActiveItem(storeB.id, false),
            UpdateActiveItem(storeC.id, true),
            UpdateActiveItem(storeD.id, true),
          )

          Post(s"/v1/stores.update_active", updates)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            val ids = Seq(storeA.id, storeB.id, storeC.id, storeD.id)

            afterAWhile {
              val records = storeDao.findByIds(ids).await

              records.find(_.id == storeA.id).get.active ==== false
              records.find(_.id == storeB.id).get.active ==== false
              records.find(_.id == storeC.id).get.active ==== true
              records.find(_.id == storeD.id).get.active ==== true
            }
          }
        }
      }

      "if store doesn't belong to the user location" should {
        "not update the gift card and return 404" in new StoreResourceFSpecContext {
          val updates = Seq(UpdateActiveItem(competitorStore.id, false))

          Post(s"/v1/stores.update_active", updates)
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val record = storeDao.findById(competitorStore.id).await.get
            record.active ==== true
          }
        }
      }

    }

    "if request has an invalid token" in {

      "reject the request" in new StoreResourceFSpecContext {
        val updates = Seq.empty[UpdateActiveItem]
        Post(s"/v1/stores.update_active", updates).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
