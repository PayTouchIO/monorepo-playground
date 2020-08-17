package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.services.ProductImportService

trait ProductImportFormResource extends FormDataResource {

  def productImportService: ProductImportService

  val productImportRoutes: Route = path("products.import") {
    post {
      parameters(
        "import_id".as[UUID],
        "location_id[]".as[Seq[UUID]],
        "dry_run".as[Boolean].?(false),
        "delete_existing".as[Boolean].?,
      ) { (importId, locationIds, dryRun, deleteExisting) =>
        authenticate { implicit user =>
          saveCSV(importId) {
            case file =>
              onSuccess(
                productImportService.scheduleProductImport(importId, file, locationIds, dryRun, deleteExisting),
              ) { result =>
                completeValidatedData(result)
              }
          }
        }
      }
    }
  }
}
