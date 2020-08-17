package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities.enums.FeedbackStatus
import io.paytouch.core.expansions.OrderFeedbackExpansions
import io.paytouch.core.filters.OrderFeedbackFilters
import io.paytouch.core.services.OrderFeedbackService

trait OrderFeedbackResource extends JsonResource {
  def orderFeedbackService: OrderFeedbackService

  val orderFeedbackRoutes: Route = path("order_feedback.list") {
    get {
      paginateWithDefaults(30) { implicit pagination =>
        parameters(
          "location_id".as[UUID].?,
          "customer_id".as[UUID].?,
          "status".as[FeedbackStatus].?,
          "from".as[LocalDateTime].?,
          "to".as[LocalDateTime].?,
        ).as(OrderFeedbackFilters) { filters =>
          expandParameters("customers")(OrderFeedbackExpansions) { expansions =>
            authenticate { implicit user =>
              onSuccess(orderFeedbackService.findAll(filters)(expansions)) {
                case result =>
                  completeAsPaginatedApiResponse(result)
              }
            }
          }
        }
      }
    }
  }
}
