package io.paytouch.core.resources.orders

import java.time.ZonedDateTime
import java.util.UUID

import cats.implicits._

import org.scalacheck._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.entities.enums.RewardType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class OrdersListFSpec extends OrdersFSpec {
  "GET /v1/orders.list" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return a paginated list of all orders sorted by received at in descending order" in new OrderResourceFSpecContext {
          val newYork = Factory.location(merchant).create // how is this New York?

          Factory.userLocation(user, newYork).create

          val globalCustomer = Factory.globalCustomer().create
          val customer = Factory.customerMerchant(merchant, globalCustomer).create
          val orderDeliveryAddress = Factory.orderDeliveryAddress(merchant).create

          val order1 = Factory
            .order(
              merchant,
              location = Some(london),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(3)),
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(2)),
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = Some(newYork),
              customer = None,
              receivedAt = Some(UtcTime.now.minusDays(1)),
            )
            .create

          val order4 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              `type` = Some(OrderType.DeliveryRetail),
              orderDeliveryAddress = Some(orderDeliveryAddress),
              receivedAt = Some(UtcTime.now),
            )
            .create

          val discount = Factory.discount(merchant).create
          val order3Discount = Factory.orderDiscount(order3, discount).create

          Factory.orderUser(order1, user).create
          Factory.orderUser(order2, user).create
          Factory.orderUser(order3, user).create

          val taxRate = Factory.taxRate(merchant).create
          Factory.orderTaxRate(order1, taxRate).create

          val loyaltyProgram =
            Factory
              .loyaltyProgram(merchant)
              .create

          val loyaltyReward =
            Factory
              .loyaltyReward(loyaltyProgram, rewardType = Some(RewardType.FreeProduct))
              .create

          val loyaltyMembership =
            Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram)
              .create

          val rewardRedemption2 =
            Factory
              .rewardRedemption(
                loyaltyMembership,
                loyaltyReward,
                orderId = Some(order2.id),
                overrideLoyaltyRewardType = Some(RewardType.DiscountPercentage),
              )
              .create

          val rewardRedemption3 =
            Factory
              .rewardRedemption(loyaltyMembership, loyaltyReward, orderId = Some(order3.id))
              .create

          Get("/v1/orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]

            orders.data.map(_.id) ==== Seq(
              order4.id,
              order3.id,
              order2.id,
              order1.id,
            )

            assertResponse(
              order1,
              orders.data.find(_.id == order1.id).get,
              Some(london),
              Some(customer),
            )

            assertResponse(
              order2,
              orders.data.find(_.id == order2.id).get,
              Some(rome),
              Some(customer),
              rewards = Seq(rewardRedemption2),
            )

            assertResponse(
              order3,
              orders.data.find(_.id == order3.id).get,
              Some(newYork),
              None,
              discounts = Seq(order3Discount),
              rewards = Seq(rewardRedemption3),
            )

            assertResponse(
              order4,
              orders.data.find(_.id == order4.id).get,
              Some(rome),
              Some(customer),
              orderDeliveryAddress = Some(orderDeliveryAddress),
            )
          }
        }

        "return a paginated list of all orders sorted by received at in descending order that belong to non-deleted locations" in new OrderResourceFSpecContext {
          val newYork = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          Factory.userLocation(user, newYork).create

          val globalCustomer = Factory.globalCustomer().create
          val customer = Factory.customerMerchant(merchant, globalCustomer).create

          val order1 = Factory
            .order(
              merchant,
              location = Some(london),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(3)),
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(2)),
            )
            .create

          val order3 = Factory
            .order(merchant, location = Some(newYork), customer = None, receivedAt = Some(UtcTime.now.minusDays(1)))
            .create

          Factory.orderUser(order1, user).create
          Factory.orderUser(order2, user).create
          Factory.orderUser(order3, user).create

          Get("/v1/orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get, Some(london), Some(customer))
            assertResponse(order2, orders.data.find(_.id == order2.id).get, Some(rome), Some(customer))
          }
        }

        "return a paginated list of all orders sorted by received at in descending order properly paginated" in new OrderResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          Factory.userLocation(user, newYork).create

          val globalCustomer = Factory.globalCustomer().create
          val customer = Factory.customerMerchant(merchant, globalCustomer).create

          val order1 = Factory
            .order(
              merchant,
              location = Some(london),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(3)),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(2)),
            )
            .create
          val order3 = Factory
            .order(merchant, location = Some(newYork), customer = None, receivedAt = Some(UtcTime.now.minusDays(1)))
            .create

          Factory.orderUser(order1, user).create
          Factory.orderUser(order2, user).create
          Factory.orderUser(order3, user).create

          Get("/v1/orders.list?per_page=2&page=1").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order3.id, order2.id)
            assertResponse(order2, orders.data.find(_.id == order2.id).get, Some(rome), Some(customer))
            assertResponse(order3, orders.data.find(_.id == order3.id).get, Some(newYork), None)
          }
        }
      }

      "filtered by location_id" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by location id" in new OrderResourceFSpecContext {
          val globalCustomer = Factory.globalCustomer().create
          val customer = Factory.customerMerchant(merchant, globalCustomer).create

          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(3)),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer),
              receivedAt = Some(UtcTime.now.minusDays(2)),
            )
            .create
          val order3 = Factory
            .order(merchant, location = Some(london), customer = None, receivedAt = Some(UtcTime.now.minusDays(1)))
            .create

          Get(s"/v1/orders.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get, Some(rome), Some(customer))
            assertResponse(order2, orders.data.find(_.id == order2.id).get, Some(rome), Some(customer))
          }
        }
      }

      "filtered by table_id" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by table id" in new OrderResourceFSpecContext {
          val seatingGood = random[Seating].some
          val seatingBad = random[Seating].copy(tableId = UUID.randomUUID()).some

          val order1 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(3).some,
              seating = seatingGood,
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(2).some,
              seating = seatingGood,
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(1).some,
              seating = seatingBad,
            )
            .create

          val order4 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(1).some,
              seating = None,
            )
            .create

          Get(s"/v1/orders.list?table_id=${seatingGood.get.tableId}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]

            orders.data.map(_.id) ==== Seq(order2, order1).map(_.id)

            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by status[]" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by status[]" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(3).some,
              status = OrderStatus.Delivered.some,
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(2).some,
              status = OrderStatus.InBar.some,
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(1).some,
              status = OrderStatus.PickedUp.some,
            )
            .create

          val order4 = {
            val initial =
              Factory
                .order(
                  merchant,
                  location = rome.some,
                  receivedAt = UtcTime.now.minusDays(1).some,
                  status = None, // .order has a fallback
                )

            initial
              .copy(
                update = initial
                  .update
                  .copy(
                    status = None, // I really want it to be None
                  ),
              )
          }.create

          val status: String =
            Seq(order1, order2)
              .flatMap(_.status.map(_.entryName))
              .mkString(",")

          Get(s"/v1/orders.list?status[]=${status}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]

            orders.data.map(_.id) ==== Seq(order2, order1).map(_.id)

            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }

        "return a paginated list of all orders sorted by received at in descending order and filtered by status[] based on the is_open flag" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(3).some,
              status = Gen.oneOf(OrderStatus.open).sample,
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(2).some,
              status = Gen.oneOf(OrderStatus.open).sample,
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = rome.some,
              receivedAt = UtcTime.now.minusDays(1).some,
              status = Gen.oneOf(OrderStatus.notOpen).sample,
            )
            .create

          val order4 = {
            val initial =
              Factory
                .order(
                  merchant,
                  location = rome.some,
                  receivedAt = UtcTime.now.minusDays(1).some,
                  status = None, // .order has a fallback
                )

            initial
              .copy(
                update = initial
                  .update
                  .copy(
                    status = None, // I really want it to be None
                  ),
              )
          }.create

          val status: String =
            Gen
              .someOf(OrderStatus.values.map(_.entryName))
              .sample
              .toList
              .flatten
              .mkString(",")

          // status will be ignored
          Get(s"/v1/orders.list?is_open=true&status[]=${status}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]

            orders.data.map(_.id) ==== Seq(order2, order1).map(_.id)

            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }

          // status will be ignored
          Get(s"/v1/orders.list?is_open=false&status[]=${status}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]

            orders.data.map(_.id) ==== Seq(order3).map(_.id)

            assertResponse(order3, orders.data.find(_.id == order3.id).get)
          }
        }
      }

      "filtered by payment_type" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by payment type" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              paymentType = Some(OrderPaymentType.Cash),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              paymentType = Some(OrderPaymentType.Cash),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              paymentType = Some(OrderPaymentType.GiftCard),
            )
            .create

          Get(s"/v1/orders.list?payment_type=cash").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by source" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by source" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              source = Some(Source.Storefront),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              source = Some(Source.Storefront),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              source = Some(Source.Register),
            )
            .create

          Get(s"/v1/orders.list?source=storefront").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by sources or delivery providers" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by sources" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              source = Some(Source.Storefront),
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              source = Some(Source.DeliveryProvider),
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              source = Some(Source.Register),
            )
            .create

          Get(s"/v1/orders.list?source[]=storefront,delivery_provider")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }

        "return a paginated list of all orders sorted by received at in descending order and filtered by delivery providers" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              deliveryProvider = DeliveryProvider.UberEats.some,
            )
            .create

          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              deliveryProvider = DeliveryProvider.Postmates.some,
            )
            .create

          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              deliveryProvider = DeliveryProvider.DoorDash.some,
            )
            .create

          Get(s"/v1/orders.list?delivery_provider[]=uber_eats,postmates")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by acceptance status" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by acceptance status" in new OrderResourceFSpecContext {
          val onlineOrderAttribute1 =
            Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              onlineOrderAttribute = Some(onlineOrderAttribute1),
            )
            .create
          val onlineOrderAttribute2 =
            Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Accepted)).create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              onlineOrderAttribute = Some(onlineOrderAttribute2),
            )
            .create
          val onlineOrderAttribute3 =
            Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Rejected)).create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              onlineOrderAttribute = Some(onlineOrderAttribute3),
            )
            .create

          Get(s"/v1/orders.list?acceptance_status=pending").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
          }
        }
      }

      "filtered by payment status" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by payment status" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              paymentStatus = Some(PaymentStatus.Refunded),
            )
            .create

          Get(s"/v1/orders.list?payment_status=paid").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by view" should {
        "with view active" should {
          "return a paginated list of all orders sorted by received at in descending order and filtered by view" in new OrderResourceFSpecContext {
            val order1 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(3)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order2 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(2)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create
            val order3 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Ready),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order4 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Canceled),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create

            Get(s"/v1/orders.list?view=active").addHeader(authorizationHeader) ~> routes ~> check {
              val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
              orders.data.map(_.id) ==== Seq(order3.id, order2.id)
              assertResponse(order2, orders.data.find(_.id == order2.id).get)
              assertResponse(order3, orders.data.find(_.id == order3.id).get)
            }
          }
        }

        "with view completed" should {
          "return a paginated list of all orders sorted by received at in descending order and filtered by view" in new OrderResourceFSpecContext {
            val order1 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(3)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order2 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(2)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create
            val order3 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Ready),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order4 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Canceled),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create

            Get(s"/v1/orders.list?view=completed").addHeader(authorizationHeader) ~> routes ~> check {
              val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
              orders.data.map(_.id) ==== Seq(order1.id)
              assertResponse(order1, orders.data.find(_.id == order1.id).get)
            }
          }
        }

        "with view canceled" should {
          "return a paginated list of all orders sorted by received at in descending order and filtered by view" in new OrderResourceFSpecContext {
            val order1 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(3)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order2 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(2)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create
            val order3 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Ready),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            val order4 = Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Canceled),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create

            Get(s"/v1/orders.list?view=canceled").addHeader(authorizationHeader) ~> routes ~> check {
              val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
              orders.data.map(_.id) ==== Seq(order4.id)
              assertResponse(order4, orders.data.find(_.id == order4.id).get)
            }
          }
        }
      }

      "with view pending" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by view" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              status = Some(OrderStatus.Ready),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create
          val order4 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              status = Some(OrderStatus.Canceled),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create
          val order5 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(0)),
              status = Some(OrderStatus.Ready),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create
          val order6 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              status = Some(OrderStatus.Canceled),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create

          Get(s"/v1/orders.list?view=pending").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order5.id, order3.id, order2.id)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)
            assertResponse(order5, orders.data.find(_.id == order5.id).get)
          }
        }
      }

      "filtered by from date-time" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by a start date-time" in new OrderResourceFSpecContext {
          val dateTimeInRome = ZonedDateTime.parse("2015-12-03T20:15:30+01:00[Europe/Rome]")
          val dateTimeInHonolulu = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")
          val order1 = Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInRome)).create
          val order2 = Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInHonolulu)).create
          val order3 =
            Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInRome.minusDays(2))).create

          Get(s"/v1/orders.list?from=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }
      }

      "filtered by to date-time" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by an end date-time" in new OrderResourceFSpecContext {
          val dateTimeInRome = ZonedDateTime.parse("2015-12-03T20:15:30+01:00[Europe/Rome]")
          val dateTimeInHonolulu = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")
          val order1 = Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInRome)).create
          val order2 = Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInHonolulu)).create
          val order3 =
            Factory.order(merchant, location = Some(rome), receivedAt = Some(dateTimeInRome.minusDays(2))).create

          Get(s"/v1/orders.list?to=2015-12-04").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order1.id, order3.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)
          }
        }
      }

      "filtered by q" should {
        "return a list of orders filtered by customer name" in new OrderResourceFSpecContext {
          val daniela = {
            val globalCustomer =
              Factory.globalCustomer(firstName = Some("Daniela"), lastName = Some("Sfregola")).create
            Factory.customerMerchant(merchant, globalCustomer).create
          }
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              customer = Some(daniela),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create

          Get(s"/v1/orders.list?q=daniela%20sfregola").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get, customer = Some(daniela))
          }
        }

        "return a list of orders filtered by order number" in new OrderResourceFSpecContext {
          val order1 =
            Factory.order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now.minusDays(3))).create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            .create

          Get(s"/v1/orders.list?q=2").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
          }
        }

        "return a list of orders filtered by payment card last digits" in new OrderResourceFSpecContext {
          val order1 = Factory.order(merchant, location = Some(rome)).create
          val paymentDetails1 = random[PaymentDetails].copy(maskPan = Some("*****123456"), last4Digits = None)
          Factory.paymentTransaction(order1, paymentDetails = Some(paymentDetails1)).create

          val order2 = Factory.order(merchant, location = Some(rome)).create
          val paymentDetails2 = random[PaymentDetails].copy(maskPan = Some("*****456123"), last4Digits = None)
          Factory.paymentTransaction(order2, paymentDetails = Some(paymentDetails2)).create

          val order3 = Factory.order(merchant, location = Some(rome)).create
          val paymentDetails3 = random[PaymentDetails].copy(maskPan = None, last4Digits = Some("0123"))
          Factory.paymentTransaction(order3, paymentDetails = Some(paymentDetails3)).create

          val order4 = Factory.order(merchant, location = Some(rome)).create
          val paymentDetails4 = random[PaymentDetails].copy(maskPan = None, last4Digits = None)
          Factory.paymentTransaction(order4, paymentDetails = Some(paymentDetails4)).create

          Get(s"/v1/orders.list?q=123").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) should containTheSameElementsAs(Seq(order2.id, order3.id))
          }
        }
      }

      "filtered by updated_since date-time" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by updated_since date-time" in new OrderResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")
          val receivedAt = ZonedDateTime.parse("2014-12-03T20:15:30+01:00[Europe/Rome]")
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(receivedAt.plusSeconds(0)),
              overrideNow = Some(now.minusDays(1)),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(receivedAt.plusSeconds(1)),
              overrideNow = Some(now),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(receivedAt.plusSeconds(2)),
              overrideNow = Some(now.plusDays(1)),
            )
            .create

          Get(s"/v1/orders.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order3.id, order2.id)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)
          }
        }
      }

      "filtered by invoice" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by invoice" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now.minusDays(3)), isInvoice = Some(true))
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              isInvoice = Some(false),
            )
            .create
          val order3 = Factory
            .order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now.minusDays(1)), isInvoice = Some(true))
            .create

          Get(s"/v1/orders.list?invoice=true").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order3.id, order1.id)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
          }
        }
      }

      "filtered by customer_id" should {
        "return a paginated list of all orders sorted by received at in descending order and filtered by customer id" in new OrderResourceFSpecContext {
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1 = Factory.customerMerchant(merchant, globalCustomer1).create

          val globalCustomer2 = Factory.globalCustomer().create
          val customer2 = Factory.customerMerchant(merchant, globalCustomer2).create

          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer1),
              receivedAt = Some(UtcTime.now.minusDays(3)),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              customer = Some(customer2),
              receivedAt = Some(UtcTime.now.minusDays(2)),
            )
            .create
          val order3 = Factory
            .order(merchant, location = Some(london), customer = None, receivedAt = Some(UtcTime.now.minusDays(1)))
            .create

          Get(s"/v1/orders.list?customer_id=${customer1.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get, Some(rome), Some(customer1))
          }
        }
      }

      "expand payment_transactions" should {
        "return a paginated list of all orders sorted by received at in descending order with expanded payment transactions" in new OrderResourceFSpecContext {
          val order1 =
            Factory.order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now.minusDays(3))).create
          val paymentTransaction11 = Factory.paymentTransaction(order1).create
          val paymentTransaction12 = Factory.paymentTransaction(order1).create
          val order2 =
            Factory.order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now.minusDays(2))).create
          val paymentTransaction21 = Factory.paymentTransaction(order2).create

          Get(s"/v1/orders.list?expand[]=payment_transactions").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(
              order1,
              orders.data.find(_.id == order1.id).get,
              paymentTransactions = Some(Seq(paymentTransaction11, paymentTransaction12)),
            )
            assertResponse(
              order2,
              orders.data.find(_.id == order2.id).get,
              paymentTransactions = Some(Seq(paymentTransaction21)),
            )
          }
        }
      }

      "expand gift_card_passes" should {
        "return a paginated list of all orders with order items expanded gift card passes" in new OrderResourceFSpecContext {
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCard = Factory.giftCard(giftCardProduct).create

          val order1 = Factory
            .order(merchant, location = Some(rome))
            .create
          val orderItem1 = Factory.orderItem(order1, product = Some(giftCardProduct)).create
          val giftCardPass1 =
            Factory
              .giftCardPass(giftCard, orderItem1, recipientEmail = Some("foo"))
              .create

          val order2 = Factory
            .order(merchant, location = Some(rome))
            .create
          val orderItem2 = Factory.orderItem(order2, product = Some(giftCardProduct)).create
          val giftCardPass2 =
            Factory
              .giftCardPass(giftCard, orderItem2, recipientEmail = Some("bar"))
              .create

          Get(s"/v1/orders.list?expand[]=gift_card_passes")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order2.id, order1.id)
            assertResponse(
              order1,
              orders.data.find(_.id == order1.id).get,
              items = Some(Seq(orderItem1)),
              giftCardPassInfoPerItem = Some(Map(orderItem1 -> GiftCardPassInfo.fromRecord(giftCardPass1))),
            )
            assertResponse(
              order2,
              orders.data.find(_.id == order2.id).get,
              items = Some(Seq(orderItem2)),
              giftCardPassInfoPerItem = Some(Map(orderItem2 -> GiftCardPassInfo.fromRecord(giftCardPass2))),
            )
          }
        }
      }

      "expand sales_summary" should {
        "return a paginated list of all orders sorted by received at in descending order with expanded sales summary" in new OrderResourceFSpecContext {
          val order1 =
            Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(3)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
          val order2 =
            Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(2)),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.PartiallyPaid),
              )
              .create
          val order3 =
            Factory
              .order(
                merchant,
                location = Some(rome),
                receivedAt = Some(UtcTime.now.minusDays(1)),
                status = Some(OrderStatus.Canceled),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create

          Get(s"/v1/orders.list?expand[]=sales_summary").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order3.id, order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)

            val salesSummary = orders.meta.salesSummary.map(json => json.extract[SalesSummary])
            salesSummary.get ==== SalesSummary(
              totalGross = Seq(
                MonetaryAmount(
                  order1.totalAmount.getOrElse[BigDecimal](0) +
                    order2.totalAmount.getOrElse[BigDecimal](0),
                  currency,
                ),
              ),
              totalNet = Seq(
                MonetaryAmount(
                  order1.subtotalAmount.getOrElse[BigDecimal](0) +
                    order2.subtotalAmount.getOrElse[BigDecimal](0),
                  currency,
                ),
              ),
            )
          }
        }
      }

      "expand type_summary" should {
        "return a paginated list of all orders sorted by received at in descending order with expanded type summary" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              `type` = Some(OrderType.DeliveryRestaurant),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              `type` = Some(OrderType.DeliveryRestaurant),
            )
            .create
          val order3 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(1)),
              `type` = Some(OrderType.InStore),
            )
            .create
          val order4 = Factory
            .order(merchant, location = Some(rome), receivedAt = Some(UtcTime.now), `type` = Some(OrderType.TakeOut))
            .create

          Get(s"/v1/orders.list?expand[]=type_summary").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order4.id, order3.id, order2.id, order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
            assertResponse(order2, orders.data.find(_.id == order2.id).get)
            assertResponse(order3, orders.data.find(_.id == order3.id).get)
            assertResponse(order4, orders.data.find(_.id == order4.id).get)

            val typeSummary = orders.meta.typeSummary.map(json => json.extract[Seq[OrdersCountByType]])
            typeSummary.get should containTheSameElementsAs(
              Seq(
                OrdersCountByType(OrderType.DeliveryRestaurant, 2),
                OrdersCountByType(OrderType.InStore, 1),
                OrdersCountByType(OrderType.TakeOut, 1),
              ),
            )
          }
        }
      }

      "with no expansion" should {
        "should also return by default online order attribute field" in new OrderResourceFSpecContext {
          val onlineOrderAttribute = Factory.onlineOrderAttribute(merchant).create
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              `type` = Some(OrderType.DeliveryRestaurant),
              onlineOrderAttribute = Some(onlineOrderAttribute),
            )
            .create

          Get(s"/v1/orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            assertResponse(order1, orders.data.find(_.id == order1.id).get)
          }
        }
      }

      "expand summaries and filters" should {
        "return summaries respecting filters" in new OrderResourceFSpecContext {
          val order1 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(3)),
              `type` = Some(OrderType.DeliveryRestaurant),
              paymentType = Some(OrderPaymentType.GiftCard),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create
          val order2 = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(UtcTime.now.minusDays(2)),
              `type` = Some(OrderType.InStore),
              paymentType = Some(OrderPaymentType.Cash),
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create

          Get(s"/v1/orders.list?expand[]=sales_summary,type_summary&payment_type=gift_card")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orders = responseAs[ApiResponseWithMetadata[Seq[OrderEntity]]]
            orders.data.map(_.id) ==== Seq(order1.id)
            assertResponse(order1, orders.data.find(_.id == order1.id).get)

            val salesSummary = orders.meta.salesSummary.map(json => json.extract[SalesSummary])
            salesSummary.get ==== SalesSummary(
              totalGross = Seq(MonetaryAmount(order1.totalAmount.getOrElse[BigDecimal](0), currency)),
              totalNet = Seq(MonetaryAmount(order1.subtotalAmount.getOrElse[BigDecimal](0), currency)),
            )

            val typeSummary = orders.meta.typeSummary.map(json => json.extract[Seq[OrdersCountByType]])
            typeSummary.get should containTheSameElementsAs(Seq(OrdersCountByType(OrderType.DeliveryRestaurant, 1)))
          }
        }
      }
    }
  }
}
