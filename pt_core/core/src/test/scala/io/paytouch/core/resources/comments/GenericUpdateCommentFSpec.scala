package io.paytouch.core.resources.comments

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GenericUpdateCommentFSpec extends CommentsFSpec {

  abstract class GenericUpdateCommentFSpecContext extends CommentsFSpecContext

  supportedResources foreach { resource =>
    s"POST /v1/${resource.namespace}.update_comment?comment_id=$$" in {
      "if request has valid token" in {

        "if comment exist" should {
          s"if the ${resource.name} exists" should {
            "update a comment" in new GenericUpdateCommentFSpecContext {
              val item = resource.createItem(merchant, london, user)

              val comment = Factory.comment(user, item).create

              @scala.annotation.nowarn("msg=Auto-application")
              val commentUpdate =
                random[CommentUpdate].copy(objectId = Some(item.id), objectType = None)

              Post(s"/v1/${resource.namespace}.update_comment?comment_id=${comment.id}", commentUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()

                val commentResponse = responseAs[ApiResponse[Comment]].data
                assertCommentUpdate(comment.id, commentUpdate.copy(objectType = Some(CommentType.PurchaseOrder)))
                assertCommentResponseById(comment.id, commentResponse, user)
              }
            }
          }
          "if comment is not own" should {
            "return 404" in new GenericUpdateCommentFSpecContext {
              val anotherUser = Factory.user(merchant).create
              val item = resource.createItem(merchant, london, user)

              val comment = Factory.comment(anotherUser, item).create

              @scala.annotation.nowarn("msg=Auto-application")
              val commentUpdate =
                random[CommentUpdate].copy(objectId = Some(item.id), objectType = None)

              Post(s"/v1/${resource.namespace}.update_comment?comment_id=${comment.id}", commentUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }
          s"if the ${resource.name} doesn't exist" should {
            "return 404" in new GenericUpdateCommentFSpecContext {
              val item = resource.createItem(merchant, london, user)

              val comment = Factory.comment(user, item).create

              @scala.annotation.nowarn("msg=Auto-application")
              val commentUpdate =
                random[CommentUpdate].copy(objectId = Some(UUID.randomUUID), objectType = None)

              Post(s"/v1/${resource.namespace}.update_comment?comment_id=${comment.id}", commentUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }
        }

        s"if the ${resource.name} does not belong to the merchant" should {
          "return 404" in new GenericUpdateCommentFSpecContext {
            val competitor = Factory.merchant.create
            val locationCompetitor = Factory.location(competitor).create
            val userCompetitor = Factory.user(competitor).create
            val itemCompetitor = resource.createItem(competitor, locationCompetitor, userCompetitor)

            val competitorComment = Factory.comment(userCompetitor, itemCompetitor).create

            @scala.annotation.nowarn("msg=Auto-application")
            val competitorCommentUpdate =
              random[CommentUpdate].copy(objectId = Some(itemCompetitor.id))

            Post(
              s"/v1/${resource.namespace}.update_comment?comment_id=${competitorComment.id}",
              competitorCommentUpdate,
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }

        "if the comment does not exist" should {
          "return 404" in new GenericUpdateCommentFSpecContext {
            @scala.annotation.nowarn("msg=Auto-application")
            val comment = random[CommentUpdate]

            Post(s"/v1/${resource.namespace}.update_comment?comment_id=${UUID.randomUUID}", comment)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
}
