package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire.wire

import io.paytouch.core.async.sqs.SQSMessageSender
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.errors
import io.paytouch.core.errors._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ PaytouchLogger, UtcTime, FixtureDaoFactory => Factory }
import io.paytouch.core.validators.ProductValidatorIncludingDeleted
import io.paytouch.utils.Tagging._

@scala.annotation.nowarn("msg=Auto-application")
class OrderSyncValidatedSpec extends ServiceDaoSpec {
  private val ZeroDiscountRecoveryMsg = "Filtered out zero discount"
  private val ItemDiscountRecoveryMsg = "Item discount id already taken, generating a new one"
  private val DeliveryAddressRecoveryMsg = "While recovering delivery address id not accessible"

  abstract class OrderSyncValidatedSpecContext extends ServiceDaoSpecContext {
    implicit val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val logger: PaytouchLogger = spy(new PaytouchLogger)
    val service: OrderSyncService = wire[OrderSyncService]

    lazy val product = Factory.simpleProduct(merchant, trackInventory = Some(false)).create
    lazy val cashier = user

    val productLocation = Factory.productLocation(product, london).create
    val stock = Factory.stock(productLocation, quantity = Some(10), sellOutOfStock = Some(false)).create

    val inventoryProduct = Factory.simpleProduct(merchant, trackInventory = Some(true)).create
    val inventoryProductLocation = Factory.productLocation(inventoryProduct, london).create
    val inventoryStock =
      Factory.stock(inventoryProductLocation, quantity = Some(10), sellOutOfStock = Some(false)).create

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
    lazy val bundle = Factory.comboProduct(merchant).create

