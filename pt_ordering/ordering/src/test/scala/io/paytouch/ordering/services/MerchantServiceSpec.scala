package io.paytouch.ordering.services

import java.util.UUID

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

class MerchantServiceSpec extends ServiceDaoSpec with CommonArbitraries {
  abstract class MerchantServiceSpecContext extends ServiceDaoSpecContext {
    implicit val uah: Authorization = userAuthorizationHeader
    val service = wire[MerchantService]
  }

  "MerchantService" in {
    "generateUrlSlug" should {
      "it generates a url slug for a merchant display name" in new MerchantServiceSpecContext {
        service.generateUrlSlug("Carlbuck's Coffee Bar").await ==== "carlbucks-coffee-bar"
      }

      "it generates a unique url slug for a merchant display name" in new MerchantServiceSpecContext {
        service.generateUrlSlug("Meat & Bread").await ==== "meat-bread"

        val existingMerchant = Factory.merchant(urlSlug = Some("meat-bread")).create
        val result = service.generateUrlSlug("Meat & Bread").await
        result !=== "meat-bread"
        result !=== ""
      }

      "it handles non ascii characters sensibly" in new MerchantServiceSpecContext {
        val result = service.generateUrlSlug("🥺").await
        result !=== ""
        result !=== "🥺"
      }
    }
  }
}
