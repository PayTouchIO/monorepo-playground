package io.paytouch.core.resources.barcodes

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ DefaultFixtures, FSpec }
import io.paytouch.core.{ ServiceConfigurations => Config }

class BarcodesGenerateFSpec extends FSpec {

  abstract class BarcodesGenerateFSpecContext extends FSpecContext with DefaultFixtures {
    val s3ImagesBucket = Config.s3ImagesBucket
  }

  "POST /v1/barcodes.generate" in {
    "if request has valid token" should {

      "return the url of the generated barcode" in new BarcodesGenerateFSpecContext {
        @scala.annotation.nowarn("msg=Auto-application")
        val metadata = random[BarcodeMetadata]

        val expectedBaseKey = {
          val encodedValue = UUID.nameUUIDFromBytes(metadata.value.getBytes)
          val filename = s"$encodedValue.${metadata.width}x${metadata.height}.m${metadata.margin}"
          s"${merchant.id}/barcodes/${metadata.format}/$filename.png"
        }

        Post(s"/v1/barcodes.generate", metadata).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val url = responseAs[ApiResponse[String]].data
          url ==== s"https://s3.amazonaws.com/$s3ImagesBucket/$expectedBaseKey"
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new BarcodesGenerateFSpecContext {
        @scala.annotation.nowarn("msg=Auto-application")
        val metadata = random[BarcodeMetadata]
        Post(s"/v1/barcodes.generate", metadata)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
