package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire.wire
import io.paytouch.core.async.sqs.SQSMessageSender

import io.paytouch.core.data.model.enums.{ Source, TransactionPaymentType }
import io.paytouch.core.entities._
import io.paytouch.core.errors
import io.paytouch.core.errors._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ PaytouchLogger, UtcTime, Multiple, FixtureDaoFactory => Factory }
import io.paytouch.core.validators.ProductValidatorIncludingDeleted
import io.paytouch.utils.Tagging._

@scala.annotation.nowarn("msg=Auto-application")
class OrderSyncRecoverySpec extends ServiceDaoSpec {
  private val ZeroDiscountRecoveryMsg = "Filtered out zero discount"
  private val ItemDiscountRecoveryMsg = "Item discount id already taken, generating a new one"
  private val DeliveryAddressRecoveryMsg = "While recovering delivery address id not accessible"

  abstract class OrderSyncRecoverySpecContext extends ServiceDaoSpecContext {
    implicit val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val logger: PaytouchLogger = spy(new PaytouchLogger)
    val service: OrderSyncService = wire[OrderSyncService]

    lazy val product = Factory.simpleProduct(merchant).create
    lazy val cashier = user

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
      taxRates = Seq(random[OrderItemTaxRateUpsertion].copy(id = Some(UUID.randomUUID), taxRateId = Some(taxRate.id))),
      variantOptions = Seq(random[OrderItemVariantOptionUpsertion].copy(variantOptionId = Some(variantOption.id))),
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
        taxRates = Seq(random[OrderTaxRateUpsertion].copy(id = None, taxRateId = taxRate.id)),
        discounts = Seq(random[ItemDiscountUpsertion].copy(id = orderDiscountId, discountId = Some(discount2.id))),
        deliveryAddress = deliveryAddressUpsertion,
      )
    }

    def expectLoggedGenericRecoverOptUUID(expectedError: errors.Error) = {
      val expected = Multiple.failure[Option[UUID]](expectedError)
      there was atLeastOne(logger)
        .loggedGenericRecover(argThat(===(expected)), argThat(===(None)))(any[String], any[AnyRef])
    }

    def expectLoggedGenericRecoverOpt[T](expectedError: errors.Error) = {
      val expected = Multiple.failure[Option[T]](expectedError)
      there was atLeastOne(logger)
        .loggedGenericRecover(argThat(===(expected)), argThat(===(None)))(any[String], any[AnyRef])
    }

    def exceptNoRecovery(validId: Option[UUID]) = {
      val expected = Multiple.success(validId)
      there was atLeastOne(logger)
        .loggedGenericRecover(argThat(===(expected)), argThat(===(None)))(any[String], any[AnyRef])
    }

    def expectLoggedSoftRecover(expectedError: errors.Error, description: String) = {
      val expected = Multiple.failure[Option[UUID]](expectedError)
      there was atLeastOne(logger).loggedGenericSoftRecover(expected, None)(description)
    }

    def expectLoggedSoftRecoverUuid(expectedError: errors.Error, description: String) = {
      val expected = Multiple.failure[UUID](expectedError)
      there was atLeastOne(logger).loggedGenericSoftRecover(===(expected), any[UUID])(contain(description))
    }

    def exceptNoSoftRecovery[T](validEntity: Option[T], description: String) = {
      val expected = Multiple.success(validEntity)
      there was atLeastOne(logger).loggedGenericSoftRecover(expected, None)(description)
    }

    def exceptNoSoftRecovery[T](validId: UUID, description: String) = {
      val expected = Multiple.success(validId)
      there was atLeastOne(logger).loggedGenericSoftRecover(===(expected), any[UUID])(contain(description))
    }
  }

  "OrderSyncRecoverySpec" in {
    "syncById" in {
      "if the order doesn't exist" should {

        "don't do any logging if the order is valid" in new OrderSyncRecoverySpecContext {
          service.syncById(orderId, createUpsertion()).await

          there was no(logger).warn(anyString)
          there was no(logger).info(anyString)
        }

        "gift card pass ids" in {
          "log and recover invalid gift card pass ids" in new OrderSyncRecoverySpecContext {
            private val invalidId = UUID.randomUUID
            val paymentDetails = validPaymentDetails.copy(
              giftCardPassId = Some(invalidId),
            )

            service.syncById(orderId, createUpsertion(paymentDetails)).await

            expectLoggedGenericRecoverOptUUID(NonAccessibleGiftCardPassIds(Seq(invalidId)))
          }

          "log and recover invalid gift card pass transaction ids" in new OrderSyncRecoverySpecContext {
            private val invalidId = UUID.randomUUID
            val paymentDetails = validPaymentDetails.copy(
              giftCardPassTransactionId = Some(invalidId),
            )

            service.syncById(orderId, createUpsertion(paymentDetails)).await

            expectLoggedGenericRecoverOptUUID(NonAccessibleGiftCardPassTransactionIds(Seq(invalidId)))
          }
        }

        "refunded transaction ids" in {
          "log and recover competitor refunded transaction ids" in new OrderSyncRecoverySpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val paymentTransactionTaken = Factory.paymentTransaction(competitorOrder).create

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(paymentTransactionTaken.id))
            service.syncById(orderId, upsertion).await

            expectLoggedGenericRecoverOptUUID(
              NonAccessibleRefundedPaymentTransactionIds(Seq(paymentTransactionTaken.id)),
            )
          }

          "recover random refunded transaction ids" in new OrderSyncRecoverySpecContext {
            val refundedId = UUID.randomUUID

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(refundedId))
            service.syncById(orderId, upsertion).await

            expectLoggedGenericRecoverOptUUID(NonAccessibleRefundedPaymentTransactionIds(Seq(refundedId)))
          }

          "do not recover existing refunded transaction ids" in new OrderSyncRecoverySpecContext {
            val previousPaymentTransaction = Factory.paymentTransaction(order).create

            val upsertion = createUpsertion(refundedPaymentTransactionId = Some(previousPaymentTransaction.id))
            service.syncById(orderId, upsertion).await

            exceptNoRecovery(Some(previousPaymentTransaction.id))
          }

          "do not recover refunded transaction ids not yet in the db" in new OrderSyncRecoverySpecContext {
            val updatedUpsertion = {
              val upsertion = createUpsertion()
              val paymentTransaction2 = upsertion.paymentTransactions(1)
              val paymentTransaction1 = upsertion
                .paymentTransactions
                .head
                .copy(
                  refundedPaymentTransactionId = Some(paymentTransaction2.id),
                )
              val remainingTransactions = upsertion.paymentTransactions.drop(2)

              val updatedPaymentTransactions = Seq(paymentTransaction1, paymentTransaction2) ++ remainingTransactions
              upsertion.copy(paymentTransactions = updatedPaymentTransactions)
            }
            service.syncById(orderId, updatedUpsertion).await

            updatedUpsertion.paymentTransactions.map { paymentTransaction =>
              exceptNoRecovery(paymentTransaction.refundedPaymentTransactionId)
            }
          }
        }

        "log and recover items with non existent product id" in new OrderSyncRecoverySpecContext {
          val productValidatorIncludingDeleted = new ProductValidatorIncludingDeleted
          private val invalidId = UUID.randomUUID

          service
            .syncById(
              orderId,
              createUpsertion(orderItemUpsertion = orderItemUpsertion.copy(productId = Some(invalidId))),
            )
            .await

          expectLoggedGenericRecoverOptUUID(productValidatorIncludingDeleted.validationErrorF(Seq(invalidId)))
        }

        "log and recover invalid reward redemptions" in new OrderSyncRecoverySpecContext {
          private val invalidId = UUID.randomUUID

          val rewardRedemptionSync = random[RewardRedemptionSync].copy(rewardRedemptionId = invalidId)
          service
            .syncById(orderId, createUpsertion().copy(rewards = Seq(rewardRedemptionSync)))
            .await

          expectLoggedGenericRecoverOptUUID(InvalidRewardRedemptionIds(Seq(invalidId)))
        }

        "log and recover already associated reward redemptions" in new OrderSyncRecoverySpecContext {
          val customer = Factory.globalCustomer().create
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create
          val loyaltyMembership = Factory.loyaltyMembership(customer, loyaltyProgram).create
          val rewardRedemption =
            Factory.rewardRedemption(loyaltyMembership, loyaltyReward, orderId = Some(UUID.randomUUID)).create

          val rewardRedemptionSync = random[RewardRedemptionSync].copy(rewardRedemptionId = rewardRedemption.id)
          service
            .syncById(orderId, createUpsertion().copy(rewards = Seq(rewardRedemptionSync)))
            .await

          expectLoggedGenericRecoverOptUUID(
            RewardRedemptionAlreadyAssociated(rewardRedemption.id, rewardRedemption.orderId, orderId),
          )
        }

        "order discount ids" in {
          "do not recover order discount id not yet in the db" in new OrderSyncRecoverySpecContext {
            val orderDiscountId = Some(UUID.randomUUID)
            val upsertion = createUpsertion(orderDiscountId = orderDiscountId)

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(orderDiscountId, ItemDiscountRecoveryMsg)
          }

          "do not recover existing order discount id" in new OrderSyncRecoverySpecContext {
            val orderDiscount = Factory.orderDiscount(order, discount).create
            val orderDiscountId = Some(orderDiscount.id)
            val upsertion = createUpsertion(orderDiscountId = orderDiscountId)

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(orderDiscountId, ItemDiscountRecoveryMsg)
          }

          "recover order discount id that belongs to another merchant" in new OrderSyncRecoverySpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val competitorDiscount = Factory.discount(competitor).create
            val competitorOrderDiscount = Factory.orderDiscount(competitorOrder, competitorDiscount).create
            val competitorOrderDiscountId = Some(competitorOrderDiscount.id)
            val upsertion = createUpsertion(orderDiscountId = competitorOrderDiscountId)

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(upsertion.discounts.headOption, ZeroDiscountRecoveryMsg)
            expectLoggedSoftRecover(
              NonAccessibleOrderDiscountIds(Seq(competitorOrderDiscount.id)),
              ItemDiscountRecoveryMsg,
            )
          }

          "do not recover order item discount id not yet in the db" in new OrderSyncRecoverySpecContext {
            val orderItemDiscountId = Some(UUID.randomUUID)
            val updatedOrderItemUpsertion = orderItemUpsertion.copy(
              discounts = Seq(orderItemDiscountUpsertion.copy(id = orderItemDiscountId)),
            )
            val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(orderItemDiscountId, ItemDiscountRecoveryMsg)
          }

          "do not recover existing order item discount id" in new OrderSyncRecoverySpecContext {
            val orderItemDiscount = Factory.orderItemDiscount(orderItem, discount).create
            val orderItemDiscountId = Some(orderItemDiscount.id)
            val updatedOrderItemUpsertion = orderItemUpsertion.copy(
              id = orderItem.id,
              discounts = Seq(orderItemDiscountUpsertion.copy(id = orderItemDiscountId)),
            )
            val upsertion = createUpsertion(orderItemUpsertion = updatedOrderItemUpsertion)

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(orderItemDiscountId, ItemDiscountRecoveryMsg)
          }

          "recover order item discount id that belongs to another merchant" in new OrderSyncRecoverySpecContext {
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

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(updatedOrderItemUpsertion.discounts.headOption, ZeroDiscountRecoveryMsg)
            expectLoggedSoftRecover(
              NonAccessibleOrderItemDiscountIds(Seq(competitorOrderItemDiscount.id)),
              ItemDiscountRecoveryMsg,
            )
          }
        }

        "delivery address ids" in {
          "do not recover delivery address id not yet in the db" in new OrderSyncRecoverySpecContext {
            val deliveryAddressId = UUID.randomUUID
            val upsertion =
              createUpsertion(deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = deliveryAddressId)))

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(deliveryAddressId, DeliveryAddressRecoveryMsg)
          }

          "do not recover existing delivery address id" in new OrderSyncRecoverySpecContext {
            val deliveryAddress = Factory.orderDeliveryAddress(merchant).create
            val upsertion =
              createUpsertion(deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = deliveryAddress.id)))

            service.syncById(orderId, upsertion).await

            exceptNoSoftRecovery(deliveryAddress.id, DeliveryAddressRecoveryMsg)
          }

          "recover delivery address id that belongs to another merchant" in new OrderSyncRecoverySpecContext {
            val competitor = Factory.merchant.create
            val competitorDeliveryAddress = Factory.orderDeliveryAddress(competitor).create
            val upsertion =
              createUpsertion(
                deliveryAddressUpsertion = Some(deliveryAddressUpsertion.copy(id = competitorDeliveryAddress.id)),
              )

            service.syncById(orderId, upsertion).await

            expectLoggedSoftRecoverUuid(
              NonAccessibleOrderDeliveryAddressIds(Seq(competitorDeliveryAddress.id)),
              DeliveryAddressRecoveryMsg,
            )
          }
        }

        "online order attribute" in {
          "log if source = storefront and online order attribute is none" in new OrderSyncRecoverySpecContext {
            val upsertion = createUpsertion().copy(source = Some(Source.Storefront))

            service.syncById(orderId, upsertion).await

            there was atLeastOne(logger)
              .recoverLog(
                argThat(===(s"Missing online order attributes for a storefront order $orderId")),
                argThat(===(upsertion)),
              )
          }
        }

        "bundles" in {
          "log if bundle contains reference to nonexisting bundle order item id" in new OrderSyncRecoverySpecContext {
            override lazy val orderBundleUpsertion_bundleOrderItemId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))

            service.syncById(orderId, upsertion).await

            expectLoggedGenericRecoverOpt[UUID](InvalidOrderItemIds(Seq(orderBundleUpsertion_bundleOrderItemId)))
          }

          "log if bundle contains reference to nonexisting article order item id" in new OrderSyncRecoverySpecContext {
            override lazy val orderBundleOption_articleOrderItemId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))

            service.syncById(orderId, upsertion).await

            expectLoggedGenericRecoverOpt[UUID](InvalidOrderItemIds(Seq(orderBundleOption_articleOrderItemId)))
          }

          "log if bundle contains reference to competitor order bundle id" in new OrderSyncRecoverySpecContext {
            val competitor = Factory.merchant.create
            val competitorOrder = Factory.order(competitor).create
            val competitorOrderItem = Factory.orderItem(competitorOrder).create
            val competitorOrderBundle =
              Factory.orderBundle(competitorOrder, competitorOrderItem, competitorOrderItem).create
            val bundleUpsertion = orderBundleUpsertion.copy(id = competitorOrderBundle.id)
            val upsertion = createUpsertion().copy(bundles = Seq(bundleUpsertion))

            service.syncById(orderId, upsertion).await

            expectLoggedSoftRecoverUuid(
              NonAccessibleOrderBundleIds(Seq(bundleUpsertion.id)),
              "Generating new bundle id",
            )
          }

          "log if bundle contains reference to an invalid bundle set id" in new OrderSyncRecoverySpecContext {
            override lazy val orderBundleSet_bundleSetId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))

            service.syncById(orderId, upsertion).await

            expectLoggedSoftRecoverUuid(
              InvalidBundleSetIds(Seq(orderBundleSet_bundleSetId)),
              "Setting bundle set id to None",
            )
          }

          "log if bundle contains reference to an invalid bundle option id" in new OrderSyncRecoverySpecContext {
            override lazy val orderBundleOption_bundleOptionId = UUID.randomUUID
            val upsertion = createUpsertion().copy(bundles = Seq(orderBundleUpsertion))

            service.syncById(orderId, upsertion).await

            expectLoggedSoftRecoverUuid(
              InvalidBundleOptionIds(Seq(orderBundleOption_bundleOptionId)),
              "Setting bundle option id to None",
            )
          }
        }
      }
    }
  }
}
