package io.paytouch.core.resources.comments

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GenericListCommentsFSpec extends CommentsFSpec {

  abstract class GenericListCommentsFSpecContext extends CommentsFSpecContext

  supportedResources foreach { resource =>
    s"GET /v1/${resource.namespace}.list_comments?${resource.parameter}=$$" in {
      "if request has valid token" in {

        s"if the ${resource.name} exists" should {
          s"return the list of comments associated to the ${resource.name}" in new GenericListCommentsFSpecContext {
            val item = resource.createItem(merchant, london, user)

            val comment1 = Factory.comment(user, item).create
            val comment2 = Factory.comment(user, item).create

            Get(s"/v1/${resource.namespace}.list_comments?${resource.parameter}=${item.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val comments = responseAs[PaginatedApiResponse[Seq[Comment]]].data
              comments.map(_.id) ==== Seq(comment1.id, comment2.id)

              assertCommentResponse(comments.find(_.id == comment1.id).get, comment1, user)
              assertCommentResponse(comments.find(_.id == comment2.id).get, comment2, user)
            }
          }
        }

        s"if the ${resource.name} does not belong to the merchant" should {
          "return 404" in new GenericListCommentsFSpecContext {
            val competitor = Factory.merchant.create
            val locationCompetitor = Factory.location(competitor).create
            val userCompetitor = Factory.user(competitor).create

            val item = resource.createItem(competitor, locationCompetitor, userCompetitor)

            Get(s"/v1/${resource.namespace}.list_comments?${resource.parameter}=${item.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }

        s"if the ${resource.name} does not exist" should {
          "return 404" in new GenericListCommentsFSpecContext {
            Get(s"/v1/${resource.namespace}.list_comments?${resource.parameter}=${UUID.randomUUID}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }

}
