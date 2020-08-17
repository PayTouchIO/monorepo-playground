package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.OnlineOrderAttributeUpdate
import io.paytouch.core.entities.{ DeliveryAddressUpsertion, Order => OrderEntity, _ }
import io.paytouch.core.entities.enums.{ CustomerSource, LoyaltyProgramType, TicketStatus }
import io.paytouch.core.USD
import io.paytouch.core.utils.{ AppTokenFixtures, UtcTime, FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class OrdersSyncNewFSpec extends OrdersSyncAssertsFSpec {
  "POST /v1/orders.sync?order_id=$" in {
    "if request has valid token" in {
      "if the order doesn't exist" should {
        "creates the order and all its relations" in new OrdersSyncFSpecContext with Fixtures {
          val anotherUser = Factory.user(merchant, locations = Seq(rome)).create
          val orderId = UUID.randomUUID
          val discount = Factory.discount(merchant).create
          val discount2 = Factory.discount(merchant).create

          val modifierSet = Factory.modifierSet(merchant).create
          val modifierOption = Factory.modifierOption(modifierSet).create

          val taxRate = Factory.taxRate(merchant).create

          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption = Factory.variantOption(template, variantOptionType, "foo").create

          val bundle = Factory.comboProduct(merchant).create
          val bundleSet = Factory.bundleSet(bundle).create
          val bundleOption = Factory.bundleOption(bundleSet, product).create

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            quantity = Some(2),
            productId = Some(product.id),
            discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(discount.id))),
            modifierOptions =
              Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(modifierOption.id))),
            taxRates =
              Seq(random[OrderItemTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = Some(taxRate.id))),
            variantOptions = Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption.id))),
          )

          val paymentTypeValues = TransactionPaymentType.values

          val paymentTransactionUpsertions = paymentTypeValues.flatMap { paymentTypeValue =>
            val paymentTransactionUpsertions = random[PaymentTransactionUpsertion](2)

            val paymentTransactionUpA = paymentTransactionUpsertions(0).copy(
              id = UUID.randomUUID,
              orderItemIds = Seq(orderItemUpsertion.id),
              paymentType = Some(paymentTypeValue),
              paymentDetails = Some(random[PaymentDetails].copy(worldpay = Some(JString("foo")))),
              refundedPaymentTransactionId = None,
              fees = Seq(random[PaymentTransactionFeeUpsertion].copy(id = UUID.randomUUID)),
            )

            val paymentTransactionUpB = paymentTransactionUpsertions(1).copy(
              id = UUID.randomUUID,
              orderItemIds = Seq(orderItemUpsertion.id),
              paymentType = Some(paymentTypeValue),
              paymentDetails = Some(random[PaymentDetails]),
              refundedPaymentTransactionId = Some(paymentTransactionUpA.id),
              fees = Seq.empty,
            )

            Seq(paymentTransactionUpA, paymentTransactionUpB)
          }

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

          val seatingUpsertion = random[Seating]

          val upsertion = baseOrderUpsertion.copy(
            creatorUserId = Some(cashier.id),
            customerId = None,
            items = Seq(orderItemUpsertion),
            merchantNotes = Seq(random[MerchantNoteUpsertion].copy(userId = user.id)),
            paymentTransactions = paymentTransactionUpsertions,
            assignedUserIds = Some(Seq(user.id, anotherUser.id)),
            taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = taxRate.id)),
            discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(discount2.id))),
            deliveryAddress = None,
            onlineOrderAttribute = None,
            bundles = Seq(orderBundleUpsertion),
            seating = seatingUpsertion,
          )

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
            assertSeating(orderResponse, Some(seatingUpsertion))
          }

          // Sync twice to ensure idempotency
          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
            assertSeating(orderResponse, Some(seatingUpsertion))
          }
        }

        "if coming from storefront" should {
          "create a customer id and associate it with the order" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID
            val deliveryAddressUpsertion = random[DeliveryAddressUpsertion].copy(id = UUID.randomUUID)
            val onlineOrderAttributeUpsertion = randomOnlineOrderAttributeUpsertion().copy(
              email = Some(randomEmail),
              firstName = Some(randomWord),
              lastName = Some(randomWord),
              phoneNumber = Some(randomWord),
              acceptanceStatus = None,
            )

            val upsertion = baseOrderUpsertion.copy(
              creatorUserId = None,
              customerId = None,
              items = Seq.empty,
              merchantNotes = Seq.empty,
              paymentTransactions = Seq.empty,
              assignedUserIds = None,
              taxRates = Seq.empty,
              discounts = Seq.empty,
              source = Some(Source.Storefront),
              deliveryAddress = Some(deliveryAddressUpsertion),
              onlineOrderAttribute = Some(onlineOrderAttributeUpsertion),
            )

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))
              assertCustomer(
                merchant,
                orderResponse,
                email = onlineOrderAttributeUpsertion.email,
                firstName = onlineOrderAttributeUpsertion.firstName,
                lastName = onlineOrderAttributeUpsertion.lastName,
                phoneNumber = onlineOrderAttributeUpsertion.phoneNumber,
                address = Some(deliveryAddressUpsertion.address),
                source = Some(CustomerSource.PtStorefront),
              )
            }
          }
        }

        "if coming from a delivery provider" should {
          "create a customer id and associate it with the order" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID

            val onlineOrderAttributeUpsertion = randomOnlineOrderAttributeUpsertion().copy(
              email = None,
              firstName = Some(randomWord),
              lastName = None,
              phoneNumber = None,
              acceptanceStatus = Some(AcceptanceStatus.Pending),
            )

            val upsertion = baseOrderUpsertion.copy(
              creatorUserId = None,
              customerId = None,
              items = Seq.empty,
              merchantNotes = Seq.empty,
              assignedUserIds = None,
              taxRates = Seq.empty,
              discounts = Seq.empty,
              source = Some(Source.DeliveryProvider),
              deliveryAddress = None,
              onlineOrderAttribute = Some(onlineOrderAttributeUpsertion),
              deliveryProvider = Some(DeliveryProvider.UberEats),
              deliveryProviderId = Some("cb9d3ab0-7441-41ce-92fa-219f1d4112df"),
              deliveryProviderNumber = Some("112DF"),
              paymentStatus = PaymentStatus.Paid,
              paymentTransactions = Seq(
                PaymentTransactionUpsertion(
                  id = UUID.randomUUID,
                  `type` = Some(TransactionType.Payment),
                  paymentType = Some(TransactionPaymentType.DeliveryProvider),
                  paymentDetails = Some(
                    PaymentDetails(
                      amount = Some(BigDecimal(14.26)),
                      currency = Some(USD),
                    ),
                  ),
                  fees = Seq.empty,
                  refundedPaymentTransactionId = None,
                  paidAt = Some(baseOrderUpsertion.receivedAt),
                  paymentProcessorV2 = Some(TransactionPaymentProcessor.DeliveryProvider),
                ),
              ),
            )

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion)
              assertCustomer(
                merchant,
                orderResponse,
                firstName = onlineOrderAttributeUpsertion.firstName,
                source = Some(CustomerSource.UberEats),
              )
            }
          }
        }

        "ignores zero discounts" in new OrdersSyncFSpecContext with Fixtures {
          val anotherUser = Factory.user(merchant, locations = Seq(rome)).create
          val orderId = UUID.randomUUID

          val zeroDiscount = random[ItemDiscountUpsertion].copy(
            discountId = None,
            amount = 0,
            totalAmount = Some(0),
            title = None,
            `type` = DiscountType.Percentage,
          )

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            quantity = Some(1),
            productId = Some(product.id),
            discounts = Seq(zeroDiscount),
            modifierOptions = Seq.empty,
            taxRates = Seq.empty,
            variantOptions = Seq.empty,
          )

          val upsertion = baseOrderUpsertion.copy(
            creatorUserId = Some(cashier.id),
            customerId = None,
            items = Seq(orderItemUpsertion),
            merchantNotes = Seq(random[MerchantNoteUpsertion].copy(userId = user.id)),
            paymentTransactions = Seq.empty,
            assignedUserIds = None,
            taxRates = Seq.empty,
            discounts = Seq.empty,
          )

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            val upsertionWithoutZeroDiscount =
              upsertion.copy(items = Seq(orderItemUpsertion.copy(discounts = Seq.empty)))
            assertUpsertion(orderResponse, upsertionWithoutZeroDiscount)
          }
        }

        "create the order and track stock updates if needed" in new OrdersSyncFSpecContext with Fixtures {
          override lazy val product = Factory.simpleProduct(merchant, trackInventory = Some(true)).create
          val productLocation = Factory.productLocation(product, london).create
          val originalStockQuantity = 50
          val stock = Factory.stock(productLocation, quantity = Some(originalStockQuantity)).create

          val orderId = UUID.randomUUID

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            quantity = Some(2),
            productId = Some(product.id),
            paymentStatus = Some(genPositivePaymentStatus.instance),
          )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            assignedUserIds = Some(Seq(user.id)),
            status = genPositiveOrderStatus.instance,
          )

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
          }

          afterAWhile {
            val updatedStock = stockDao.findById(stock.id).await.get
            val quantityDecrease = orderItemUpsertion.quantity.getOrElse[BigDecimal](0)
            updatedStock.quantity ==== originalStockQuantity - quantityDecrease
          }

          afterAWhile {
            val history = productQuantityHistoryDao.findAllByProductIds(Seq(product.id)).await
            history.size ==== 1
          }
        }

        "create the online order and track stock updates if needed" in new OrdersSyncFSpecContext with Fixtures {
          override lazy val product = Factory.simpleProduct(merchant, trackInventory = Some(true)).create
          val productLocation = Factory.productLocation(product, london).create
          val originalStockQuantity = 50
          val stock = Factory.stock(productLocation, quantity = Some(originalStockQuantity)).create

          val orderId = UUID.randomUUID
          val deliveryAddressUpsertion = random[DeliveryAddressUpsertion].copy(id = UUID.randomUUID)

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            quantity = Some(2),
            productId = Some(product.id),
            paymentStatus = Some(genPositivePaymentStatus.instance),
          )

          val onlineOrderAttributeUpsertion = randomOnlineOrderAttributeUpsertion().copy(
            email = Some(randomEmail),
            firstName = Some(randomWord),
            lastName = Some(randomWord),
            phoneNumber = Some(randomWord),
            acceptanceStatus = Some(AcceptanceStatus.Open),
          )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            assignedUserIds = Some(Seq(user.id)),
            status = genPositiveOrderStatus.instance,
            source = Some(Source.Storefront),
            deliveryAddress = Some(deliveryAddressUpsertion),
            onlineOrderAttribute = Some(onlineOrderAttributeUpsertion),
          )

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
          }

          afterAWhile {
            val updatedStock = stockDao.findById(stock.id).await.get
            val quantityDecrease = orderItemUpsertion.quantity.getOrElse[BigDecimal](0)
            updatedStock.quantity ==== originalStockQuantity - quantityDecrease
          }

          afterAWhile {
            val history = productQuantityHistoryDao.findAllByProductIds(Seq(product.id)).await
            history.size ==== 1
          }

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
          }

          afterAWhile {
            val updatedStock = stockDao.findById(stock.id).await.get
            val quantityDecrease = orderItemUpsertion.quantity.getOrElse[BigDecimal](0)
            updatedStock.quantity ==== originalStockQuantity - quantityDecrease
          }

          afterAWhile {
            val history = productQuantityHistoryDao.findAllByProductIds(Seq(product.id)).await
            history.size ==== 1
          }
        }

        "create the order and do not track stock updates if not needed" in new OrdersSyncFSpecContext with Fixtures {
          override lazy val product = Factory.simpleProduct(merchant, trackInventory = Some(false)).create
          val productLocation = Factory.productLocation(product, london).create
          val originalStockQuantity = 50
          val stock = Factory.stock(productLocation, quantity = Some(originalStockQuantity)).create

          val orderId = UUID.randomUUID

          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            quantity = Some(2),
            productId = Some(product.id),
          )

          val upsertion = baseOrderUpsertion.copy(
            customerId = None,
            items = Seq(orderItemUpsertion),
            assignedUserIds = Some(Seq(user.id)),
          )

          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion, number = Some("1"))
          }

          // give some time to process the messages
          Thread.sleep(100)

          afterAWhile {
            val updatedStock = stockDao.findById(stock.id).await.get
            updatedStock.quantity ==== originalStockQuantity
          }

          afterAWhile {
            val history = productQuantityHistoryDao.findAllByProductIds(Seq(product.id)).await
            history.isEmpty should beTrue
          }
        }

        "if the customer id is given" should {
          "link customer to order and location" in new OrdersSyncFSpecContext with Fixtures {
            val loyaltyProgram = Factory.loyaltyProgram(merchant, locations = Seq(london)).create
            val totalItemPriceAmount = BigDecimal(27)
            val orderId = UUID.randomUUID
            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              totalPriceAmount = Some(totalItemPriceAmount),
              paymentStatus = Some(PaymentStatus.Paid),
              discounts = Seq.empty,
              modifierOptions = Seq.empty,
              taxRates = Seq.empty,
              variantOptions = Seq.empty,
            )
            val upsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Paid,
              status = OrderStatus.Completed,
              items = Seq(itemUpsertion),
              totalAmount = totalItemPriceAmount,
            )
            val expectedTotalSpend = totalItemPriceAmount
            Post(s"/v1/orders.sync?order_id=${orderId}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              val orderRecord = orderDao.findById(orderResponse.id).await.get

              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(
                globalCustomer.id,
                london.id,
                totalVisits = 1,
                totalSpend = expectedTotalSpend,
              )
              assertCustomerIsNotEnrolled(orderRecord.customerId.get, loyaltyProgram)
            }
          }

          "include taxes and delivery fees in total spend amount" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID

            val totalItemPriceAmount = genBigDecimal.instance
            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              taxAmount = Some(0),
              discountAmount = Some(0),
              totalPriceAmount = Some(10),
              paymentStatus = Some(PaymentStatus.Paid),
              discounts = Seq.empty,
              modifierOptions = Seq.empty,
              taxRates = Seq.empty,
              variantOptions = Seq.empty,
            )
            val upsertion = baseOrderUpsertion.copy(
              paymentStatus = PaymentStatus.Paid,
              status = OrderStatus.Completed,
              items = Seq(itemUpsertion),
              subtotalAmount = BigDecimal(10),
              discountAmount = Some(BigDecimal(0)),
              taxAmount = BigDecimal(3),
              deliveryFeeAmount = Some(BigDecimal(2)),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=${orderId}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              val orderRecord = orderDao.findById(orderResponse.id).await.get

              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(
                globalCustomer.id,
                london.id,
                totalVisits = 1,
                totalSpend = BigDecimal(15),
              )
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
            val newEmail = randomEmail
            Factory.globalCustomerWithEmail(merchant = Some(merchant), email = Some(newEmail)).create

            val order1 = Factory
              .order(
                merchant,
                location = Some(london),
                globalCustomer = Some(globalCustomer),
                totalAmount = Some(BigDecimal(10)),
              )
              .create
            val orderItem =
              Factory.orderItem(order1, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(10)).create
            val orderItemRefunded =
              Factory
                .orderItem(order1, paymentStatus = Some(PaymentStatus.Refunded), totalPriceAmount = Some(15))
                .create
            val previousSpend = 10

            val orderPending = Factory
              .order(
                merchant,
                location = Some(london),
                globalCustomer = Some(globalCustomer),
                totalAmount = Some(10),
                status = Some(OrderStatus.Completed),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create

            val orderInProgress =
              Factory
                .order(
                  merchant,
                  location = Some(london),
                  globalCustomer = Some(globalCustomer),
                  status = Some(OrderStatus.InProgress),
                  paymentStatus = Some(PaymentStatus.Pending),
                )
                .create

            val orderCanceled =
              Factory
                .order(
                  merchant,
                  location = Some(london),
                  globalCustomer = Some(globalCustomer),
                  status = Some(OrderStatus.Canceled),
                  paymentStatus = Some(PaymentStatus.Pending),
                )
                .create

            val orderId = UUID.randomUUID

            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
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
                  orderItemIds = Seq(itemUpsertion.id),
                ),
              ),
              items = Seq(itemUpsertion),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(
                globalCustomer.id,
                london.id,
                totalVisits = 4,
                totalSpend = previousSpend + 15,
              )
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }
          }

          "includes delivery fees and taxes in the loyalty amount" in new LoyaltyAwardFixtures {
            val orderId = UUID.randomUUID

            val totalItemPriceAmount = genBigDecimal.instance
            val itemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(product.id),
              taxAmount = Some(0),
              discountAmount = Some(0),
              totalPriceAmount = Some(10),
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
                  orderItemIds = Seq(itemUpsertion.id),
                ),
              ),
              subtotalAmount = BigDecimal(10),
              discountAmount = Some(BigDecimal(0)),
              tipAmount = Some(BigDecimal(2)),
              taxAmount = BigDecimal(3),
              deliveryFeeAmount = Some(BigDecimal(2)),
              items = Seq(itemUpsertion),
              totalAmount = BigDecimal(15),
            )

            Post(s"/v1/orders.sync?order_id=${orderId}", upsertion)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              val orderRecord = orderDao.findById(orderResponse.id).await.get

              assertUpsertion(orderResponse, upsertion, customerId = Some(globalCustomer.id))
              assertCustomerLocationUpsertion(
                globalCustomer.id,
                london.id,
                totalVisits = 1,
                totalSpend = BigDecimal(15),
              )
              assertCustomerEarnedLoyaltyPoints(globalCustomer.id, loyaltyProgram, 15)
            }
          }
        }

        "if a gift card pass creation is needed" should {

          "reject request if item price amount is missing" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = None,
            )

            val upsertion = baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("GiftCardPassWithoutPrice")
            }
          }

          "reject request if merchant has no gift card" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID
            val giftCardProduct = Factory.giftCardProduct(merchant).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(10),
            )

            val upsertion = baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("GiftCardPassWithoutGiftCard")
            }
          }

          "do not create the pass if status is not paid" in new OrdersSyncFSpecContext with Fixtures {
            val orderId = UUID.randomUUID
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Pending),
              priceAmount = Some(10),
            )

            val upsertion = baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))

              assertNoPassAttached(orderItemUpsertion.id)
            }
          }

          "create the pass with predefined amount if status is changing to paid" in new OrdersSyncFSpecContext
            with Fixtures {
            val orderId = UUID.randomUUID

            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct, amounts = Some(Seq(1, 2, 3, 4))).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(1),
            )

            val upsertion = baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))

              assertGiftCardPassCreation(orderItemUpsertion, giftCard, isCustomAmount = false)
            }

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))

              assertPassAttached(orderItemUpsertion.id)
            }
          }

          "create the pass with custom amount if status is changing to paid" in new OrdersSyncFSpecContext
            with Fixtures {
            val orderId = UUID.randomUUID
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct, amounts = Some(Seq(1, 2, 3, 4))).create

            val orderItemUpsertion = randomOrderItemUpsertion().copy(
              id = UUID.randomUUID,
              productId = Some(giftCardProduct.id),
              paymentStatus = Some(PaymentStatus.Paid),
              priceAmount = Some(10),
            )

            val upsertion = baseOrderUpsertion.copy(items = Seq(orderItemUpsertion))

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))

              assertGiftCardPassCreation(orderItemUpsertion, giftCard, isCustomAmount = true)
            }

            Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderResponse = responseAs[ApiResponse[OrderEntity]].data
              assertUpsertion(orderResponse, upsertion, number = Some("1"))

              assertPassAttached(orderItemUpsertion.id)
            }
          }
        }
      }

      "with a custom product" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val orderId = UUID.randomUUID
          val upsertion = baseOrderUpsertion.copy(
            items = Seq(
              randomOrderItemUpsertion()
                .copy(id = UUID.randomUUID, productId = None, productType = Some(ArticleType.CustomProduct)),
            ),
          )
          Post(s"/v1/orders.sync?order_id=${orderId}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)
          }
        }
      }

      "even if a product in an order item does not belong to the merchant" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val orderId = UUID.randomUUID
          val upsertion = baseOrderUpsertion.copy(
            items = Seq(randomOrderItemUpsertion().copy(id = UUID.randomUUID, productId = Some(UUID.randomUUID))),
          )
          Post(s"/v1/orders.sync?order_id=${orderId}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
          }
        }
      }

      "even if the order does not belong to the merchant" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val competitor = Factory.merchant.create
          val competitorOrder = Factory.order(competitor).create

          val upsertion = baseOrderUpsertion

          Post(s"/v1/orders.sync?order_id=${competitorOrder.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
          }
        }
      }

      "even if all the ids are random" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val randomId = UUID.randomUUID
          val orderItemUpsertion = randomOrderItemUpsertion().copy(
            id = UUID.randomUUID,
            productId = Some(UUID.randomUUID),
            discounts = Seq(random[ItemDiscountUpsertion].copy(discountId = Some(UUID.randomUUID))),
            modifierOptions =
              Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(UUID.randomUUID))),
            variantOptions = Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(UUID.randomUUID))),
          )
          val upsertion = randomOrderUpsertion().copy(
            customerId = Some(UUID.randomUUID),
            source = Some(Source.Register),
            paymentTransactions =
              Seq(random[PaymentTransactionUpsertion].copy(id = UUID.randomUUID, refundedPaymentTransactionId = None)),
            items = Seq(orderItemUpsertion),
            assignedUserIds = Some(Seq(UUID.randomUUID, UUID.randomUUID, UUID.randomUUID)),
            deliveryAddress = None,
            onlineOrderAttribute = None,
            bundles = Seq.empty,
          )

          Post(s"/v1/orders.sync?order_id=$randomId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            orderResponse.number must beNone
          }
        }
      }

      "even if a location has been soft-deleted" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val newYork = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          val orderId = UUID.randomUUID
          val upsertion = baseOrderUpsertion.copy(
            locationId = newYork.id,
            items = Seq(
              randomOrderItemUpsertion()
                .copy(id = UUID.randomUUID, productId = Some(product.id), taxRates = Seq.empty),
            ),
            taxRates = Seq.empty,
          )
          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)
          }
        }
      }

      "even if a user has been soft-deleted" should {
        "it should save the order" in new OrdersSyncFSpecContext with Fixtures {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create
          val orderId = UUID.randomUUID
          val upsertion = baseOrderUpsertion.copy(
            creatorUserId = Some(deletedUser.id),
            locationId = rome.id,
          )
          Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)
          }
        }
      }
    }

    "if request has valid app token" should {
      "sync the order" in new OrdersSyncFSpecContext with Fixtures with AppTokenFixtures {
        val orderId = UUID.randomUUID

        Post(s"/v1/orders.sync?order_id=$orderId", baseOrderUpsertion)
          .addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          assertUpsertion(orderResponse, baseOrderUpsertion)
        }
      }
    }

    "if request has invalid token" should {
      "reject the request" in new OrdersSyncFSpecContext with Fixtures {
        val orderId = UUID.randomUUID

        Post(s"/v1/orders.sync?order_id=$orderId", baseOrderUpsertion)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
