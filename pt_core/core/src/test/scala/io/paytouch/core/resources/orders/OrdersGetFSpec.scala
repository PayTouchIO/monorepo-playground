package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.StatusTransition
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.entities.enums.{ LoyaltyProgramType, TicketStatus }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class OrdersGetFSpec extends OrdersFSpec {
  "GET /v1/orders.get?order_id=$" in {
    "if request has valid token" in {
      "if the order belongs to the merchant" should {
        "with no parameters" in {
          "return an order with order items" in new OrderResourceFSpecContext {
            val kitchen = Factory.kitchenKitchen(london).create
            val bar = Factory.kitchenBar(london).create

            val order = Factory.order(merchant, Some(london)).create
            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create

            val ticket1 =
              Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create
            val ticket2 =
              Factory.ticket(order, status = Some(TicketStatus.Completed), routeToKitchenId = bar.id).create
            Factory.ticketOrderItem(ticket1, orderItem1).create
            Factory.ticketOrderItem(ticket2, orderItem2).create

            Factory.orderUser(order, user).create

            val items = Seq(orderItem1, orderItem2)
            val orderStatusPerItem =
              Map(orderItem1 -> Some(OrderRoutingStatus.New), orderItem2 -> Some(OrderRoutingStatus.Completed))
            val orderRoutingStatuses =
              OrderRoutingStatusesByType(
                KitchenType.Bar -> Some(OrderRoutingStatus.Completed),
                KitchenType.Kitchen -> Some(OrderRoutingStatus.New),
              )

            val bundle = Factory.comboProduct(merchant).create
            val bundleSet = Factory.bundleSet(bundle).create
            val bundleOption = Factory.bundleOption(bundleSet, bundle).create
            val orderBundle =
              Factory.orderBundle(order, orderItem1, orderItem2, Some(bundleSet), Some(bundleOption)).create

            Get(s"/v1/orders.get?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                items = Some(items),
                orderRoutingStatuses = Some(orderRoutingStatuses),
                orderStatusPerItem = Some(orderStatusPerItem),
              )
            }
          }

          "return an order with delivery address" in new OrderResourceFSpecContext {
            val orderDeliveryAddress = Factory.orderDeliveryAddress(merchant).create
            val order = Factory
              .order(
                merchant,
                Some(london),
                `type` = Some(OrderType.DeliveryRetail),
                orderDeliveryAddress = Some(orderDeliveryAddress),
              )
              .create

            Get(s"/v1/orders.get?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                orderDeliveryAddress = Some(orderDeliveryAddress),
              )
            }
          }

          "return an order with online order attribute even if it is cloaked" in new OrderResourceFSpecContext {
            val onlineOrderAttribute =
              Factory
                .onlineOrderAttribute(merchant, Some(AcceptanceStatus.Open))
                .create

            val order =
              Factory
                .order(
                  merchant,
                  Some(london),
                  `type` = Some(OrderType.DeliveryRetail),
                  onlineOrderAttribute = Some(onlineOrderAttribute),
                )
                .create

            Get(s"/v1/orders.get?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]

              assertResponse(
                order,
                orderResponse.data,
                onlineOrderAttribute = Some(onlineOrderAttribute),
              )
            }
          }

          "return an order by delivery_provider even if it is cloaked" in new OrderResourceFSpecContext {
            val onlineOrderAttribute =
              Factory
                .onlineOrderAttribute(merchant, Some(AcceptanceStatus.Open))
                .create

            val order =
              Factory
                .order(
                  merchant = merchant,
                  location = Some(london),
                  `type` = Some(OrderType.DeliveryRetail),
                  onlineOrderAttribute = Some(onlineOrderAttribute),
                  deliveryProvider = Some(DeliveryProvider.UberEats),
                )
                .create

            Get(
              s"/v1/orders.get?delivery_provider=${order.deliveryProvider.get.entryName}&delivery_provider_id=${order.deliveryProviderId.get}",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]

              assertResponse(
                order,
                orderResponse.data,
                onlineOrderAttribute = Some(onlineOrderAttribute),
              )
            }
          }

          "return correct statuses ensuring BC compatibility for kitchen status with bar only tickets" in new OrderResourceFSpecContext {
            val kitchen = Factory.kitchenKitchen(london).create
            val bar = Factory.kitchenBar(london).create

            val order = Factory.order(merchant, Some(london)).create
            val orderItem1 = Factory.orderItem(order).create

            val ticket1 =
              Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = bar.id).create
            Factory.ticketOrderItem(ticket1, orderItem1).create

            Factory.orderUser(order, user).create

            val items = Seq(orderItem1)
            val orderStatusPerItem =
              Map(orderItem1 -> Some(OrderRoutingStatus.New))
            val orderRoutingStatuses =
              OrderRoutingStatusesByType(KitchenType.Bar -> Some(OrderRoutingStatus.New), KitchenType.Kitchen -> None)

            Get(s"/v1/orders.get?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                items = Some(items),
                orderRoutingStatuses = Some(orderRoutingStatuses),
                orderStatusPerItem = Some(orderStatusPerItem),
              )
            }
          }
        }

        "expand payment_transactions" should {
          "return an order with order items expanded payment transactions" in new OrderResourceFSpecContext {
            val template = Factory.templateProduct(merchant).create
            val variant = Factory.variantProduct(merchant, template).create

            val variantOptionType = Factory.variantOptionType(template, name = Some("color")).create
            val variantOption = Factory.variantOption(template, variantOptionType, "blue").create
            Factory.productVariantOption(variant, variantOption).create

            val modifierSet = Factory.modifierSet(merchant).create
            val modifierOption = Factory.modifierOption(modifierSet).create

            val discount = Factory.discount(merchant).create

            val statusTransitions =
              Seq(StatusTransition(UUID.randomUUID, OrderStatus.Ready, UtcTime.now.minusHours(1)))

            val taxRate = Factory.taxRate(merchant).create

            val order = Factory
              .order(merchant, `type` = Some(OrderType.InStorePickUp), statusTransitions = Some(statusTransitions))
              .create
            val orderItem1 = Factory.orderItem(order, product = Some(variant)).create
            val orderItem2 = Factory.orderItem(order).create

            val paymentTransaction1 =
              Factory.paymentTransaction(order, orderItems = Seq(orderItem1, orderItem2)).create
            val paymentTransaction2 = Factory.paymentTransaction(order).create
            val paymentTransaction1Fee1 = Factory.paymentTransactionFee(paymentTransaction1).create
            Factory.orderTaxRate(order, taxRate).create

            Factory.orderItemVariantOption(orderItem1, variantOption).create
            Factory.orderItemModifierOption(orderItem1, modifierOption).create
            Factory.orderItemTaxRate(orderItem1, taxRate).create
            Factory.orderItemDiscount(orderItem1, discount).create

            Get(s"/v1/orders.get?order_id=${order.id}&expand[]=payment_transactions")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                paymentTransactions = Some(Seq(paymentTransaction1, paymentTransaction2)),
                paymentTransactionFees = Map(paymentTransaction1 -> Seq(paymentTransaction1Fee1)),
                items = Some(Seq(orderItem1, orderItem2)),
                statusTransitions = Some(statusTransitions),
              )
            }
          }
        }

        "expand gift_card_passes" should {
          "return an order with order items expanded gift card passes" in new OrderResourceFSpecContext {
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val order = Factory
              .order(merchant)
              .create
            val orderItem = Factory.orderItem(order, product = Some(giftCardProduct)).create
            val giftCardPass =
              Factory
                .giftCardPass(giftCard, orderItem, recipientEmail = Some("foo"))
                .create

            Get(s"/v1/orders.get?order_id=${order.id}&expand[]=gift_card_passes")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                items = Some(Seq(orderItem)),
                giftCardPassInfoPerItem = Some(Map(orderItem -> GiftCardPassInfo.fromRecord(giftCardPass))),
              )
            }
          }
        }

        "expand loyaltyPoints" should {
          "return an order with actually assigned loyalty points expanded for frequency based loyalty programs" in new OrderResourceFSpecContext {
            val globalCustomer = Factory.globalCustomer().create
            val customer = Factory.customerMerchant(merchant, globalCustomer).create
            val order = Factory
              .order(merchant, customer = Some(customer))
              .create
            val orderItem = Factory.orderItem(order).create

            val loyaltyProgram =
              Factory.loyaltyProgram(merchant, `type` = Some(LoyaltyProgramType.Frequency), points = Some(100)).create
            val loyaltyMembership = Factory
              .loyaltyMembership(
                globalCustomer,
                loyaltyProgram,
                points = Some(100),
                merchantOptInAt = Some(UtcTime.now),
              )
              .create
            Factory
              .loyaltyPointsHistory(
                loyaltyMembership,
                100,
                LoyaltyPointsHistoryType.Visit,
                order = Some(order),
                objectId = Some(order.id),
              )
              .create

            Get(s"/v1/orders.get?order_id=${order.id}&expand[]=loyalty_points")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                customer = Some(customer),
                items = Some(Seq(orderItem)),
                loyaltyPoints = Some(LoyaltyPoints.actual(100)),
              )
            }
          }

          "return an order with actually assigned loyalty points expanded for spend based loyalty programs" in new OrderResourceFSpecContext {
            val globalCustomer = Factory.globalCustomer().create
            val customer = Factory.customerMerchant(merchant, globalCustomer).create
            val order = Factory
              .order(merchant, customer = Some(customer))
              .create
            val orderItem = Factory.orderItem(order).create
            val paymentTransaction = Factory.paymentTransaction(order).create

            val loyaltyProgram =
              Factory.loyaltyProgram(merchant, `type` = Some(LoyaltyProgramType.Spend), points = Some(100)).create
            val loyaltyMembership = Factory
              .loyaltyMembership(
                globalCustomer,
                loyaltyProgram,
                points = Some(100),
                merchantOptInAt = Some(UtcTime.now),
              )
              .create
            Factory
              .loyaltyPointsHistory(
                loyaltyMembership,
                100,
                LoyaltyPointsHistoryType.SpendTransaction,
                order = Some(order),
                objectId = Some(paymentTransaction.id),
              )
              .create

            Get(s"/v1/orders.get?order_id=${order.id}&expand[]=loyalty_points")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                customer = Some(customer),
                items = Some(Seq(orderItem)),
                loyaltyPoints = Some(LoyaltyPoints.actual(100)),
              )
            }
          }

          "if customer is enrolled and order generated zero points" should {
            "return an order with actually zero assigned loyalty points expanded" in new OrderResourceFSpecContext {
              val globalCustomer = Factory.globalCustomer().create
              val customer = Factory.customerMerchant(merchant, globalCustomer).create
              val order = Factory
                .order(merchant, customer = Some(customer), totalAmount = Some(10))
                .create
              val orderItem = Factory.orderItem(order).create

              val loyaltyProgram = Factory
                .loyaltyProgram(
                  merchant,
                  `type` = Some(LoyaltyProgramType.Frequency),
                  points = Some(10),
                  minimumPurchaseAmount = Some(20),
                )
                .create
              val loyaltyMembership =
                Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

              Get(s"/v1/orders.get?order_id=${order.id}&expand[]=loyalty_points")
                .addHeader(authorizationHeader) ~> routes ~> check {
                val orderResponse = responseAs[ApiResponse[OrderEntity]]
                assertResponse(
                  order,
                  orderResponse.data,
                  customer = Some(customer),
                  items = Some(Seq(orderItem)),
                  loyaltyPoints = Some(LoyaltyPoints.actual(0)),
                )
              }
            }
          }

          "return an order with potentially assigned loyalty points expanded" in new OrderResourceFSpecContext {
            val globalCustomer = Factory.globalCustomer().create
            val customer = Factory.customerMerchant(merchant, globalCustomer).create

            val order = Factory
              .order(
                merchant,
                location = Some(rome),
                globalCustomer = Some(globalCustomer),
                totalAmount = Some(40),
                tipAmount = Some(0),
              )
              .create
            val orderItem = Factory.orderItem(order).create

            val loyaltyProgram =
              Factory
                .loyaltyProgram(
                  merchant,
                  `type` = Some(LoyaltyProgramType.Frequency),
                  points = Some(10),
                  minimumPurchaseAmount = Some(20),
                )
                .create

            Get(s"/v1/orders.get?order_id=${order.id}&expand[]=loyalty_points")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                items = Some(Seq(orderItem)),
                customer = Some(customer),
                loyaltyPoints = Some(LoyaltyPoints.potential(10)),
              )
            }
          }
        }

        "with tipsAssignments" should {
          "return an order with tips assignments" in new OrderResourceFSpecContext {
            val order = Factory.order(merchant, Some(london)).create
            val tipsAssignment = Factory.tipsAssignment(merchant, london, order = Some(order)).create

            val otherOrder = Factory.order(merchant, Some(london)).create
            val otherTipsAssignment = Factory.tipsAssignment(merchant, london, order = Some(otherOrder)).create

            Get(s"/v1/orders.get?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
                tipsAssignments = Some(
                  Seq(
                    tipsAssignment,
                  ),
                ),
              )
            }
          }
        }

        "find by delivery_provider_id" should {
          "returns an order" in new OrderResourceFSpecContext {
            val order = Factory.order(merchant, Some(london), deliveryProvider = Some(DeliveryProvider.UberEats)).create

            Get(s"/v1/orders.get?delivery_provider=uber_eats&delivery_provider_id=${order.deliveryProviderId.get}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val orderResponse = responseAs[ApiResponse[OrderEntity]]
              assertResponse(
                order,
                orderResponse.data,
              )
            }
          }

          "rejects the request when the delivery provider is wrong" in new OrderResourceFSpecContext {
            val order = Factory.order(merchant, Some(london), deliveryProvider = Some(DeliveryProvider.UberEats)).create

            Get(s"/v1/orders.get?delivery_provider=postmates&delivery_provider_id=${order.deliveryProviderId.get}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }

      "if the order does not belong to the merchant" should {
        "return 404" in new OrderResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorOrder = Factory.order(competitor).create

          Get(s"/v1/orders.get?order_id=${competitorOrder.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
