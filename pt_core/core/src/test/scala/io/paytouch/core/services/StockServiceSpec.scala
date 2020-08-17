package io.paytouch.core.services

import com.softwaremill.macwire.wire

import io.paytouch.implicits._

import io.paytouch.core.entities.StockUpdate
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class StockServiceSpec extends ServiceDaoSpec with StockTestHelpers {
  abstract class StockServiceSpecContext extends ServiceDaoSpecContext with StockTestHelperContext {
    val service = wire[StockService]
  }

  "StockService" in {
    "bulkUpdate" should {
      "if validation is successful" should {
        "for products" should {
          "update stocks and product quantity history" in new StockServiceSpecContext {
            val randomStockUpdates = random[StockUpdate](2)

            val jeansQuantity = 22
            val jeansUpdate =
              randomStockUpdates.head.copy(locationId = london.id, productId = jeans.id, quantity = Some(jeansQuantity))

            val shirtQuantity = 44
            val shirtUpdate =
              randomStockUpdates(1).copy(locationId = london.id, productId = shirt.id, quantity = Some(shirtQuantity))

            service.bulkUpdate(Seq(jeansUpdate, shirtUpdate)).await.success

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get.quantity ==== jeansQuantity
              stocks.find(_.id == shirtLondonStock.id).get.quantity ==== shirtQuantity
              stocks.find(_.id == ingredientLondonStock.id).get ==== ingredientLondonStock
              stocks.find(_.id == recipeLondonStock.id).get ==== recipeLondonStock
            }

            afterAWhile {
              assertProductHistoryRecord(
                jeansLondon,
                jeansLondonStock,
                jeansQuantity - jeansLondonStock.quantity,
                jeansUpdate.reason,
                jeansUpdate.notes,
              )
              assertProductHistoryRecord(
                shirtLondon,
                shirtLondonStock,
                shirtQuantity - shirtLondonStock.quantity,
                shirtUpdate.reason,
                shirtUpdate.notes,
              )
              assertNoProductHistoryRecord(ingredient.id, london.id)
              assertNoProductHistoryRecord(recipe.id, london.id)
            }
          }
        }

        "for ingredients" should {
          "update stocks, product quantity history and parts for those with tracking_inventory_parts" in new StockServiceSpecContext {
            val ingredientQuantity = 3333
            val ingredientUpdate = random[StockUpdate].copy(
              locationId = london.id,
              productId = ingredient.id,
              quantity = Some(ingredientQuantity),
            )

            service.bulkUpdate(Seq(ingredientUpdate)).await.success

            afterAWhile {
              val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == jeansLondonStock.id).get ==== jeansLondonStock
              stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
              stocks.find(_.id == ingredientLondonStock.id).get.quantity ==== ingredientQuantity
              stocks.find(_.id == recipeLondonStock.id).get ==== recipeLondonStock
            }

            afterAWhile {
              assertNoProductHistoryRecord(jeans.id, london.id)
              assertNoProductHistoryRecord(shirt.id, london.id)
              assertProductHistoryRecord(
                ingredientLondon,
                ingredientLondonStock,
                ingredientQuantity - ingredientLondonStock.quantity,
                ingredientUpdate.reason,
                ingredientUpdate.notes,
              )
              assertNoProductHistoryRecord(recipe.id, london.id)
            }
          }
        }

        "for recipes" should {
          "when quantity increases" should {
            "update stocks, product quantity history and parts for those with tracking_inventory_parts" in new StockServiceSpecContext {
              val diffQuantity = 1
              val recipeUpdate = random[StockUpdate].copy(
                locationId = london.id,
                productId = recipe.id,
                quantity = Some(recipeLondonStock.quantity + diffQuantity),
              )

              service.bulkUpdate(Seq(recipeUpdate)).await.success

              afterAWhile {
                val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
                val stocks = stockDao.findByIds(ids).await
                stocks.find(_.id == jeansLondonStock.id).get ==== jeansLondonStock
                stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
                stocks
                  .find(_.id == ingredientLondonStock.id)
                  .get
                  .quantity ==== ingredientLondonStock.quantity - (3 * diffQuantity)
                stocks.find(_.id == recipeLondonStock.id).get.quantity ==== recipeLondonStock.quantity + diffQuantity
              }

              afterAWhile {
                assertNoProductHistoryRecord(jeans.id, london.id)
                assertNoProductHistoryRecord(shirt.id, london.id)
                assertProductHistoryRecord(
                  ingredientLondon,
                  ingredientLondonStock,
                  -3 * diffQuantity,
                  recipeUpdate.reason,
                  recipeUpdate.notes,
                )
                assertProductHistoryRecord(
                  recipeLondon,
                  recipeLondonStock,
                  diffQuantity,
                  recipeUpdate.reason,
                  recipeUpdate.notes,
                )
              }
            }
          }

          "when quantity decreases" should {
            "update stocks, product quantity history, but not the parts" in new StockServiceSpecContext {
              val diffQuantity = -1
              val recipeUpdate = random[StockUpdate].copy(
                locationId = london.id,
                productId = recipe.id,
                quantity = Some(recipeLondonStock.quantity + diffQuantity),
              )

              service.bulkUpdate(Seq(recipeUpdate)).await.success

              afterAWhile {
                val ids = Seq(jeansLondonStock.id, shirtLondonStock.id, ingredientLondonStock.id, recipeLondonStock.id)
                val stocks = stockDao.findByIds(ids).await
                stocks.find(_.id == jeansLondonStock.id).get ==== jeansLondonStock
                stocks.find(_.id == shirtLondonStock.id).get ==== shirtLondonStock
                stocks.find(_.id == ingredientLondonStock.id).get ==== ingredientLondonStock
                stocks.find(_.id == recipeLondonStock.id).get.quantity ==== recipeLondonStock.quantity + diffQuantity
              }

              afterAWhile {
                assertNoProductHistoryRecord(jeans.id, london.id)
                assertNoProductHistoryRecord(shirt.id, london.id)
                assertNoProductHistoryRecord(ingredient.id, london.id)
                assertProductHistoryRecord(
                  recipeLondon,
                  recipeLondonStock,
                  diffQuantity,
                  recipeUpdate.reason,
                  recipeUpdate.notes,
                )
              }
            }
          }
        }
      }

      "if validation fails" should {
        "do not perform any product quantity history or stock modifier changes" in new StockServiceSpecContext {
          val newYork = Factory.location(merchant).create
          val updates = Seq(
            random[StockUpdate].copy(locationId = newYork.id, productId = jeans.id),
          )
          service.bulkUpdate(updates).await.failures

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
        }
      }
    }
  }
}
