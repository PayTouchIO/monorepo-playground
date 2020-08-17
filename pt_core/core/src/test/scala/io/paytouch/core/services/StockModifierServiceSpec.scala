package io.paytouch.core.services

import io.paytouch._

import java.util.UUID

import com.softwaremill.macwire._

import io.paytouch.core.async.sqs.SendMsgWithRetry
import io.paytouch.core.data.model.{ OrderBundleUpdate, OrderItemUpdate, OrderRecord, OrderUpdate }
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus, QuantityChangeReason }
import io.paytouch.core.data.model.upsertions.{ OrderItemUpsertion, OrderUpsertion }
import io.paytouch.core.messages.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.validators.{ RecoveredOrderBundleOption, RecoveredOrderBundleSet }

@scala.annotation.nowarn("msg=Auto-application")
class StockModifierServiceSpec extends ServiceDaoSpec with StockTestHelpers {
  abstract class StockModifierServiceSpecContext extends ServiceDaoSpecContext with StockTestHelperContext {
    val service = wire[StockModifierService]
    val orderUpdate = randomModelOrderUpdate().copy(locationId = Some(london.id), status = Some(OrderStatus.Completed))

    def createOrderUpsertion(
        orderUpdate: OrderUpdate,
        orderItemUpsertions: Seq[OrderItemUpsertion],
        orderBundleUpdates: Option[Seq[OrderBundleUpdate]] = None,
      ) =
      OrderUpsertion
        .empty(orderUpdate)
        .copy(orderItems = orderItemUpsertions, orderBundles = orderBundleUpdates)

    def getStockEntity(id: UUID) = {
      val stockRecord = stockDao.findByIds(Seq(id)).await.head
      stockService.fromRecordToEntity(stockRecord)
    }

    def getOrderEntity(order: OrderRecord) = orderService.fromRecordToEntity(order)
  }

