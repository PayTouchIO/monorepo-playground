package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.core.data.model.{ LoyaltyRewardRecord, MerchantNote => MerchantNoteModel }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.{ LoyaltyProgramType, RewardType, TicketStatus }
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, UtcTime }

@scala.annotation.nowarn("msg=Auto-application")
class OrdersSyncExistingFSpec extends OrdersSyncAssertsFSpec {

  "POST /v1/orders.sync?order_id=$" in {
    "if request has valid token" in {

      "if the order exists" should {
        "update the order and all its relations clearing old relations but merging payment transactions" in new OrdersSyncFSpecContext
          with Fixtures {
          val discount1 = Factory.discount(merchant).create
          val discount2 = Factory.discount(merchant).create

          val modifierSet = Factory.modifierSet(merchant).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val taxRate = Factory.taxRate(merchant).create

          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "foo").create

          val merchantNote = random[MerchantNoteModel]

          val order = Factory.order(merchant, location = Some(london), merchantNotes = Seq(merchantNote)).create
          val orderItem = Factory.orderItem(order, Some(product)).create
          val orderItemDiscount1 = Factory.orderItemDiscount(orderItem, discount1).create
          val orderItemDiscount2 = Factory.orderItemDiscount(orderItem).create
          val orderItemDiscount3 = Factory.orderItemDiscount(orderItem).create
          val orderItemModifierOption = Factory.orderItemModifierOption(orderItem, modifierOption1).create
          val orderItemVariantOption = Factory.orderItemVariantOption(orderItem, variantOption1).create
          val orderItemTaxRate = Factory.orderItemTaxRate(orderItem, taxRate).create

          val paymentTransaction = Factory.paymentTransaction(order).create
          val paymentTransactionOrderItem = Factory.paymentTransactionOrderItem(paymentTransaction, orderItem).create
          val orderDiscount = Factory.orderDiscount(order, discount2).create

          val bundle = Factory.comboProduct(merchant).create
          val bundleSet = Factory.bundleSet(bundle).create
          val bundleOption = Factory.bundleOption(bundleSet, product).create

          val orderBundle = Factory.orderBundle(order, orderItem, orderItem, Some(bundleSet), Some(bundleOption)).create

          val orderItemUpsertion =
            randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              discounts = Seq(
                random[ItemDiscountUpsertion].copy(
                  id = Some(orderItemDiscount1.id),
                  discountId = Some(discount1.id),
                  title = orderItemDiscount1.title,
                ),
                random[ItemDiscountUpsertion]
                  .copy(id = Some(orderItemDiscount2.id), discountId = None, title = orderItemDiscount2.title),
              ),
              modifierOptions =
                Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(modifierOption2.id))),
              variantOptions =
                Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption2.id))),
              taxRates =
                Seq(random[OrderItemTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = Some(taxRate.id))),
            )
          val orderBundleOptionUpsertion = random[OrderBundleOptionUpsertion].copy(
            id = UUID.randomUUID,
            bundleOptionId = bundleOption.id,
            articleOrderItemId = orderItemUpsertion.id,
          )
          val orderBundleSetUpsertion = random[OrderBundleSetUpsertion].copy(
            id = UUID.randomUUID,
            bundleSetId = bundleSet.id,
            orderBundleOptions = Seq(orderBundleOptionUpsertion),
          )
          val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
            id = UUID.randomUUID,
            bundleOrderItemId = orderItemUpsertion.id,
            orderBundleSets = Seq(orderBundleSetUpsertion),
          )

          val merchantNoteUpsertion = random[MerchantNoteUpsertion]

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            paymentTransactions = Seq(
              random[PaymentTransactionUpsertion].copy(
                id = UUID.randomUUID,
                orderItemIds = Seq(orderItemUpsertion.id),
                refundedPaymentTransactionId = None,
                fees = Seq.empty,
              ),
            ),
            taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = taxRate.id)),
            paymentStatus = PaymentStatus.Pending,
            discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(discount2.id))),
            bundles = Seq(orderBundleUpsertion),
            merchantNotes = Seq(merchantNoteUpsertion.copy(id = merchantNote.id)),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
            orderItemDao.findById(orderItem.id).await must beNone
            orderItemDiscountDao.findById(orderItemDiscount3.id).await must beNone
            orderItemModifierOptionDao.findById(orderItemModifierOption.id).await must beNone
            orderItemVariantOptionDao.findById(orderItemVariantOption.id).await must beNone
            orderItemTaxRateDao.findById(orderItemTaxRate.id).await must beNone
            paymentTransactionDao.countAllByMerchantId(merchant.id).await ==== 2 // existing + upserted transaction
          }
        }

        "updates the order and all its relations merging payment transactions and not updating existing ones" in new OrdersSyncFSpecContext
          with Fixtures {
          val order = Factory.order(merchant).create
          val paymentTransaction = Factory.paymentTransaction(order).create

          val existingPaymentTransactionUpsertion =
            random[PaymentTransactionUpsertion].copy(
              id = paymentTransaction.id,
              orderItemIds = Seq.empty,
              refundedPaymentTransactionId = None,
              fees = Seq.empty,
            )
          val newPaymentTransactionUpsertion =
            random[PaymentTransactionUpsertion].copy(
              id = UUID.randomUUID,
              orderItemIds = Seq.empty,
              refundedPaymentTransactionId = None,
              fees = Seq.empty,
            )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            paymentTransactions = Seq(existingPaymentTransactionUpsertion, newPaymentTransactionUpsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            val unchangedExistingPaymentTransaction = PaymentTransactionUpsertion(
              id = paymentTransaction.id,
              `type` = paymentTransaction.`type`,
              refundedPaymentTransactionId = None,
              paymentType = paymentTransaction.paymentType,
              paymentDetails = paymentTransaction.paymentDetails,
              paidAt = paymentTransaction.paidAt,
              fees = Seq.empty,
              paymentProcessorV2 = None,
            )
            val expectedUpsertionWithUnchangedExistingPaymentTransaction = upsertion.copy(
              paymentTransactions = Seq(unchangedExistingPaymentTransaction, newPaymentTransactionUpsertion),
            )
            assertUpsertion(orderResponse, expectedUpsertionWithUnchangedExistingPaymentTransaction)
            paymentTransactionDao.countAllByMerchantId(merchant.id).await ==== 2 // existing + upserted transaction
          }
        }

        "Paytouch" in {
          "when order status is set to canceled also cancel all the tickets that are not completed" in new OrdersSyncFSpecContext
            with Fixtures {
            val order = Factory.order(merchant, Some(london)).create
            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create
            val orderItem3 = Factory.orderItem(order).create
            val kitchen = Factory.kitchen(london).create

            val ticket1 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.Completed),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket2 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.InProgress),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket1OrderItem1 = Factory.ticketOrderItem(ticket1, orderItem1).create
            val ticket1OrderItem2 = Factory.ticketOrderItem(ticket1, orderItem2).create
            val ticket2OrderItem3 = Factory.ticketOrderItem(ticket2, orderItem3).create

            val upsertion = baseOrderUpsertion.copy(
              status = OrderStatus.Canceled,
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              val tickets = ticketDao.findByOrderId(order.id).await
              tickets.size ==== 2
              tickets.find(_.id == ticket1.id).get.status ==== TicketStatus.Completed
              tickets.find(_.id == ticket2.id).get.status ==== TicketStatus.Canceled
              tickets.find(_.id == ticket2.id).get.show ==== false
            }
          }
        }

        "Dash" in {
          "when order status is set to canceled also cancel all the tickets that are not completed and hide if acknowledged" in new OrdersSyncFSpecContext
            with Fixtures {
            override lazy val merchant =
              Factory
                .merchant
                .createForceOverride(
                  _.copy(setupType = SetupType.Dash.some),
                )

            val onlineOrderAttribute =
              Factory
                .onlineOrderAttribute(merchant)
                .createForceOverride(_.copy(cancellationStatus = CancellationStatus.Acknowledged.some))

            val order =
              Factory
                .order(
                  merchant,
                  Some(london),
                  onlineOrderAttribute = onlineOrderAttribute.some,
                )
                .create

            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create
            val orderItem3 = Factory.orderItem(order).create
            val kitchen = Factory.kitchen(london).create

            val ticket1 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.Completed),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket2 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.InProgress),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket1OrderItem1 = Factory.ticketOrderItem(ticket1, orderItem1).create
            val ticket1OrderItem2 = Factory.ticketOrderItem(ticket1, orderItem2).create
            val ticket2OrderItem3 = Factory.ticketOrderItem(ticket2, orderItem3).create

            val upsertion = baseOrderUpsertion.copy(
              status = OrderStatus.Canceled,
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              val tickets = ticketDao.findByOrderId(order.id).await
              tickets.size ==== 2
              tickets.find(_.id == ticket1.id).get.status ==== TicketStatus.Completed
              tickets.find(_.id == ticket2.id).get.status ==== TicketStatus.Canceled
              tickets.find(_.id == ticket2.id).get.show ==== false
            }
          }

          "when order status is set to canceled also cancel all the tickets that are not completed but NOT hide UNLESS acknowledged" in new OrdersSyncFSpecContext
            with Fixtures {
            override lazy val merchant =
              Factory
                .merchant
                .createForceOverride(
                  _.copy(setupType = SetupType.Dash.some),
                )

            val order =
              Factory.order(merchant, Some(london)).create

            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create
            val orderItem3 = Factory.orderItem(order).create
            val kitchen = Factory.kitchen(london).create

            val ticket1 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.Completed),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket2 =
              Factory
                .ticket(
                  order,
                  status = Some(TicketStatus.InProgress),
                  routeToKitchenId = kitchen.id,
                )
                .create

            val ticket1OrderItem1 = Factory.ticketOrderItem(ticket1, orderItem1).create
            val ticket1OrderItem2 = Factory.ticketOrderItem(ticket1, orderItem2).create
            val ticket2OrderItem3 = Factory.ticketOrderItem(ticket2, orderItem3).create

            val upsertion = baseOrderUpsertion.copy(
              status = OrderStatus.Canceled,
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              val tickets = ticketDao.findByOrderId(order.id).await
              tickets.size ==== 2
              tickets.find(_.id == ticket1.id).get.status ==== TicketStatus.Completed
              tickets.find(_.id == ticket2.id).get.status ==== TicketStatus.Canceled
              tickets.find(_.id == ticket2.id).get.show ==== true // the important bit
            }
          }
        }

        "when order status is set to completed also complete all the tickets that are new" in new OrdersSyncFSpecContext
          with Fixtures {
          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create
          val orderItem4 = Factory.orderItem(order).create
          val kitchen = Factory.kitchen(london).create
          val ticket1 =
            Factory.ticket(order, status = Some(TicketStatus.Completed), routeToKitchenId = kitchen.id).create
          val ticket2 =
            Factory.ticket(order, status = Some(TicketStatus.InProgress), routeToKitchenId = kitchen.id).create
          val ticket3 =
            Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create
          val ticket1OrderItem1 = Factory.ticketOrderItem(ticket1, orderItem1).create
          val ticket1OrderItem2 = Factory.ticketOrderItem(ticket1, orderItem2).create
          val ticket2OrderItem3 = Factory.ticketOrderItem(ticket2, orderItem3).create
          val ticket3OrderItem4 = Factory.ticketOrderItem(ticket3, orderItem4).create

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.Completed,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)
            val tickets = ticketDao.findByOrderId(order.id).await
            tickets.size ==== 3
            tickets.find(_.id == ticket1.id).get.status ==== TicketStatus.Completed
            tickets.find(_.id == ticket2.id).get.status ==== TicketStatus.InProgress
            val ticketEntity3 = tickets.find(_.id == ticket3.id).get
            ticketEntity3.status ==== TicketStatus.Completed
            ticketEntity3.show ==== false
          }
        }

        "when order status is not set to canceled do not touch any ticket status" in new OrdersSyncFSpecContext
          with Fixtures {
          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create
          val kitchen = Factory.kitchen(london).create
          val ticket1 =
            Factory.ticket(order, status = Some(TicketStatus.Completed), routeToKitchenId = kitchen.id).create
          val ticket2 =
            Factory.ticket(order, status = Some(TicketStatus.InProgress), routeToKitchenId = kitchen.id).create
          val ticket1OrderItem1 = Factory.ticketOrderItem(ticket1, orderItem1).create
          val ticket1OrderItem2 = Factory.ticketOrderItem(ticket1, orderItem2).create
          val ticket2OrderItem3 = Factory.ticketOrderItem(ticket2, orderItem3).create

          val upsertion = baseOrderUpsertion.copy(status = OrderStatus.InKitchen)

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)
            val tickets = ticketDao.findByOrderId(order.id).await
            tickets.size ==== 2
            tickets.find(_.id == ticket1.id).get.status ==== TicketStatus.Completed
            tickets.find(_.id == ticket2.id).get.status ==== TicketStatus.InProgress
          }
        }

        "when non pending, update the order and all its relations without cancelling order items" in new OrdersSyncFSpecContext
          with Fixtures {
          val discount1 = Factory.discount(merchant).create
          val discount2 = Factory.discount(merchant).create

          val modifierSet = Factory.modifierSet(merchant).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val taxRate = Factory.taxRate(merchant).create

          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "foo").create

          val order = Factory.order(merchant, location = Some(london)).create
          val orderItem = Factory.orderItem(order, Some(product)).create
          val orderItemDiscount = Factory.orderItemDiscount(orderItem, discount1).create
          val orderItemModifierOption = Factory.orderItemModifierOption(orderItem, modifierOption1).create
          val orderItemVariantOption = Factory.orderItemVariantOption(orderItem, variantOption1).create
          val orderItemTaxRate = Factory.orderItemTaxRate(orderItem, taxRate).create

          val paymentTransaction = Factory.paymentTransaction(order).create
          val paymentTransactionOrderItem = Factory.paymentTransactionOrderItem(paymentTransaction, orderItem).create

          val orderItemUpsertion =
            randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(discount2.id))),
              modifierOptions =
                Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(modifierOption2.id))),
              variantOptions =
                Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption2.id))),
              taxRates =
                Seq(random[OrderItemTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = Some(taxRate.id))),
            )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            paymentTransactions = Seq(
              random[PaymentTransactionUpsertion].copy(
                id = UUID.randomUUID,
                orderItemIds = Seq(orderItemUpsertion.id),
                refundedPaymentTransactionId = None,
                fees = Seq.empty,
              ),
            ),
            taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = None, taxRateId = taxRate.id)),
            paymentStatus = PaymentStatus.Paid,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
            orderItemDao.findByOrderId(order.id).await.map(_.id) should containTheSameElementsAs(
              Seq(orderItem.id, orderItemUpsertion.id),
            )
          }
        }

        "when pending, update the order and  all its relations order items included" in new OrdersSyncFSpecContext
          with Fixtures {
          val discount1 = Factory.discount(merchant).create
          val discount2 = Factory.discount(merchant).create

          val modifierSet = Factory.modifierSet(merchant).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val taxRate = Factory.taxRate(merchant).create

          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "foo").create

          val order = Factory.order(merchant, location = Some(london)).create
          val orderItem = Factory.orderItem(order, Some(product)).create
          val orderItemDiscount = Factory.orderItemDiscount(orderItem, discount1).create
          val orderItemModifierOption = Factory.orderItemModifierOption(orderItem, modifierOption1).create
          val orderItemVariantOption = Factory.orderItemVariantOption(orderItem, variantOption1).create
          val orderItemTaxRate = Factory.orderItemTaxRate(orderItem, taxRate).create

          val paymentTransaction = Factory.paymentTransaction(order).create
          val paymentTransactionOrderItem = Factory.paymentTransactionOrderItem(paymentTransaction, orderItem).create

          val orderItemUpsertion =
            randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(discount2.id))),
              modifierOptions =
                Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(modifierOption2.id))),
              variantOptions =
                Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption2.id))),
              taxRates =
                Seq(random[OrderItemTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = Some(taxRate.id))),
            )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            paymentTransactions = Seq(
              random[PaymentTransactionUpsertion].copy(
                id = UUID.randomUUID,
                orderItemIds = Seq(orderItemUpsertion.id),
                refundedPaymentTransactionId = None,
                fees = Seq.empty,
              ),
            ),
            taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = taxRate.id)),
            paymentStatus = PaymentStatus.Pending,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
            orderItemDiscountDao.findById(orderItemDiscount.id).await must beNone
            orderItemDao.findByOrderId(order.id).await.map(_.id) ==== Seq(orderItemUpsertion.id)
            orderItemDao.findById(orderItem.id).await should beNone
            orderItemModifierOptionDao.findById(orderItemModifierOption.id).await must beNone
            orderItemVariantOptionDao.findById(orderItemVariantOption.id).await must beNone
            orderItemTaxRateDao.findById(orderItemTaxRate.id).await must beNone
            paymentTransactionDao.countAllByMerchantId(merchant.id).await ==== 2 // existing + upserted transaction
          }
        }

        "when option name is missing, accept order anyway and fill option name from db" in new OrdersSyncFSpecContext
          with Fixtures {
          val order = Factory.order(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "bar").create

          val variantOptionUpsertion =
            random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption1.id), optionName = None)

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            productId = Some(product.id),
            variantOptions = Seq(variantOptionUpsertion),
          )

          val upsertion = baseOrderUpsertion.copy(
            items = Seq(orderItemUpsertion),
            paymentStatus = PaymentStatus.Pending,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            val orderItemVariantOptions =
              orderItemVariantOptionDao.findAllByOrderItemIds(Seq(orderItemUpsertion.id)).await
            orderItemVariantOptions.size ==== 1
            val orderItemVariantOption = orderItemVariantOptions.head
            orderItemVariantOption.optionName ==== variantOption1.name
            variantOptionUpsertion.optionTypeName ==== Some(orderItemVariantOption.optionTypeName)
          }
        }

        "when option type name is missing, accept order anyway and fill option type name from db" in new OrdersSyncFSpecContext
          with Fixtures {
          val order = Factory.order(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "bar").create

          val variantOptionUpsertion =
            random[OrderItemVariantOptionUpsertion]
              .copy(variantOptionId = Some(variantOption1.id), optionTypeName = None)

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            productId = Some(product.id),
            variantOptions = Seq(variantOptionUpsertion),
          )

          val upsertion = baseOrderUpsertion.copy(
            items = Seq(orderItemUpsertion),
            paymentStatus = PaymentStatus.Pending,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            val orderItemVariantOptions =
              orderItemVariantOptionDao.findAllByOrderItemIds(Seq(orderItemUpsertion.id)).await
            orderItemVariantOptions.size ==== 1
            val orderItemVariantOption = orderItemVariantOptions.head
            variantOptionUpsertion.optionName ==== Some(orderItemVariantOption.optionName)
            orderItemVariantOption.optionTypeName ==== variantOptionType.name
          }
        }

        "don't overwrite `worldpay` key once it's been written" in new OrdersSyncFSpecContext with Fixtures {
          val order = Factory.order(merchant).create
          val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create
          val paymentTransaction = Factory
            .paymentTransaction(
              order,
              paymentDetails = Some(random[PaymentDetails].copy(worldpay = Some(JString("foo")))),
            )
            .create

          val upsertion = baseOrderUpsertion.copy(
            paymentStatus = PaymentStatus.Paid,
            status = OrderStatus.Completed,
            paymentTransactions = Seq(
              random[PaymentTransactionUpsertion].copy(
                id = paymentTransaction.id,
                `type` = Some(TransactionType.Payment),
                paymentType = Some(TransactionPaymentType.Cash),
                paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2, worldpay = None)),
                fees = Seq.empty,
                orderItemIds = Seq(orderItem.id),
              ),
            ),
            items = Seq.empty,
            totalAmount = BigDecimal(15),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val paymentDetails = paymentTransactionDao.findById(paymentTransaction.id).await.get.paymentDetails.get
            paymentDetails.worldpay ==== Some(JString("foo"))
          }
        }

        "if a gift card pass creation is needed" should {

          "reject request if item price amount is missing" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant).create
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = None,
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("GiftCardPassWithoutPrice")
            }
          }

          "reject request if merchant has no gift card" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant).create
            val giftCardProduct = Factory.giftCardProduct(merchant).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(10),
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("GiftCardPassWithoutGiftCard")
            }
          }

          "do not create the pass if status is not changing to paid" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(10),
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)

              assertNoPassAttached(orderItemUpsertion.id)
            }
          }

          "create the pass with predefined amount if status is changing to paid" in new OrdersSyncFSpecContext
            with Fixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct, amounts = Some(Seq(1, 2, 3, 4))).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(1),
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)

              assertGiftCardPassCreation(orderItemUpsertion, giftCard, isCustomAmount = false)
            }

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)

              assertPassAttached(orderItemUpsertion.id)
            }
          }

          "create the pass with custom amount if status is changing to paid" in new OrdersSyncFSpecContext
            with Fixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct, amounts = Some(Seq(1, 2, 3, 4))).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(10),
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)

              assertGiftCardPassCreation(orderItemUpsertion, giftCard, isCustomAmount = true)
            }

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)

              assertPassAttached(orderItemUpsertion.id)
            }
          }

          "mark reward redemption as redeemed" in {
            trait RewardRedemptionFixtures extends OrdersSyncFSpecContext with Fixtures {
              lazy val order = Factory.order(merchant).create
              lazy val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create
              lazy val loyaltyProgram = Factory.loyaltyProgram(merchant).create
              lazy val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram).create
              lazy val rewardRedemption =
                Factory.rewardRedemption(loyaltyMembership, loyaltyReward).create
              val loyaltyReward: LoyaltyRewardRecord
            }
            "loyalty reward = free product" in new RewardRedemptionFixtures {
              lazy val loyaltyReward =
                Factory.loyaltyReward(loyaltyProgram, rewardType = Some(RewardType.FreeProduct)).create

              val orderItemUpsertion = randomOrderItemUpsertion().copy(id = orderItem.id, productId = Some(product.id))

              val rewardRedemptionSync = RewardRedemptionSync(
                rewardRedemptionId = rewardRedemption.id,
                objectId = orderItem.id,
                objectType = RewardRedemptionType.OrderItem,
              )

              val rewardRedemption2 =
                Factory.rewardRedemption(loyaltyMembership, loyaltyReward).create
              val rewardRedemptionSyncUnexisting = RewardRedemptionSync(
                rewardRedemptionId = rewardRedemption2.id,
                objectId = UUID.randomUUID,
                objectType = RewardRedemptionType.OrderItem,
              )

              val upsertion = baseOrderUpsertion.copy(
                items = Seq(orderItemUpsertion),
                rewards = Seq(rewardRedemptionSync, rewardRedemptionSyncUnexisting),
              )

              Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                val orderResponse = responseAs[ApiResponse[OrderEntity]].data
                val upsertionWithSkippedInvalidRewardRedemption = upsertion.copy(rewards = Seq(rewardRedemptionSync))
                assertUpsertion(orderResponse, upsertionWithSkippedInvalidRewardRedemption)
              }
            }
            "loyalty reward = ticket discount" in new RewardRedemptionFixtures {
              lazy val loyaltyReward =
                Factory.loyaltyReward(loyaltyProgram, rewardType = Some(RewardType.DiscountPercentage)).create

              val ticketDiscountId = UUID.randomUUID
              val ticketDiscount = random[ItemDiscountUpsertion].copy(id = Some(ticketDiscountId))

              val rewardRedemptionSync = RewardRedemptionSync(
                rewardRedemptionId = rewardRedemption.id,
                objectId = ticketDiscountId,
                objectType = RewardRedemptionType.OrderDiscount,
              )

              val upsertion = baseOrderUpsertion.copy(
                items = Seq.empty,
                rewards = Seq(rewardRedemptionSync),
                discounts = Seq(ticketDiscount),
              )

              Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                val orderResponse = responseAs[ApiResponse[OrderEntity]].data
                assertUpsertion(orderResponse, upsertion)
              }
            }
          }
        }

        "if the customer id is given and customer already linked to location and is enrolled in loyalty program" should {
          trait LoyaltyAwardFixtures extends OrdersSyncFSpecContext with Fixtures {
            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(london),
                active = Some(true),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(1),
                spendAmountForPoints = Some(1),
                pointsToReward = Some(1),
                minimumPurchaseAmount = Some(1),
              )
              .create
            Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create
          }

          "link customer to order and location and assign loyalty points" in new LoyaltyAwardFixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create

            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(product.id),
              totalPriceAmount = Some(15),
              paymentStatus = Some(PaymentStatus.Paid),
              discounts = Seq.empty,
              modifierOptions = Seq.empty,
              taxRates = Seq.empty,
              variantOptions = Seq.empty,
            )
            val upsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Paid,
              status = OrderStatus.Completed,
              paymentTransactions = Seq(
                random[PaymentTransactionUpsertion].copy(
                  id = UUID.randomUUID,
                  `type` = Some(TransactionType.Payment),
                  paymentType = Some(TransactionPaymentType.Cash),
                  paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
                  fees = Seq.empty,
                  orderItemIds = Seq(orderItem.id),
                ),
              ),
              items = Seq(itemUpsertion),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(globalCustomer.id, london.id, totalVisits = 1, totalSpend = 15)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }
          }

          "doesn't award double points when synced twice" in new LoyaltyAwardFixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create

            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(product.id),
              totalPriceAmount = Some(15),
              paymentStatus = Some(PaymentStatus.Paid),
              discounts = Seq.empty,
              modifierOptions = Seq.empty,
              taxRates = Seq.empty,
              variantOptions = Seq.empty,
            )
            val upsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Paid,
              status = OrderStatus.Completed,
              paymentTransactions = Seq(
                random[PaymentTransactionUpsertion].copy(
                  id = UUID.randomUUID,
                  `type` = Some(TransactionType.Payment),
                  paymentType = Some(TransactionPaymentType.Cash),
                  paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
                  fees = Seq.empty,
                  orderItemIds = Seq(orderItem.id),
                ),
              ),
              items = Seq(itemUpsertion),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(globalCustomer.id, london.id, totalVisits = 1, totalSpend = 15)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }

            Thread.sleep(100)

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(globalCustomer.id, london.id, totalVisits = 1, totalSpend = 15)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }
          }

          "subtracts loyalty points when an order is refunded" in new LoyaltyAwardFixtures {
            val order = Factory.order(merchant).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Pending)).create

            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(product.id),
              totalPriceAmount = Some(15),
              paymentStatus = Some(PaymentStatus.Paid),
              discounts = Seq.empty,
              modifierOptions = Seq.empty,
              taxRates = Seq.empty,
              variantOptions = Seq.empty,
            )

            val paymentTransactionUpsertion = random[PaymentTransactionUpsertion].copy(
              id = UUID.randomUUID,
              `type` = Some(TransactionType.Payment),
              paymentType = Some(TransactionPaymentType.Cash),
              paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
              fees = Seq.empty,
              orderItemIds = Seq(orderItem.id),
            )

            val upsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Paid,
              status = OrderStatus.Completed,
              paymentTransactions = Seq(paymentTransactionUpsertion),
              items = Seq(itemUpsertion),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(globalCustomer.id, london.id, totalVisits = 1, totalSpend = 15)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }

            val itemRefundUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              paymentStatus = Some(PaymentStatus.Refunded),
            )

            val paymentTransactionRefundUpsertion = random[PaymentTransactionUpsertion].copy(
              id = UUID.randomUUID,
              `type` = Some(TransactionType.Refund),
              paymentType = Some(TransactionPaymentType.Cash),
              paymentDetails = Some(random[PaymentDetails].copy(amount = Some(15), tipAmount = 0)),
              fees = Seq.empty,
              orderItemIds = Seq(orderItem.id),
            )

            val refundUpsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Refunded,
              status = OrderStatus.Completed,
              paymentTransactions = Seq(paymentTransactionUpsertion, paymentTransactionRefundUpsertion),
              items = Seq(itemUpsertion),
            )

            Post(s"/v1/orders.sync?order_id=${order.id}", refundUpsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, refundUpsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(globalCustomer.id, london.id, totalVisits = 1, totalSpend = 15)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 0)
            }
          }
        }

        "seating" should {
          "add seating to an order without seating" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant, seating = None).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = orderItem.productId,
              paymentStatus = Some(PaymentStatus.Paid),
            )

            val seatingUpsertion = random[Seating]
            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion), seating = seatingUpsertion)

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              assertSeating(orderResponse, Some(seatingUpsertion))
            }
          }

          "remove seating from an order with seating" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant, seating = Some(random[Seating])).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = orderItem.productId,
              paymentStatus = Some(PaymentStatus.Paid),
            )

            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion), seating = ResettableSeating.reset)

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              assertSeating(orderResponse, None)
            }
          }

          "override seating on an order with seating" in new OrdersSyncFSpecContext with Fixtures {
            val order = Factory.order(merchant, seating = Some(random[Seating])).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = orderItem.productId,
              paymentStatus = Some(PaymentStatus.Paid),
            )

            val seatingUpsertion = random[Seating]
            val upsertion =
              baseOrderUpsertion.copy(items = Seq(orderItemUpsertion), seating = seatingUpsertion)

            Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              assertSeating(orderResponse, Some(seatingUpsertion))
            }
          }

          // Temproarily disabled as there is an issue with the serializtion of
          // ResettableSeating. Even though we set seating =
          // ResettableSeating.ignore, in the resource it comes in as
          // ResettableSeating.reset.
          //
          // This appears to be an issue with Akka Htto and/or Json4s. We have
          // tests to explicitely test this case in ResettableSerializerSpec and
          // when testing manually it works as expected.
          //
          //"ignore seating on an order with seating" in new OrdersSyncFSpecContext with Fixtures {
          //  val order = Factory.order(merchant, seating = Some(random[Seating])).create
          //  val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create

          //  val orderItemUpsertion = randomOrderItemUpsertion().copy(
          //    id = orderItem.id,
          //    productId = orderItem.productId,
          //    paymentStatus = Some(PaymentStatus.Paid),
          //  )

          //  val upsertion =
          //    baseOrderUpsertion.copy(items = Seq(orderItemUpsertion), seating = ResettableSeating.ignore)

          //  Post(s"/v1/orders.sync?order_id=${order.id}", upsertion)
          //    .addHeader(authorizationHeader) ~> routes ~> check {
          //    assertStatusOK()
          //    val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          //    assertUpsertion(orderResponse, upsertion)
          //    assertSeating(orderResponse, order.seating)
          //  }
          //}
        }
      }
    }
  }

}
