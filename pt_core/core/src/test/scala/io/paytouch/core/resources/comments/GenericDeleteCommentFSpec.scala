package io.paytouch.core.resources.comments

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GenericDeleteCommentFSpec extends CommentsFSpec {

  abstract class GenericDeleteCommentFSpecContext extends CommentsFSpecContext

  supportedResources foreach { resource =>
    s"POST /v1/${resource.namespace}s.delete_comment?comment_id=$$" in {
      "if request has valid token" in {

        s"if the ${resource.name} exists and user owns the comment" should {
          "delete a comment" in new GenericDeleteCommentFSpecContext {
            val item = resource.createItem(merchant, london, user)

            val comment = Factory.comment(user, item).create

            Post(s"/v1/${resource.namespace}.delete_comment?comment_id=${comment.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NoContent)

              assertCommentDeleted(comment.id)
            }
          }
        }

        s"if the ${resource.name} exists and user doesn't own the comment" should {
          "delete a comment" in new GenericDeleteCommentFSpecContext {
            val item = resource.createItem(merchant, london, user)

            val anotherUser = Factory.user(merchant).create
            val comment = Factory.comment(anotherUser, item).create

            Post(s"/v1/${resource.namespace}.delete_comment?comment_id=${comment.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
              assertCommentWasntDeleted(comment.id)
            }
          }
        }

        s"if the ${resource.name} does not belong to the merchant" should {
          "return 404" in new GenericDeleteCommentFSpecContext {
            val competitor = Factory.merchant.create
            val locationCompetitor = Factory.location(competitor).create
            val userCompetitor = Factory.user(competitor).create

            val item = resource.createItem(competitor, locationCompetitor, userCompetitor)

            val competitorComment = Factory.comment(userCompetitor, item).create

            Post(s"/v1/${resource.namespace}.delete_comment?comment_id=${competitorComment.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
              assertCommentWasntDeleted(competitorComment.id)
            }
          }
        }

        s"if the ${resource.name} does not exist" should {
          "return 404" in new GenericDeleteCommentFSpecContext {
            Post(s"/v1/${resource.namespace}.delete_comment?comment_id=${UUID.randomUUID}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
}