  "StockModifierService" in {
    "modifyStocks" should {
      "when no previous order item exists" should {
        "update stocks only for products tracking inventory and their parts" in new StockModifierServiceSpecContext {
          val order = Factory.order(merchant).create

          val orderItemJeans = random[OrderItemUpdate].copy(
            productId = Some(jeans.id),
            quantity = Some(1),
            paymentStatus = Some(PaymentStatus.Pending),
          )
          val orderItemShirt = random[OrderItemUpdate].copy(
            productId = Some(shirt.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = {
            val orderItems = Seq(
              random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
              random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
            )
            createOrderUpsertion(orderUpdate, orderItems)
          }

          service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq.empty, Seq.empty).await

          afterAWhile {
            val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
            val stocks = stockDao.findByIds(ids).await
            stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 - 1
            stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
            stocks.find(_.id == ingredientLondonStock.id).get.quantity ==== 1000 - 2 * 100
            stocks.find(_.id == recipeLondonStock.id).get.quantity ==== 5000 - 2 * 50
          }

          afterAWhile {
            assertProductHistoryRecordFromOrder(order.id, jeansLondon, jeansLondonStock, 1)
            assertNoProductHistoryRecord(shirt.id, london.id)
            assertProductHistoryRecordFromOrder(order.id, ingredientLondon, ingredientLondonStock, 2 * 100)
            assertProductHistoryRecordFromOrder(order.id, recipeLondon, recipeLondonStock, 2 * 50)
          }
        }

        "calculates stock changes for bundle products" in new StockModifierServiceSpecContext {
          val order = Factory.order(merchant).create

          val orderItemBundle = random[OrderItemUpdate].copy(
            id = Some(UUID.randomUUID),
            productId = Some(bundle.id),
            quantity = Some(3),
            paymentStatus = Some(PaymentStatus.Pending),
          )
          val orderItemBundleItem = random[OrderItemUpdate].copy(
            id = Some(UUID.randomUUID),
            productId = Some(jeans.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderBundle = random[OrderBundleUpdate].copy(
            bundleOrderItemId = orderItemBundle.id,
            orderBundleSets = Some(
              Seq(
                random[RecoveredOrderBundleSet].copy(
                  bundleSetId = Some(bundleSet.id),
                  orderBundleOptions = Seq(
                    random[RecoveredOrderBundleOption].copy(
                      bundleOptionId = Some(bundleOption1.id),
                      articleOrderItemId = orderItemBundleItem.id,
                    ),
                  ),
                ),
              ),
            ),
          )

          val upsertion = {
            val orderItems = Seq(
              random[OrderItemUpsertion].copy(orderItem = orderItemBundle),
              random[OrderItemUpsertion].copy(orderItem = orderItemBundleItem),
            )
            val orderBundles = Seq(
              orderBundle,
            )
            createOrderUpsertion(orderUpdate, orderItems, Some(orderBundles))
          }

          service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq.empty, Seq.empty).await

          afterAWhile {
            val ids = Seq(jeansLondonStock.id)
            val stocks = stockDao.findByIds(ids).await
            stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 - 6
          }

          afterAWhile {
            assertProductHistoryRecordFromOrder(order.id, jeansLondon, jeansLondonStock, 6)
            assertNoProductHistoryRecord(shirt.id, london.id)
            assertNoProductHistoryRecord(ingredient.id, london.id)
            assertNoProductHistoryRecord(recipe.id, london.id)
          }
        }

        "send stock_changed messages for products with inventory tracking" in new StockModifierServiceSpecContext {
          val order = Factory.order(merchant).create
          val orderItemJeans = random[OrderItemUpdate].copy(
            productId = Some(jeans.id),
            quantity = Some(1),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = {
            val orderItems = Seq(
              random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
            )
            createOrderUpsertion(orderUpdate, orderItems)
          }

          service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq.empty, Seq.empty).await

          afterAWhile {
            val jeansStockEntity = getStockEntity(jeansLondonStock.id)
            jeansStockEntity.quantity ==== 99
            actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
            ok
          }

          // The assertion below needs to be done in a different `afterAWhile` block
          // because if it fails after the `actorMock.expectMsg` has consumed the message
          // then the above block will be repeated and will time out waiting for another message.
          afterAWhile {
            assertProductHistoryRecordFromOrder(order.id, jeansLondon, jeansLondonStock, 1)
          }
        }

        "doesn't send stock_changed messages for products without inventory tracking" in new StockModifierServiceSpecContext {
          val order = Factory.order(merchant).create
          val orderItemShirt = random[OrderItemUpdate].copy(
            productId = Some(shirt.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = {
            val orderItems = Seq(
              random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
            )
            createOrderUpsertion(orderUpdate, orderItems)
          }

          service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq.empty, Seq.empty).await

          afterAWhile {
            actorMock.expectNoMessage()

            assertNoProductHistoryRecord(shirt.id, london.id)
          }
        }
      }

      "when previous order items exist" should {
        "when increasing the order quantity" in {
          "update stocks only for products tracking inventory and their parts" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(1)).create
            val previousShirt = Factory.orderItem(order, product = Some(shirt), quantity = Some(2)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Pending),
              )
            val orderItemShirt =
              random[OrderItemUpdate].copy(
                id = Some(previousShirt.id),
                productId = Some(shirt.id),
                quantity = Some(4),
                paymentStatus = Some(PaymentStatus.Pending),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
                random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service
              .modifyStocks(
                order.id,
                upsertion,
                Some(getOrderEntity(order)),
                Seq(previousJeans, previousShirt),
                Seq.empty,
              )
              .await

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 - (2 - 1)
              stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
              stocks.find(_.id == ingredientLondonStock.id).get.quantity ==== 1000 - (4 - 2) * 100
              stocks.find(_.id == recipeLondonStock.id).get.quantity ==== 5000 - (4 - 2) * 50
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(order.id, jeansLondon, jeansLondonStock, 2 - 1)
              assertNoProductHistoryRecord(shirt.id, london.id)
              assertProductHistoryRecordFromOrder(order.id, ingredientLondon, ingredientLondonStock, (4 - 2) * 100)
              assertProductHistoryRecordFromOrder(order.id, recipeLondon, recipeLondonStock, (4 - 2) * 50)
            }

            afterAWhile {
              val jeansStockEntity = getStockEntity(jeansLondonStock.id)
              actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
              jeansStockEntity.quantity ==== 99
            }
          }

          "calculates stock changes for bundle products" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousBundle = Factory.orderItem(order, product = Some(bundle), quantity = Some(3)).create
            val previousItem = Factory.orderItem(order, product = Some(jeans), quantity = Some(2)).create
            val orderBundle =
              Factory.orderBundle(order, previousBundle, previousItem, Some(bundleSet), Some(bundleOption1)).create

            val orderItemBundle = random[OrderItemUpdate].copy(
              id = Some(previousBundle.id),
              productId = Some(bundle.id),
              quantity = Some(4),
              paymentStatus = Some(PaymentStatus.Pending),
            )
            val orderItemBundleItem = random[OrderItemUpdate].copy(
              id = Some(previousItem.id),
              productId = Some(jeans.id),
              quantity = Some(2),
              paymentStatus = Some(PaymentStatus.Pending),
            )

            val orderBundleUpdate = random[OrderBundleUpdate].copy(
              id = Some(orderBundle.id),
              bundleOrderItemId = Some(previousBundle.id),
              orderBundleSets = Some(
                Seq(
                  random[RecoveredOrderBundleSet].copy(
                    id = orderBundle.orderBundleSets.head.id,
                    bundleSetId = Some(bundleSet.id),
                    orderBundleOptions = Seq(
                      random[RecoveredOrderBundleOption].copy(
                        id = orderBundle.orderBundleSets.head.orderBundleOptions.head.id,
                        bundleOptionId = Some(bundleOption1.id),
                        articleOrderItemId = Some(previousItem.id),
                      ),
                    ),
                  ),
                ),
              ),
            )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemBundle),
                random[OrderItemUpsertion].copy(orderItem = orderItemBundleItem),
              )
              val orderBundles = Seq(
                orderBundleUpdate,
              )
              createOrderUpsertion(orderUpdate, orderItems, Some(orderBundles))
            }

            service
              .modifyStocks(
                order.id,
                upsertion,
                Some(getOrderEntity(order)),
                Seq(previousBundle, previousItem),
                Seq(orderBundle),
              )
              .await

            val currentQuantity = 8
            val previousQuantity = 6

            afterAWhile {
              val ids = Seq(jeansLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 - (currentQuantity - previousQuantity)
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(
                order.id,
                jeansLondon,
                jeansLondonStock,
                currentQuantity - previousQuantity,
              )
              assertNoProductHistoryRecord(shirt.id, london.id)
              assertNoProductHistoryRecord(ingredient.id, london.id)
              assertNoProductHistoryRecord(recipe.id, london.id)
            }
          }
        }

