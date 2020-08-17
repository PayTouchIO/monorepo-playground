package io.paytouch.core.processors

import scala.concurrent._

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.messages.entities.{ ImagesAssociated, ImagesDeleted }
import io.paytouch.core.services.ImageUploadService
import io.paytouch.core.utils.MultipleLocationFixtures

class ImagesProcessorSpec extends ProcessorSpec {
  abstract class ImagesDeletedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    implicit val u: UserContext = userContext

    val imageIds = Seq(UUID.randomUUID)
    val imageUploadServiceMock = mock[ImageUploadService]

    lazy val processor = new ImagesProcessor(imageUploadServiceMock)
  }

  "ImagesDeletedProcessor" in {

    "delete the image" in new ImagesDeletedProcessorSpecContext {
      imageUploadServiceMock.deleteImageByIdsAndMerchantId(any, any) returns Future.successful(Seq.empty)

      processor.execute(ImagesDeleted(imageIds, merchant.id))

      afterAWhile {
        there was one(imageUploadServiceMock).deleteImageByIdsAndMerchantId(imageIds, merchant.id)
      }
    }

    "associate object the images" in new ImagesDeletedProcessorSpecContext {
      imageUploadServiceMock.associateImagesToObjectId(any, any, any) returns Future.successful(true)

      val objectId = UUID.randomUUID
      processor.execute(ImagesAssociated(objectId = objectId, imageIds = imageIds, merchantId = merchant.id))

      afterAWhile {
        there was one(imageUploadServiceMock).associateImagesToObjectId(
          imageIds = imageIds,
          objectId = objectId,
          merchant.id,
        )
      }
    }
  }
}
