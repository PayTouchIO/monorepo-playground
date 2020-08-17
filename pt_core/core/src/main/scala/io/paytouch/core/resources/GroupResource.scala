package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.{ GroupCreation, GroupUpdate, Ids }
import io.paytouch.core.expansions.GroupExpansions
import io.paytouch.core.filters.GroupFilters
import io.paytouch.core.services.GroupService

trait GroupResource extends JsonResource {

  def groupService: GroupService

  val groupRoutes: Route =
    path("groups.create") {
      post {
        parameters("group_id".as[UUID]) { id =>
          entity(as[GroupCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(groupService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("groups.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
            ).as(GroupFilters) { filters =>
              expandParameters(
                "customers",
                "customers_count",
                "revenue",
                "visits",
              )(GroupExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(groupService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("groups.get") {
        get {
          parameter("group_id".as[UUID]) { id =>
            expandParameters("customers")(GroupExpansions.apply) { expansions =>
              authenticate { implicit user =>
                onSuccess(groupService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("groups.update") {
        post {
          parameter("group_id".as[UUID]) { id =>
            entity(as[GroupUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(groupService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("groups.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(groupService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      }
}