        "when partially decreasing the order item quantity" in {
          "update stocks only for products tracking inventory and their parts" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(2)).create
            val previousShirt = Factory.orderItem(order, product = Some(shirt), quantity = Some(4)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(1),
                paymentStatus = Some(PaymentStatus.Pending),
              )
            val orderItemShirt =
              random[OrderItemUpdate].copy(
                id = Some(previousShirt.id),
                productId = Some(shirt.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Pending),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
                random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service
              .modifyStocks(
                order.id,
                upsertion,
                Some(getOrderEntity(order)),
                Seq(previousJeans, previousShirt),
                Seq.empty,
              )
              .await

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await

              val expected =
                // There were 102 items in stock (not visible in code)
                //              2 were ordered (set in `val previousJeans ... .create`) and so 100 are left (set in ProductStockFixtures)
                // The customer changed his/her mind:
                //              1 was ordered instead of the 2 and so 101 are left
                101

              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== expected
              stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
              stocks.find(_.id == ingredientLondonStock.id).get ==== ingredientLondonStock
              stocks.find(_.id == recipeLondonStock.id).get ==== recipeLondonStock
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(
                order.id,
                jeansLondon,
                jeansLondonStock,
                1 - 2,
                reason = QuantityChangeReason.CustomerReturn,
              )
              assertNoProductHistoryRecord(shirt.id, london.id)
              assertNoProductHistoryRecord(ingredient.id, london.id)
              assertNoProductHistoryRecord(recipe.id, london.id)
            }

