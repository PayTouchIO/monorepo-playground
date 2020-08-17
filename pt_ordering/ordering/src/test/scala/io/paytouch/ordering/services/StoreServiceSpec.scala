package io.paytouch.ordering.services

import java.util.UUID

import scala.collection.mutable

import akka.http.scaladsl.model.headers.Authorization

import com.softwaremill.macwire._

import io.paytouch.ordering._
import io.paytouch.ordering.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageType
import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.entities.{ UpdateActiveItem, _ }
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.messages.entities.{ ImagesAssociated, ImagesDeleted, StoreCreated }
import io.paytouch.ordering.messages.SQSMessageHandler
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.ordering.utils._

@scala.annotation.nowarn("msg=Auto-application")
class StoreServiceSpec extends ServiceDaoSpec with CommonArbitraries {
  abstract class StoreServiceSpecContext extends ServiceDaoSpecContext {
    implicit val uah: Authorization = userAuthorizationHeader

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    val imageService = wire[ImageService]

    val service = wire[StoreService]
  }

  "StoreService" in {
    "updateActive" should {
      "not send a SQS message" in new StoreServiceSpecContext {
        val storeItems = Seq(UpdateActiveItem(londonStore.id, true), UpdateActiveItem(romeStore.id, false))
        service.updateActive(storeItems).await.success

        actorMock.expectNoMessage()
      }
    }

    "create" should {
      "send a ImagesAssociated SQS message if there is at least one new id to associate" in new StoreServiceSpecContext
        with CreationFixtures {
        val urls = random[ImageUrls](2)
        val heroImageUrls = Seq(urls.head)
        val logoImageUrls = Seq(urls(1))

        PtCoreStubData.recordImageIds(
          mutable.Map(
            ImageType.StoreHero -> heroImageUrls.map(_.imageUploadId),
            ImageType.StoreLogo -> logoImageUrls.map(_.imageUploadId),
          ),
        )

        val creation = baseCreation.copy(heroImageUrls = heroImageUrls, logoImageUrls = logoImageUrls)

        service.create(newStoreId, creation).await.success

        val heroImgAssociatedMsg =
          ImagesAssociated(
            objectId = baseCreation.locationId,
            imageIds = heroImageUrls.map(_.imageUploadId),
          )

        val logoImgAssociatedMsg =
          ImagesAssociated(
            objectId = baseCreation.locationId,
            imageIds = logoImageUrls.map(_.imageUploadId),
          )

        actorMock.expectMsgAllOf(
          SendMsgWithRetry(storeCreatedMsg),
          SendMsgWithRetry(heroImgAssociatedMsg),
          SendMsgWithRetry(logoImgAssociatedMsg),
        )
      }

      "do not send a ImagesAssociated SQS message if there are no new ids to associate" in new StoreServiceSpecContext
        with CreationFixtures {
        val creation = baseCreation

        service.create(newStoreId, creation).await.success

        actorMock.expectMsgAllOf(SendMsgWithRetry(storeCreatedMsg))
      }
    }

    "update" should {
      "send a ImagesDeleted SQS message if there is at least one id to delete" in new StoreServiceSpecContext
        with UpdateFixtures {
        lazy val urls = random[ImageUrls](2)
        override lazy val londonHeroImageUrl = Seq(urls.head.copy(imageUploadId = UUID.randomUUID))
        override lazy val londonLogoImageUrl = Seq(urls(1).copy(imageUploadId = UUID.randomUUID))

        val update = emptyUpdate.copy(heroImageUrls = Some(Seq.empty), logoImageUrls = Some(Seq.empty))

        service.update(storeId, update).await.success

        val heroImgDeletedMsg = ImagesDeleted(londonHeroImageUrl.map(_.imageUploadId))
        val logoImgDeletedMsg = ImagesDeleted(londonLogoImageUrl.map(_.imageUploadId))
        actorMock.expectMsgAllOf(SendMsgWithRetry(heroImgDeletedMsg), SendMsgWithRetry(logoImgDeletedMsg))
      }

      "do not send a ImagesDeleted SQS message if there are no ids to delete" in new StoreServiceSpecContext
        with UpdateFixtures {
        val update = emptyUpdate

        service.update(storeId, update).await.success

        actorMock.expectNoMessage()
      }

      "send a ImagesAssociated SQS message if there is at least one new id to associate" in new StoreServiceSpecContext
        with UpdateFixtures {
        lazy val urls = random[ImageUrls](2)
        val heroImageUrls = Seq(urls.head)
        val logoImageUrls = Seq(urls(1))

        PtCoreStubData.recordImageIds(
          mutable.Map(
            ImageType.StoreHero -> heroImageUrls.map(_.imageUploadId),
            ImageType.StoreLogo -> logoImageUrls.map(_.imageUploadId),
          ),
        )

        val update = emptyUpdate.copy(heroImageUrls = Some(heroImageUrls), logoImageUrls = Some(logoImageUrls))

        service.update(storeId, update).await.success

        val heroImgAssociatedMsg = ImagesAssociated(storeLocationId, heroImageUrls.map(_.imageUploadId))
        val logoImgAssociatedMsg = ImagesAssociated(storeLocationId, logoImageUrls.map(_.imageUploadId))
        actorMock.expectMsgAllOf(SendMsgWithRetry(heroImgAssociatedMsg), SendMsgWithRetry(logoImgAssociatedMsg))
      }

      "do not send a ImagesAssociated SQS message if there are no new ids to associate" in new StoreServiceSpecContext
        with UpdateFixtures {
        val update = emptyUpdate

        service.update(storeId, update).await.success

        actorMock.expectNoMessage()
      }
    }

    "find" should {
      "return only methods defined in validMethodTypes marked as active (assuming they were active in the first place)" in new StoreServiceSpecContext
        with CreationFixtures {
        val creation = baseCreation.copy(
          paymentMethods = Seq(
            PaymentMethod(PaymentMethodType.Cash, active = true),
            PaymentMethod(PaymentMethodType.Ekashu, active = true),
          ),
        )

        override lazy val merchant =
          Factory
            .merchant(
              paymentProcessor = Some(PaymentProcessor.Worldpay),
            )
            .create

        service.create(newStoreId, creation)(userContext).await.success

        val store =
          service.findById(newStoreId).await.get

        store.paymentMethods === Seq(
          PaymentMethod(PaymentMethodType.Cash, active = true),
          PaymentMethod(PaymentMethodType.Worldpay, active = false),
        )
      }

      "always contain at least Cash" in new StoreServiceSpecContext with CreationFixtures {
        val creation = baseCreation.copy(
          paymentMethods = Seq(
            PaymentMethod(PaymentMethodType.Cash, active = true),
          ),
        )

        override lazy val merchant =
          Factory
            .merchant(
              paymentProcessor = None,
            )
            .create

        service.create(newStoreId, creation)(userContext).await.success

        val store =
          service.findById(newStoreId).await.get

        store.paymentMethods === Seq(
          PaymentMethod(PaymentMethodType.Cash, active = true),
        )
      }
    }
  }

