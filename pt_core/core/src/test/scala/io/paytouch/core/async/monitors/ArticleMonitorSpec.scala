package io.paytouch.core.async.monitors

import java.util.UUID

import akka.actor.Props
import io.paytouch.core.data.model.enums.{ ChangeReason, ImageUploadType }
import io.paytouch.core.data.model.{ ProductCostHistoryRecord, ProductLocationRecord, ProductPriceHistoryRecord }
import io.paytouch.core.entities.{ ArticleLocationUpdate, ArticleUpdate, VariantArticleUpdate }
import io.paytouch.core.services.ImageUploadService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ArticleMonitorSpec extends MonitorSpec {

  abstract class ProductMonitorSpecContext extends MonitorSpecContext with StateFixtures {

    val imageUploadService = mock[ImageUploadService]
    val productPriceHistoryDao = daos.productPriceHistoryDao
    val productCostHistoryDao = daos.productCostHistoryDao

    lazy val monitor = monitorSystem.actorOf(Props(new ArticleMonitor(imageUploadService)))

    def assertProductPriceHistory(
        record: ProductPriceHistoryRecord,
        productLocation: ProductLocationRecord,
        newPriceAmount: BigDecimal,
        reason: ChangeReason,
        notes: Option[String],
      ) = {
      record.merchantId ==== merchant.id
      record.productId ==== productLocation.productId
      record.locationId ==== productLocation.locationId
      record.userId ==== user.id
      record.prevPriceAmount ==== price
      record.newPriceAmount ==== newPriceAmount
      record.reason ==== reason
      record.notes ==== notes
    }

    def assertProductCostHistory(
        record: ProductCostHistoryRecord,
        productLocation: ProductLocationRecord,
        newCostAmount: BigDecimal,
        reason: ChangeReason,
        notes: Option[String],
      ) = {
      record.merchantId ==== merchant.id
      record.productId ==== productLocation.productId
      record.locationId ==== productLocation.locationId
      record.userId ==== user.id
      record.prevCostAmount ==== cost
      record.newCostAmount ==== newCostAmount
      record.reason ==== reason
      record.notes ==== notes
    }

    def assertNoProductPriceHistoryRecorded(id: UUID, priceHistoryRecords: Seq[ProductPriceHistoryRecord]) =
      priceHistoryRecords.find(_.productId == id) must beNone

    def assertNoProductCostHistoryRecorded(id: UUID, costHistoryRecords: Seq[ProductCostHistoryRecord]) =
      costHistoryRecords.find(_.productId == id) must beNone
  }

  "ProductMonitor" should {

    "when detecting image changes" should {

      "delete old images" in new ProductMonitorSpecContext {
        val newImage = Factory.imageUpload(merchant, Some(tShirt.id), None, Some(ImageUploadType.Product)).create

        val update = random[ArticleUpdate].copy(imageUploadIds = Some(Seq(newImage.id)))

        monitor ! ProductChange(state, update, userContext)

        afterAWhile {
          there was one(imageUploadService).deleteImages(Seq(imageA.id, imageB.id), ImageUploadType.Product)
        }
      }

      "do not delete images that have not changed" in new ProductMonitorSpecContext {
        val update = random[ArticleUpdate].copy(imageUploadIds = None)
        monitor ! ProductChange(state, update, userContext)

        there was noCallsTo(imageUploadService)
      }
    }

    "when detecting price changes" should {

      "detect change in product and variants" in new ProductMonitorSpecContext {
        val newPrice = 15
        val productLocationUpdate = random[ArticleLocationUpdate].copy(price = newPrice, cost = None)
        val productLocationNotUpdate = random[ArticleLocationUpdate].copy(price = price, cost = None)
        val locationOverridesWithChanges = Map(rome.id -> Some(productLocationUpdate))
        val locationOverridesWithNoChanges = Map(rome.id -> Some(productLocationNotUpdate))

        val blueUpdate =
          random[VariantArticleUpdate].copy(id = blueTShirt.id, locationOverrides = locationOverridesWithChanges)
        val yellowUpdate =
          random[VariantArticleUpdate].copy(id = yellowTShirt.id, locationOverrides = locationOverridesWithNoChanges)

        val update = random[ArticleUpdate]
          .copy(locationOverrides = locationOverridesWithChanges)
          .copy(variantProducts = Some(Seq(blueUpdate, yellowUpdate)))

        monitor ! ProductChange(state, update, userContext)

        afterAWhile {
          val priceHistoryRecords = productPriceHistoryDao.findAllByProductIds(productIds).await
          val costHistoryRecords = productCostHistoryDao.findAllByProductIds(productIds).await

          priceHistoryRecords.size ==== 2

          assertProductPriceHistory(
            priceHistoryRecords.find(_.productId == tShirt.id).get,
            tShirtRome,
            newPrice,
            update.reason,
            update.notes,
          )
          assertProductPriceHistory(
            priceHistoryRecords.find(_.productId == blueTShirt.id).get,
            blueTShirtRome,
            newPrice,
            update.reason,
            update.notes,
          )
          assertNoProductPriceHistoryRecorded(yellowTShirt.id, priceHistoryRecords)

          assertNoProductCostHistoryRecorded(tShirt.id, costHistoryRecords)
          assertNoProductCostHistoryRecorded(blueTShirt.id, costHistoryRecords)
          assertNoProductCostHistoryRecorded(yellowTShirt.id, costHistoryRecords)
        }
      }

      "do nothing if prices haven't changed" in new ProductMonitorSpecContext {
        val update = random[ArticleUpdate].copy(variantProducts = None)

        monitor ! ProductChange(state, update, userContext)

        afterAWhile {
          val priceHistoryRecords = productPriceHistoryDao.findAllByProductIds(productIds).await
          priceHistoryRecords ==== Seq.empty
        }

      }
    }

    "when detecting cost changes" should {

      "detect change in product and variants" in new ProductMonitorSpecContext {
        val newCost = 15
        val productLocationUpdate = random[ArticleLocationUpdate].copy(price = price, cost = Some(newCost))
        val locationOverrides = Map(rome.id -> Some(productLocationUpdate))

        val blueUpdate = random[VariantArticleUpdate].copy(id = blueTShirt.id, locationOverrides = locationOverrides)
        val yellowUpdate =
          random[VariantArticleUpdate].copy(id = yellowTShirt.id, locationOverrides = locationOverrides)

        val update = random[ArticleUpdate]
          .copy(locationOverrides = locationOverrides)
          .copy(variantProducts = Some(Seq(blueUpdate, yellowUpdate)))

        monitor ! ProductChange(state, update, userContext)

        afterAWhile {
          val costHistoryRecords = productCostHistoryDao.findAllByProductIds(productIds).await
          costHistoryRecords.size ==== 3

          assertProductCostHistory(
            costHistoryRecords.find(_.productId == tShirt.id).get,
            tShirtRome,
            newCost,
            update.reason,
            update.notes,
          )
          assertProductCostHistory(
            costHistoryRecords.find(_.productId == blueTShirt.id).get,
            blueTShirtRome,
            newCost,
            update.reason,
            update.notes,
          )
          assertProductCostHistory(
            costHistoryRecords.find(_.productId == yellowTShirt.id).get,
            yellowTShirtRome,
            newCost,
            update.reason,
            update.notes,
          )
        }
      }

      "do nothing if costs haven't changed" in new ProductMonitorSpecContext {
        val update = random[ArticleUpdate].copy(variantProducts = None)

        monitor ! ProductChange(state, update, userContext)

        afterAWhile {
          val costHistoryRecords = productCostHistoryDao.findAllByProductIds(productIds).await
          costHistoryRecords ==== Seq.empty
        }

      }
    }
  }

  trait StateFixtures extends MonitorStateFixtures {
    val tShirt = Factory.templateProduct(merchant).create

    val price = 10
    val cost = 5

    val blueTShirt = Factory.variantProduct(merchant, tShirt).create
    val yellowTShirt = Factory.variantProduct(merchant, tShirt).create
    val variants = Seq(blueTShirt, yellowTShirt)

    val productIds = Seq(tShirt.id, blueTShirt.id, yellowTShirt.id)

    val tShirtRome = Factory
      .productLocation(tShirt, rome, priceAmount = Some(price), costAmount = Some(cost))
      .create
    val blueTShirtRome = Factory
      .productLocation(blueTShirt, rome, priceAmount = Some(price), costAmount = Some(cost))
      .create
    val yellowTShirtRome = Factory
      .productLocation(yellowTShirt, rome, priceAmount = Some(price), costAmount = Some(cost))
      .create
    val productLocations = Seq(tShirtRome, blueTShirtRome, yellowTShirtRome)

    val imageA = Factory.imageUpload(merchant, Some(tShirt.id), None, Some(ImageUploadType.Product)).create
    val imageB = Factory.imageUpload(merchant, Some(tShirt.id), None, Some(ImageUploadType.Product)).create

    val state = (tShirt, variants, productLocations, Seq(imageA, imageB))
  }
}
