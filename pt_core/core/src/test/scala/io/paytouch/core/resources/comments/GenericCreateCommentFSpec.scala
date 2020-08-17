package io.paytouch.core.resources.comments

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GenericCreateCommentFSpec extends CommentsFSpec {

  abstract class GenericCreateCommentFSpecContext extends CommentsFSpecContext

  supportedResources foreach { resource =>
    s"POST /v1/${resource.namespace}.create_comment?comment_id=$$" in {
      "if request has valid token" in {

        s"if the ${resource.name} exists" should {
          "create a comment" in new GenericCreateCommentFSpecContext {
            val item = resource.createItem(merchant, london, user)

            val commentUuid = UUID.randomUUID

            @scala.annotation.nowarn("msg=Auto-application")
            val commentCreation =
              random[CommentCreation].copy(objectId = item.id, objectType = None)

            Post(s"/v1/${resource.namespace}.create_comment?comment_id=$commentUuid", commentCreation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val commentResponse = responseAs[ApiResponse[Comment]].data
              assertCommentCreation(commentUuid, commentCreation.copy(objectType = Some(resource.commentType)))
              assertCommentResponseById(commentUuid, commentResponse, user)
            }
          }
        }

        s"if the ${resource.name} does not belong to the merchant" should {
          "return 404" in new GenericCreateCommentFSpecContext {
            val competitor = Factory.merchant.create
            val locationCompetitor = Factory.location(competitor).create
            val userCompetitor = Factory.user(competitor).create

            val item = resource.createItem(competitor, locationCompetitor, userCompetitor)

            val commentUuid = UUID.randomUUID

            @scala.annotation.nowarn("msg=Auto-application")
            val competitorComment = random[CommentCreation].copy(objectId = item.id)

            Post(s"/v1/${resource.namespace}.create_comment?comment_id=$commentUuid", competitorComment)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }

        s"if the ${resource.name} does not exist" should {
          "return 404" in new GenericCreateCommentFSpecContext {

            @scala.annotation.nowarn("msg=Auto-application")
            val comment = random[CommentCreation]
            Post(s"/v1/${resource.namespace}.create_comment?comment_id=${UUID.randomUUID}", comment)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
}