    val orderItemDiscountUpsertion = random[ItemDiscountUpsertion].copy(discountId = Some(discount.id))
    val orderItemUpsertion = randomOrderItemUpsertion().copy(
      quantity = Some(2),
      productId = Some(product.id),
      productName = Some(product.name),
      productDescription = product.description,
      unit = Some(product.unit),
      discounts = Seq(orderItemDiscountUpsertion),
      modifierOptions = Seq(random[OrderItemModifierOptionUpsertion].copy(modifierOptionId = Some(modifierOption.id))),
      taxRates = Seq(random[OrderItemTaxRateUpsertion].copy(id = None, taxRateId = Some(taxRate.id))),
      variantOptions = Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption.id))),
      paymentStatus = Some(genPositivePaymentStatus.instance),
    )

    val deliveryAddressUpsertion = random[DeliveryAddressUpsertion]

    val paymentTypeValues = TransactionPaymentType.values

    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val giftCard = Factory.giftCard(giftCardProduct).create
    val order = Factory.order(merchant).create
    val orderItem = Factory.orderItem(order, Some(giftCardProduct)).create
    val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
    val giftCardPassTransaction = Factory.giftCardPassTransaction(giftCardPass).create
    lazy val bundleSet = Factory.bundleSet(bundle).create
    lazy val bundleOption = Factory.bundleOption(bundleSet, product).create

    val validPaymentDetails = random[PaymentDetails].copy(
      giftCardPassId = Some(giftCardPass.id),
      giftCardPassTransactionId = Some(giftCardPassTransaction.id),
    )

    lazy val orderBundleSet_bundleSetId = bundleSet.id
    lazy val orderBundleSetUpsertion = random[OrderBundleSetUpsertion].copy(
      id = UUID.randomUUID,
      bundleSetId = orderBundleSet_bundleSetId,
      orderBundleOptions = Seq(orderBundleOptionUpsertion),
    )
    lazy val orderBundleOption_bundleOptionId = bundleOption.id
    lazy val orderBundleOption_articleOrderItemId = orderItemUpsertion.id
    lazy val orderBundleOptionUpsertion = random[OrderBundleOptionUpsertion].copy(
      id = UUID.randomUUID,
      bundleOptionId = orderBundleOption_bundleOptionId,
      articleOrderItemId = orderBundleOption_articleOrderItemId,
    )
    lazy val orderBundleUpsertion_bundleOrderItemId = orderItemUpsertion.id
    lazy val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
      id = UUID.randomUUID,
      bundleOrderItemId = orderBundleUpsertion_bundleOrderItemId,
      orderBundleSets = Seq(orderBundleSetUpsertion),
    )

    def createUpsertion(
        paymentDetails: PaymentDetails = validPaymentDetails,
        orderItemUpsertion: OrderItemUpsertion = orderItemUpsertion,
        refundedPaymentTransactionId: Option[UUID] = None,
        orderDiscountId: Option[UUID] = None,
        deliveryAddressUpsertion: Option[DeliveryAddressUpsertion] = None,
      ) = {
      val paymentTransactionUpsertions = paymentTypeValues.map { paymentTypeValue =>
        random[PaymentTransactionUpsertion].copy(
          id = UUID.randomUUID,
          orderItemIds = Seq(orderItemUpsertion.id),
          refundedPaymentTransactionId = refundedPaymentTransactionId,
          paymentType = Some(paymentTypeValue),
          paymentDetails = Some(paymentDetails),
          fees = Seq.empty,
        )
      }

      randomOrderUpsertion().copy(
        creatorUserId = Some(cashier.id),
        locationId = london.id,
        items = Seq(orderItemUpsertion),
        merchantNotes = Seq(random[MerchantNoteUpsertion].copy(userId = user.id)),
        paymentTransactions = paymentTransactionUpsertions,
        assignedUserIds = Some(Seq(user.id, anotherUser.id)),
        taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = taxRate.id)),
        discounts = Seq(random[ItemDiscountUpsertion].copy(id = orderDiscountId, discountId = Some(discount2.id))),
        deliveryAddress = deliveryAddressUpsertion,
        status = genPositiveOrderStatus.instance,
      )
    }
  }

  "OrderSyncValidatedSpec" in {

    "validatedSyncById" in {
      "if the order doesn't exist" should {

        "don't return any error if the order is valid" in new OrderSyncValidatedSpecContext {
          service.validatedSyncById(orderId, createUpsertion()).await.success
        }

        "reject items with non existent product id" in new OrderSyncValidatedSpecContext {
          val productValidatorIncludingDeleted = new ProductValidatorIncludingDeleted
          private val invalidId = UUID.randomUUID
          val expected = Seq(productValidatorIncludingDeleted.validationErrorF(Seq(invalidId)))

          service
            .validatedSyncById(
              orderId,
              createUpsertion(orderItemUpsertion = orderItemUpsertion.copy(productId = Some(invalidId))),
            )
            .await
            .failures ==== expected
        }

        "gift card pass ids" in {
          "reject invalid gift card pass ids" in new OrderSyncValidatedSpecContext {
            private val invalidId = UUID.randomUUID
            val paymentDetails = validPaymentDetails.copy(
              giftCardPassId = Some(invalidId),
            )
            val expected = NonAccessibleGiftCardPassIds(Seq(invalidId))

            service.validatedSyncById(orderId, createUpsertion(paymentDetails)).await.failures.headOption ==== Some(
              expected,
            )
          }

          "reject invalid gift card pass transaction ids" in new OrderSyncValidatedSpecContext {
            private val invalidId = UUID.randomUUID
            val paymentDetails = validPaymentDetails.copy(
              giftCardPassTransactionId = Some(invalidId),
            )
            val expected = NonAccessibleGiftCardPassTransactionIds(Seq(invalidId))

            service.validatedSyncById(orderId, createUpsertion(paymentDetails)).await.failures.headOption ==== Some(
              expected,
            )
          }
        }

        "refunded transaction ids" in {
          "reject competitor refunded transaction ids" in new OrderSyncValidatedSpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val paymentTransactionTaken = Factory.paymentTransaction(competitorOrder).create

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(paymentTransactionTaken.id))
            val expected = NonAccessibleRefundedPaymentTransactionIds(Seq(paymentTransactionTaken.id))

            service.validatedSyncById(orderId, upsertion).await.failures.headOption ==== Some(expected)
          }

          "reject random refunded transaction ids" in new OrderSyncValidatedSpecContext {
            val refundedId = UUID.randomUUID

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(refundedId))
            val expected = NonAccessibleRefundedPaymentTransactionIds(Seq(refundedId))

            service.validatedSyncById(orderId, upsertion).await.failures.headOption ==== Some(expected)
          }

          "allow existing refunded transaction ids" in new OrderSyncValidatedSpecContext {
            val previousPaymentTransaction = Factory.paymentTransaction(order).create

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(previousPaymentTransaction.id))
            service.validatedSyncById(orderId, upsertion).await.success
          }

          "allow refunded transaction ids not yet in the db" in new OrderSyncValidatedSpecContext {
            val updatedUpsertion = {
              val upsertion = createUpsertion()
              val paymentTransaction2 = upsertion.paymentTransactions(1)
              val paymentTransaction1 = upsertion
                .paymentTransactions(0)
                .copy(
                  refundedPaymentTransactionId = Some(paymentTransaction2.id),
                )
              val remainingTransactions = upsertion.paymentTransactions.drop(2)

              val updatedPaymentTransactions = Seq(paymentTransaction1, paymentTransaction2) ++ remainingTransactions
              upsertion.copy(paymentTransactions = updatedPaymentTransactions)
            }

            service.validatedSyncById(orderId, updatedUpsertion).await.success
          }
        }

        "reward redemptions" in {
          "reject invalid reward redemptions" in new OrderSyncValidatedSpecContext {
            private val invalidId = UUID.randomUUID

            val rewardRedemptionSync = random[RewardRedemptionSync].copy(rewardRedemptionId = invalidId)
            val expected = InvalidRewardRedemptionIds(Seq(invalidId))

            service
              .validatedSyncById(orderId, createUpsertion().copy(rewards = Seq(rewardRedemptionSync)))
              .await
              .failures
              .headOption ==== Some(expected)
          }

          "reject already associated reward redemptions" in new OrderSyncValidatedSpecContext {
            val customer = Factory.globalCustomer().create
            val loyaltyProgram = Factory.loyaltyProgram(merchant).create
            val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create
            val loyaltyMembership = Factory.loyaltyMembership(customer, loyaltyProgram).create
            val rewardRedemption =
              Factory.rewardRedemption(loyaltyMembership, loyaltyReward, orderId = Some(UUID.randomUUID)).create

            val rewardRedemptionSync = random[RewardRedemptionSync].copy(rewardRedemptionId = rewardRedemption.id)
            val expected =
              RewardRedemptionAlreadyAssociated(rewardRedemption.id, rewardRedemption.orderId, orderId)

            service
              .validatedSyncById(orderId, createUpsertion().copy(rewards = Seq(rewardRedemptionSync)))
              .await
              .failures
              .headOption ==== Some(expected)
          }
        }

        "stock" in {
          "trackInventory = true" in {
            "reject orders where the stock is less than the requested quantity" in new OrderSyncValidatedSpecContext {
              val updatedOrderItemUpsertion = orderItemUpsertion.copy(
                quantity = Some(20),
                productId = Some(inventoryProduct.id),
              )
              val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)
              val expected = Seq(ProductOutOfStock(inventoryProduct.id))

              service.validatedSyncById(orderId, upsertion).await.failures ==== expected
            }

            "allow orders where the stock is equal to the requested quantity" in new OrderSyncValidatedSpecContext {
              val updatedOrderItemUpsertion = orderItemUpsertion.copy(
                quantity = Some(10),
                productId = Some(inventoryProduct.id),
              )
              val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

              service.validatedSyncById(orderId, upsertion).await.success
            }
          }

          "trackInventory = false" in {
            "allow orders where the stock is less than the requested quantity" in new OrderSyncValidatedSpecContext {
              val updatedOrderItemUpsertion = orderItemUpsertion.copy(
                quantity = Some(20),
              )
              val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

              service.validatedSyncById(orderId, upsertion).await.success
            }

            "allow orders where the stock is equal to the requested quantity" in new OrderSyncValidatedSpecContext {
              val updatedOrderItemUpsertion = orderItemUpsertion.copy(
                quantity = Some(10),
              )
              val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

              service.validatedSyncById(orderId, upsertion).await.success
            }
          }
        }

        "order discount ids" in {
          "allow order discount id not yet in the db" in new OrderSyncValidatedSpecContext {
            val orderDiscountId = Some(UUID.randomUUID)
            val upsertion = createUpsertion(orderDiscountId = orderDiscountId)

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "allow existing order discount id" in new OrderSyncValidatedSpecContext {
            val orderDiscount = Factory.orderDiscount(order, discount).create
            val orderDiscountId = Some(orderDiscount.id)
            val upsertion = createUpsertion(orderDiscountId = orderDiscountId)

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "reject order discount id that belongs to another merchant" in new OrderSyncValidatedSpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val competitorDiscount = Factory.discount(competitor).create
            val competitorOrderDiscount = Factory.orderDiscount(competitorOrder, competitorDiscount).create
            val competitorOrderDiscountId = Some(competitorOrderDiscount.id)
            val upsertion = createUpsertion(orderDiscountId = competitorOrderDiscountId)

            val expected = Seq(NonAccessibleOrderDiscountIds(Seq(competitorOrderDiscount.id)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }

          "allow order item discount id not yet in the db" in new OrderSyncValidatedSpecContext {
            val orderItemDiscountId = Some(UUID.randomUUID)
            val updatedOrderItemUpsertion = orderItemUpsertion.copy(
              discounts = Seq(orderItemDiscountUpsertion.copy(id = orderItemDiscountId)),
            )
            val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "allow existing order item discount id" in new OrderSyncValidatedSpecContext {
            val orderItemDiscount = Factory.orderItemDiscount(orderItem, discount).create
            val orderItemDiscountId = Some(orderItemDiscount.id)
            val updatedOrderItemUpsertion = orderItemUpsertion.copy(
              id = orderItem.id,
              discounts = Seq(orderItemDiscountUpsertion.copy(id = orderItemDiscountId)),
            )
            val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "reject order item discount id that belongs to another merchant" in new OrderSyncValidatedSpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val competitorOrderItem = Factory.orderItem(competitorOrder).create
            val competitorDiscount = Factory.discount(competitor).create
            val competitorOrderItemDiscount = Factory.orderItemDiscount(competitorOrderItem, competitorDiscount).create
            val competitorOrderItemDiscountId = Some(competitorOrderItemDiscount.id)
            val updatedOrderItemUpsertion = orderItemUpsertion.copy(
              id = orderItem.id,
              discounts = Seq(orderItemDiscountUpsertion.copy(id = competitorOrderItemDiscountId)),
            )
            val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)
            val expected = Seq(NonAccessibleOrderItemDiscountIds(Seq(competitorOrderItemDiscount.id)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }
        }

        "delivery address ids" in {
          "allow delivery address id not yet in the db" in new OrderSyncValidatedSpecContext {
            val deliveryAddressId = UUID.randomUUID
            val upsertion =
              createUpsertion(deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = deliveryAddressId)))

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "allow existing delivery address id" in new OrderSyncValidatedSpecContext {
            val deliveryAddress = Factory.orderDeliveryAddress(merchant).create
            val upsertion =
              createUpsertion(deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = deliveryAddress.id)))

            service.validatedSyncById(orderId, upsertion).await.success
          }

          "reject delivery address id that belongs to another merchant" in new OrderSyncValidatedSpecContext {
            val competitor = Factory.merchant.create
            val competitorDeliveryAddress = Factory.orderDeliveryAddress(competitor).create
            val upsertion =
              createUpsertion(
                deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = competitorDeliveryAddress.id)),
              )
            val expected = Seq(NonAccessibleOrderDeliveryAddressIds(Seq(competitorDeliveryAddress.id)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }
        }

        "online order attribute" in {
          "reject if source = storefront and online order attribute is none" in new OrderSyncValidatedSpecContext {
            val upsertion = createUpsertion().copy(source = Some(Source.Storefront))

            val expected = Seq(MissingOnlineOrderAttributes())

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }
        }

        "bundles" in {
          "reject if bundle contains reference to nonexisting bundle order item id" in new OrderSyncValidatedSpecContext {
            override lazy val orderBundleUpsertion_bundleOrderItemId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))
            val expected = Seq(InvalidOrderItemIds(Seq(orderBundleUpsertion_bundleOrderItemId)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }

          "reject if bundle contains reference to nonexisting article order item id" in new OrderSyncValidatedSpecContext {
            override lazy val orderBundleOption_articleOrderItemId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))
            val expected = Seq(InvalidOrderItemIds(Seq(orderBundleOption_articleOrderItemId)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }

          "reject if bundle contains reference to competitor order bundle id" in new OrderSyncValidatedSpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val competitorOrderItem = Factory.orderItem(competitorOrder).create
            val competitorOrderBundle =
              Factory.orderBundle(competitorOrder, competitorOrderItem, competitorOrderItem).create
            val bundleUpsertion = orderBundleUpsertion.copy(id = competitorOrderBundle.id)
            val upsertion = createUpsertion().copy(bundles = Seq(bundleUpsertion))
            val expected = Seq(NonAccessibleOrderBundleIds(Seq(bundleUpsertion.id)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }

          "reject if bundle contains reference to an invalid bundle set id" in new OrderSyncValidatedSpecContext {
            override lazy val orderBundleSet_bundleSetId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))
            val expected = Seq(InvalidBundleSetIds(Seq(orderBundleSet_bundleSetId)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }

          "reject if bundle contains reference to an invalid bundle option id" in new OrderSyncValidatedSpecContext {
            override lazy val orderBundleOption_bundleOptionId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))
            val expected = Seq(InvalidBundleOptionIds(Seq(orderBundleOption_bundleOptionId)))

            service.validatedSyncById(orderId, upsertion).await.failures ==== expected
          }
        }
      }
    }
  }
}
