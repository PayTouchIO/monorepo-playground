package io.paytouch.core.resources.transferorders

import java.time.LocalDateTime

import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ PaginatedApiResponse, TransferOrder => TransferOrderEntity }
import io.paytouch.core.utils.{ Formatters, UtcTime, FixtureDaoFactory => Factory }

class TransferOrdersListFSpec extends TransferOrdersFSpec {

  trait TransferOrdersListFSpecContext extends TransferOrderResourceFSpecContext {
    val now = UtcTime.ofLocalDateTime(LocalDateTime.parse("2017-06-19T16:32:31"))
    val yesterday = now.minusDays(1)
  }

  "GET /v1/transfer_orders.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all transfer orders" in new TransferOrdersListFSpecContext {
          val transferOrder1 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(1))).create
          val transferOrder2 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(2))).create
          val transferOrder3 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(3))).create
          val transferOrder4 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(4))).create

          Get("/v1/transfer_orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]]
            entities.data.map(_.id) ==== Seq(transferOrder1.id, transferOrder2.id, transferOrder3.id, transferOrder4.id)
            assertResponse(entities.data.find(_.id == transferOrder1.id).get, transferOrder1)
            assertResponse(entities.data.find(_.id == transferOrder2.id).get, transferOrder2)
            assertResponse(entities.data.find(_.id == transferOrder3.id).get, transferOrder3)
            assertResponse(entities.data.find(_.id == transferOrder4.id).get, transferOrder4)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of all transfer orders filtered by location_id (applied on from_location)" in new TransferOrdersListFSpecContext {
          val transferOrder1 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(1))).create
          val transferOrder2 = Factory.transferOrder(rome, london, user, overrideNow = Some(now.plusDays(2))).create
          val transferOrder3 = Factory.transferOrder(london, rome, user, overrideNow = Some(now.plusDays(3))).create
          val transferOrder4 = Factory.transferOrder(london, rome, user, overrideNow = Some(now.plusDays(4))).create

          Get(s"/v1/transfer_orders.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]]
            entities.data.map(_.id) ==== Seq(transferOrder1.id, transferOrder2.id)
            assertResponse(entities.data.find(_.id == transferOrder1.id).get, transferOrder1)
            assertResponse(entities.data.find(_.id == transferOrder2.id).get, transferOrder2)
          }
        }
      }

      "with from filter" should {
        "return a paginated list of all transfer orders filtered by from date" in new TransferOrdersListFSpecContext {
          val yesterdayAsString = Formatters.LocalDateFormatter.format(yesterday.toLocalDate)

          val transferOrder1 =
            Factory.transferOrder(rome, london, user, overrideNow = Some(yesterday.minusDays(1))).createAndReload
          val transferOrder2 =
            Factory.transferOrder(rome, london, user, overrideNow = Some(yesterday.plusDays(1))).createAndReload

          Get(s"/v1/transfer_orders.list?from=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]]
            entities.data.map(_.id) ==== Seq(transferOrder2.id)
            assertResponse(entities.data.find(_.id == transferOrder2.id).get, transferOrder2)
          }
        }
      }

      "with to filter" should {
        "return a paginated list of all transfer orders filtered by to date" in new TransferOrdersListFSpecContext {
          val yesterdayAsString = Formatters.LocalDateFormatter.format(yesterday.toLocalDate)

          val transferOrder1 =
            Factory.transferOrder(rome, london, user, overrideNow = Some(yesterday.minusDays(1))).createAndReload
          val transferOrder2 =
            Factory.transferOrder(rome, london, user, overrideNow = Some(yesterday.plusDays(1))).createAndReload

          Get(s"/v1/transfer_orders.list?to=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]]
            entities.data.map(_.id) ==== Seq(transferOrder1.id)
            assertResponse(entities.data.find(_.id == transferOrder1.id).get, transferOrder1)
          }
        }
      }

      "with q filter" should {
        "return a paginated list of all transfer orders filtered by a given query on transfer order number" in new TransferOrdersListFSpecContext {
          val transferOrder1 = Factory.transferOrder(rome, london, user).create
          val transferOrder2 = Factory.transferOrder(rome, london, user).create

          Get(s"/v1/transfer_orders.list?q=2").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder2.id)
            assertResponse(entities.find(_.id == transferOrder2.id).get, transferOrder2)
          }
        }
      }

      "with view filter" should {
        "return a paginated list of all transfer orders filtered by a given a view=assigned" in new TransferOrdersListFSpecContext {
          val transferOrder1 =
            Factory.transferOrder(rome, london, user, status = Some(ReceivingObjectStatus.Completed)).create
          val transferOrder2 =
            Factory.transferOrder(rome, london, user, status = Some(ReceivingObjectStatus.Created)).create
          val transferOrder3 =
            Factory.transferOrder(rome, london, user, status = Some(ReceivingObjectStatus.Receiving)).create
          val transferOrder4 =
            Factory.transferOrder(rome, london, user, status = Some(ReceivingObjectStatus.Partial)).create

          Get(s"/v1/transfer_orders.list?view=complete").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder1.id)
            assertResponse(entities.find(_.id == transferOrder1.id).get, transferOrder1)
          }
        }

        "return a paginated list of all transfer orders filtered by a given a view=incomplete" in new TransferOrdersListFSpecContext {
          val transferOrder1 =
            Factory
              .transferOrder(
                rome,
                london,
                user,
                status = Some(ReceivingObjectStatus.Completed),
                overrideNow = Some(now.plusDays(1)),
              )
              .create
          val transferOrder2 =
            Factory
              .transferOrder(
                rome,
                london,
                user,
                status = Some(ReceivingObjectStatus.Created),
                overrideNow = Some(now.plusDays(2)),
              )
              .create
          val transferOrder3 =
            Factory
              .transferOrder(
                rome,
                london,
                user,
                status = Some(ReceivingObjectStatus.Receiving),
                overrideNow = Some(now.plusDays(3)),
              )
              .create
          val transferOrder4 =
            Factory
              .transferOrder(
                rome,
                london,
                user,
                status = Some(ReceivingObjectStatus.Partial),
                overrideNow = Some(now.plusDays(4)),
              )
              .create

          Get(s"/v1/transfer_orders.list?view=incomplete").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id).sorted ==== Seq(transferOrder2.id, transferOrder3.id, transferOrder4.id).sorted
            assertResponse(entities.find(_.id == transferOrder2.id).get, transferOrder2)
            assertResponse(entities.find(_.id == transferOrder3.id).get, transferOrder3)
            assertResponse(entities.find(_.id == transferOrder4.id).get, transferOrder4)
          }
        }
      }

      "with expand[]=from_location" should {
        "return transfer orders" in new TransferOrdersListFSpecContext {
          val transferOrder = Factory.transferOrder(rome, london, user).create

          Get(s"/v1/transfer_orders.list?expand[]=from_location").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder.id)
            assertResponse(entities.find(_.id == transferOrder.id).get, transferOrder, fromLocation = Some(rome))
          }
        }
      }

      "with expand[]=to_location" should {
        "return transfer orders" in new TransferOrdersListFSpecContext {
          val transferOrder = Factory.transferOrder(rome, london, user).create

          Get(s"/v1/transfer_orders.list?expand[]=to_location").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder.id)
            assertResponse(entities.find(_.id == transferOrder.id).get, transferOrder, toLocation = Some(london))
          }
        }
      }

      "with expand[]=user" should {
        "return transfer orders" in new TransferOrdersListFSpecContext {
          val transferOrder = Factory.transferOrder(rome, london, user).create

          Get(s"/v1/transfer_orders.list?expand[]=user").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder.id)
            assertResponse(entities.find(_.id == transferOrder.id).get, transferOrder, user = Some(user))
          }
        }
      }

      "with expand[]=products_count" should {
        "return transfer orders" in new TransferOrdersListFSpecContext {
          val blueShirt = Factory.simpleProduct(merchant).create
          val yellowShirt = Factory.simpleProduct(merchant).create

          val transferOrder = Factory.transferOrder(rome, london, user).create
          Factory.transferOrderProduct(transferOrder, blueShirt, quantity = Some(3)).create
          Factory.transferOrderProduct(transferOrder, yellowShirt, quantity = Some(7)).create

          Get(s"/v1/transfer_orders.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder.id)
            assertResponse(entities.find(_.id == transferOrder.id).get, transferOrder, productsCount = Some(10))
          }
        }
      }

      "with expand[]=stock_value" should {
        "return transfer orders" in new TransferOrdersListFSpecContext {
          val blueShirt = Factory.simpleProduct(merchant).create
          Factory.productLocation(blueShirt, rome, costAmount = Some(3)).create
          Factory.productLocation(blueShirt, london, costAmount = Some(2)).create

          val yellowShirt = Factory.simpleProduct(merchant).create
          Factory.productLocation(yellowShirt, rome, costAmount = Some(2)).create
          Factory.productLocation(yellowShirt, london, costAmount = Some(1)).create

          val transferOrder = Factory.transferOrder(rome, london, user).create
          Factory.transferOrderProduct(transferOrder, blueShirt, quantity = Some(3)).create
          Factory.transferOrderProduct(transferOrder, yellowShirt, quantity = Some(7)).create

          Get(s"/v1/transfer_orders.list?expand[]=stock_value").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[TransferOrderEntity]]].data
            entities.map(_.id) ==== Seq(transferOrder.id)
            assertResponse(entities.find(_.id == transferOrder.id).get, transferOrder, stockValue = Some(13.$$$))
          }
        }
      }
    }
  }

}