            afterAWhile {
              val jeansStockEntity = getStockEntity(jeansLondonStock.id)
              actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
              jeansStockEntity.quantity ==== 101
            }
          }

          "update stocks only for products tracking inventory and their parts even if they are completely removed from the order" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(2)).create
            val previousShirt = Factory.orderItem(order, product = Some(shirt), quantity = Some(4)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(1),
                paymentStatus = Some(PaymentStatus.Pending),
              )
            val orderItemShirt =
              random[OrderItemUpdate].copy(
                id = Some(previousShirt.id),
                productId = Some(shirt.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Pending),
              )

            val upsertion = {
              val orderItems = Seq(
                // Jeans are not part of the order!
                random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service
              .modifyStocks(
                order.id,
                upsertion,
                Some(getOrderEntity(order)),
                Seq(previousJeans, previousShirt),
                Seq.empty,
              )
              .await

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await

              val expected =
                // There were 102 items in stock (not visible in code)
                //              2 were ordered (set in `val previousJeans ... .create`) and so 100 are left (set in ProductStockFixtures)
                // The customer changed his/her mind and the entire order was cancelled
                // so there are 102 items in stock again
                102

              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== expected
              stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
              stocks.find(_.id == ingredientLondonStock.id).get ==== ingredientLondonStock
              stocks.find(_.id == recipeLondonStock.id).get ==== recipeLondonStock
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(
                order.id,
                jeansLondon,
                jeansLondonStock,
                -2,
                reason = QuantityChangeReason.CustomerReturn,
              )
              assertNoProductHistoryRecord(shirt.id, london.id)
              assertNoProductHistoryRecord(ingredient.id, london.id)
              assertNoProductHistoryRecord(recipe.id, london.id)
            }

            afterAWhile {
              val jeansStockEntity = getStockEntity(jeansLondonStock.id)
              actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
              jeansStockEntity.quantity ==== 102
            }
          }
        }

        "when an item is refunded completely" in {
          "return stocks to inventory" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(2)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Refunded),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq(previousJeans), Seq.empty).await

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 + 2
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(
                order.id,
                jeansLondon,
                jeansLondonStock,
                -2,
                reason = QuantityChangeReason.CustomerReturn,
              )
            }

            afterAWhile {
              val jeansStockEntity = getStockEntity(jeansLondonStock.id)
              actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
              jeansStockEntity.quantity ==== 102
            }
          }
        }

        "when an order is canceled" in {
          "return stocks to inventory" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(2)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Pending),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
              )
              createOrderUpsertion(orderUpdate.copy(status = Some(OrderStatus.Canceled)), orderItems)
            }

            service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq(previousJeans), Seq.empty).await

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100 + 2
            }

            afterAWhile {
              assertProductHistoryRecordFromOrder(
                order.id,
                jeansLondon,
                jeansLondonStock,
                -2,
                reason = QuantityChangeReason.CustomerReturn,
              )
            }

            afterAWhile {
              val jeansStockEntity = getStockEntity(jeansLondonStock.id)
              actorMock.expectMsg(SendMsgWithRetry(EntitySynced(jeansStockEntity, london.id)))
              jeansStockEntity.quantity ==== 102
            }
          }
        }

        "when an item was refunded completely" in {
          "do not perform any stock update or product quantity history" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory
              .orderItem(order, product = Some(jeans), quantity = Some(2), paymentStatus = Some(PaymentStatus.Refunded))
              .create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Refunded),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service.modifyStocks(order.id, upsertion, Some(getOrderEntity(order)), Seq(previousJeans), Seq.empty).await
            Thread.sleep(1000)

            val ids = Seq(jeansLondonStock.id)
            val stocks = stockDao.findByIds(ids).await
            stocks.find(_.id == jeansLondonStock.id).get.quantity ==== 100
            assertNoProductHistoryRecord(jeans.id, london.id)
          }
        }

        "when order quantity do not change" in {
          "do not perform any stock update or product quantity history" in new StockModifierServiceSpecContext {
            val order = Factory.order(merchant).create
            val previousJeans = Factory.orderItem(order, product = Some(jeans), quantity = Some(1)).create
            val previousShirt = Factory.orderItem(order, product = Some(shirt), quantity = Some(2)).create

            val orderItemJeans =
              random[OrderItemUpdate].copy(
                id = Some(previousJeans.id),
                productId = Some(jeans.id),
                quantity = Some(1),
                paymentStatus = Some(PaymentStatus.Pending),
              )
            val orderItemShirt =
              random[OrderItemUpdate].copy(
                id = Some(previousShirt.id),
                productId = Some(shirt.id),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Pending),
              )

            val upsertion = {
              val orderItems = Seq(
                random[OrderItemUpsertion].copy(orderItem = orderItemJeans),
                random[OrderItemUpsertion].copy(orderItem = orderItemShirt),
              )
              createOrderUpsertion(orderUpdate, orderItems)
            }

            service
              .modifyStocks(
                order.id,
                upsertion,
                Some(getOrderEntity(order)),
                Seq(previousJeans, previousShirt),
                Seq.empty,
              )
              .await

            // giving it some time to make sure that processed messages are discarded
            Thread.sleep(1000)

            val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
            val stocks = stockDao.findByIds(ids).await
            stocks.find(_.id == jeansLondonStock.id).get ==== jeansLondonStock
            stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
            stocks.find(_.id == ingredientLondonStock.id).get ==== ingredientLondonStock
            stocks.find(_.id == recipeLondonStock.id).get ==== recipeLondonStock

            assertNoProductHistoryRecord(jeans.id, london.id)
            assertNoProductHistoryRecord(shirt.id, london.id)
            assertNoProductHistoryRecord(ingredient.id, london.id)
            assertNoProductHistoryRecord(recipe.id, london.id)

            actorMock.expectNoMessage()
          }
        }
      }
    }
  }
}
