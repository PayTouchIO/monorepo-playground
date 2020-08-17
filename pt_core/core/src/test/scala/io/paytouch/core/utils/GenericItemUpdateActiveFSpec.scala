package io.paytouch.core.utils

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model._
import io.paytouch.core.entities.UpdateActiveLocation
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

import scala.concurrent._

abstract class GenericItemUpdateActiveFSpec[I <: SlickRecord, R <: SlickToggleableRecord with SlickItemLocationRecord]
    extends FSpec {

  def namespace: String

  def singular: String

  def parameter = s"${singular}_id"

  def finder(id: UUID): Future[Option[R]]

  def itemFinder(id: UUID): Future[Option[I]]

  def itemFactory(merchant: MerchantRecord): I

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: I,
      location: LocationRecord,
      active: Option[Boolean],
    ): R

  abstract class ItemResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val locationDao = daos.locationDao

    val paris = Factory.location(merchant).create
    val berlin = Factory.location(merchant).create

    def assertItemLocationWasUpdated(itemLocation: R, expectedState: Boolean) = {
      val updatedItemLocation = finder(itemLocation.id).await.get
      updatedItemLocation.active ==== expectedState
      if (itemLocation.active != expectedState)
        updatedItemLocation.updatedAt !=== itemLocation.updatedAt
      ok
    }
  }

  s"POST /v1/$namespace.update_active?$parameter=<$parameter>" in {
    "if request has valid token" in {
      s"enables a $singular at specific locations" in new ItemResourceFSpecContext {
        val item = itemFactory(merchant)
        val itemLondon = itemLocationFactory(merchant, item, london, active = Some(true))
        val itemRome = itemLocationFactory(merchant, item, rome, active = Some(false))
        val itemParis = itemLocationFactory(merchant, item, paris, active = Some(true))
        val itemBerlin = itemLocationFactory(merchant, item, berlin, active = Some(false))

        val itemLocationUpdate = Seq(
          UpdateActiveLocation(london.id, active = false),
          UpdateActiveLocation(rome.id, active = true),
        )

        Post(s"/v1/$namespace.update_active?$parameter=${item.id}", itemLocationUpdate)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          afterAWhile {
            // changes are reflected
            assertItemLocationWasUpdated(itemLondon, expectedState = false)
            assertItemLocationWasUpdated(itemRome, expectedState = true)
            // data for locations not sent is untouched
            assertItemLocationWasUpdated(itemParis, expectedState = true)
            assertItemLocationWasUpdated(itemBerlin, expectedState = false)
          }

          val updatedItem = itemFinder(item.id).await.get
          item.updatedAt !=== updatedItem.updatedAt

          val changedLocationIds = Seq(london.id, rome.id)
          val updatedLocations = locationDao.findByIds(changedLocationIds).await
          london.updatedAt !=== updatedLocations.find(_.id == london.id).get.updatedAt
          rome.updatedAt !=== updatedLocations.find(_.id == rome.id).get.updatedAt
        }
      }
    }
  }
}
