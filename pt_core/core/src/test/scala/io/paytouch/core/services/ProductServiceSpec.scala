package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.monitors.{ ArticleMonitor, ProductChange }
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class ProductServiceSpec extends ServiceDaoSpec {

  abstract class ProductsServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    val productMonitor = actorMock.ref.taggedWith[ArticleMonitor]
    val service = wire[ProductService]
    val productDao = daos.productDao
    val productLocationDao = daos.productLocationDao
  }

  "ProductService" in {
    "create" should {
      "if successful" should {
        "not send any messages" in new ProductsServiceSpecContext {
          val newId = UUID.randomUUID
          val creation = random[ProductCreation]

          val (_, productEntity) = service.create(newId, creation).await.success

          actorMock.expectNoMessage()
        }
      }

      "if validation fails" should {
        "not send any message" in new ProductsServiceSpecContext {
          val newId = UUID.randomUUID
          val creation = random[ProductCreation].copy(imageUploadIds = Seq(UUID.randomUUID))

          service.create(newId, creation).await.failures

          actorMock.expectNoMessage()
        }
      }
    }

    "update" should {
      "if successful" should {
        "send the correct messages" in new ProductsServiceSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val update = random[ArticleUpdate].copy(categoryIds = None, supplierIds = None)

          val (_, productEntity) = service.update(product.id, update).await.success

          val state = (product, Seq.empty, Seq.empty, Seq.empty)
          actorMock.expectMsg(ProductChange(state, update, userCtx))
          actorMock.expectNoMessage()
        }

        "sends a entity synced messsage for each location" in new ProductsServiceSpecContext {
          val product = Factory.simpleProduct(merchant).create
          val productLocationRome = Factory.productLocation(product, rome).create
          val productLocationLondon = Factory.productLocation(product, london).create

          val update = random[ArticleUpdate].copy(categoryIds = None, supplierIds = None)

          val (_, productEntity) = service.update(product.id, update).await.success

          val state = (product, Seq.empty, Seq(productLocationRome, productLocationLondon), Seq.empty)
          actorMock.expectMsg(ProductChange(state, update, userCtx))
          actorMock.expectMsg(
            SendMsgWithRetry(
              EntitySynced[IdOnlyEntity](IdOnlyEntity(productEntity.id, productEntity.classShortName), rome.id)(
                userCtx,
              ),
            ),
          )
          actorMock.expectMsg(
            SendMsgWithRetry(
              EntitySynced[IdOnlyEntity](IdOnlyEntity(productEntity.id, productEntity.classShortName), london.id)(
                userCtx,
              ),
            ),
          )
          actorMock.expectNoMessage()
        }
      }

      "if validation fails" should {
        "not send any message" in new ProductsServiceSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val update =
            random[ArticleUpdate].copy(variantProducts = Some(Seq(random[VariantArticleUpdate])))

          service.update(product.id, update).await.failures

          actorMock.expectNoMessage()
        }
      }
    }

    "updateAverageCost" should {
      "update average cost of given product ids" in new ProductsServiceSpecContext {
        val product = Factory.simpleProduct(merchant).create
        val otherProduct = Factory.simpleProduct(merchant).create
        val productLocationRome = Factory.productLocation(product, rome).create
        val productLocationLondon = Factory.productLocation(product, london).create

        val receivingOrderRome = Factory.receivingOrder(rome, user).create
        val receivingOrderProductRome1 =
          Factory.receivingOrderProduct(receivingOrderRome, product, costAmount = Some(5)).create
        val receivingOrderProductRome2 =
          Factory.receivingOrderProduct(receivingOrderRome, product, costAmount = Some(10)).create
        val receivingOrderProductRome3 =
          Factory.receivingOrderProduct(receivingOrderRome, product, costAmount = None).create
        val receivingOrderLondon = Factory.receivingOrder(london, user).create
        val receivingOrderProductLondon1 =
          Factory.receivingOrderProduct(receivingOrderLondon, product, costAmount = Some(15)).create
        val receivingOrderProductLondon2 =
          Factory.receivingOrderProduct(receivingOrderLondon, otherProduct, costAmount = Some(1000)).create

        service.updateAverageCost(Seq(product.id), rome.id).await

        afterAWhile {
          productDao.findById(product.id).await.get.averageCostAmount ==== Some(10)
          productLocationDao.findById(productLocationRome.id).await.get.averageCostAmount ==== Some(7.5)
          productLocationDao.findById(productLocationLondon.id).await.get.averageCostAmount ==== None
        }
      }
    }
  }
}
