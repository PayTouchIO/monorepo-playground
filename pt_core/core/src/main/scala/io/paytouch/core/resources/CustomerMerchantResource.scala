package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ParameterDirectives.ParamMagnet
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ CustomerSource, CustomerSourceAlias }
import io.paytouch.core.expansions.CustomerExpansions
import io.paytouch.core.filters.CustomerFilters
import io.paytouch.core.services.CustomerMerchantService

trait CustomerMerchantResource extends JsonResource {
  def customerMerchantService: CustomerMerchantService

  val customerMerchantRoutes: Route =
    path("customers.create") {
      post {
        entity(as[CustomerMerchantUpsertion]) { creation =>
          authenticate { implicit user =>
            onSuccess(customerMerchantService.create(creation))(result => completeAsApiResponse(result))
          }
        }
      }
    } ~
      path("customers.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(customerMerchantService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("customers.get") {
        get {
          parameter("customer_id".as[UUID]) { id =>
            parameters(
              "loyalty_program_id".as[UUID].?,
              "updated_since".as[ZonedDateTime].?,
            ).as(CustomerFilters.forGet _) { filters =>
              expandParameters(
                "visits",
                "spend",
                "avg_tips",
                "loyalty_statuses",
                "loyalty_memberships",
                "billing_details",
              )(
                CustomerExpansions.forGet,
              ) { expansions =>
                authenticate { implicit user =>
                  onSuccess(customerMerchantService.findById(id, filters)(expansions)) { result =>
                    completeAsOptApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("customers.get_by") {
        get {
          parameter("loyalty_lookup_id".as[String]) { lookupId =>
            authenticate { implicit user =>
              onSuccess(customerMerchantService.findByLoyaltyMembershipsLookupId(lookupId)) { result =>
                completeAsOptApiResponse(result)
              }
            }
          }
        }
      } ~
      path("customers.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "group_id".as[UUID].?,
              "q".?,
              "source[]".as[Seq[CustomerSourceAlias]].?,
              "updated_since".as[ZonedDateTime].?,
            ).as(CustomerFilters.forList _) { filters =>
              expandParameters(
                "visits",
                "spend",
                "locations",
                "loyalty_programs",
                "loyalty_statuses",
                "loyalty_memberships",
                "billing_details",
              )(CustomerExpansions.forList) { expansions =>
                authenticate { implicit user =>
                  onSuccess(customerMerchantService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("customers.update") {
        post {
          parameter("customer_id".as[UUID]) { id =>
            entity(as[CustomerMerchantUpsertion]) { upsertion =>
              authenticate { implicit user =>
                onSuccess(customerMerchantService.update(id, upsertion))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("customers.sync") {
        post {
          parameter("customer_id".as[UUID]) { id =>
            entity(as[CustomerMerchantUpsertion]) { upsertion =>
              authenticate { implicit user =>
                onSuccess(customerMerchantService.sync(id, upsertion))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
