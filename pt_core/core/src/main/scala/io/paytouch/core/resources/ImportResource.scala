package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.services.ImportService

trait ImportResource extends JsonResource {

  def importService: ImportService

  val importRoutes: Route =
    path("imports.get") {
      parameter("import_id".as[UUID]) { id =>
        get {
          authenticate { implicit user =>
            onSuccess(importService.findById(id))(result => completeAsOptApiResponse(result))
          }
        }
      }
    }
}