  trait CreationFixtures { self: StoreServiceSpecContext =>
    val newStoreId = UUID.randomUUID
    val storeCreatedMsg = StoreCreated(merchant.id, newYorkId)

    val baseCreation = StoreCreation(
      locationId = newYorkId,
      urlSlug = "NewYork-NewYork",
      catalogId = UUID.randomUUID,
      active = true,
      description = None,
      heroBgColor = None,
      heroImageUrls = Seq.empty,
      logoImageUrls = Seq.empty,
      takeOutEnabled = true,
      takeOutStopMinsBeforeClosing = None,
      deliveryEnabled = true,
      deliveryMinAmount = None,
      deliveryMaxAmount = None,
      deliveryMaxDistance = None,
      deliveryStopMinsBeforeClosing = None,
      deliveryFeeAmount = None,
    )

    PtCoreStubData.recordCatalogIds(Seq(baseCreation.catalogId))
  }

  trait UpdateFixtures { self: StoreServiceSpecContext =>
    val storeId = londonStore.id
    val storeLocationId = londonStore.locationId

    val emptyUpdate = StoreUpdate(
      locationId = None,
      urlSlug = None,
      catalogId = None,
      active = None,
      description = None,
      heroBgColor = None,
      heroImageUrls = None,
      logoImageUrls = None,
      takeOutEnabled = None,
      takeOutStopMinsBeforeClosing = None,
      deliveryEnabled = None,
      deliveryMinAmount = None,
      deliveryMaxAmount = None,
      deliveryMaxDistance = None,
      deliveryStopMinsBeforeClosing = None,
      deliveryFeeAmount = None,
      paymentMethods = None,
    )
  }
}
