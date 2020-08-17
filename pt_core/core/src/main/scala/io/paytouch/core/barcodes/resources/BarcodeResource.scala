package io.paytouch.core.barcodes.resources

import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.resources.JsonResource

trait BarcodeResource extends JsonResource {

  def barcodeService: BarcodeService

  val barcodeRoutes = path("barcodes.generate") {
    post {
      entity(as[BarcodeMetadata]) { metadata =>
        authenticate { implicit user =>
          implicit val merchant = user.toMerchantContext
          onSuccess(barcodeService.generate(metadata))(result => completeAsApiResponse(result, "barcode"))
        }
      }
    }
  }
}
