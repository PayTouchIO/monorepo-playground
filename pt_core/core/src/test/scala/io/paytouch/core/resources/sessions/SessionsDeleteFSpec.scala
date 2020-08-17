package io.paytouch.core.resources.sessions

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SessionsDeleteFSpec extends SessionsFSpec {

  abstract class SessionDeleteResourceFSpecContext extends SessionResourceFSpecContext {
    def assertSessionDeleted(id: UUID) =
      sessionDao.findById(id).await should beNone
    def assertSessionNotDeleted(id: UUID) = sessionDao.findById(id).await should beSome
  }

  "POST /v1/sessions.delete" in {

    "if request has valid token" in {
      "if session doesn't exist" should {
        "do nothing and return 204" in new SessionDeleteResourceFSpecContext {
          val nonExistingSessionId = UUID.randomUUID

          Post(s"/v1/sessions.delete", Ids(ids = Seq(nonExistingSessionId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertSessionDeleted(nonExistingSessionId)
          }
        }
      }

      "if session belongs to the user and is  the current one" should {
        "do nothing and return 204" in new SessionDeleteResourceFSpecContext {
          Post(s"/v1/sessions.delete", Ids(ids = Seq(initialSession.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertSessionDeleted(initialSession.id)
          }
        }
      }

      "if session belongs to the user and is not the current one" should {
        "delete the session and return 204" in new SessionDeleteResourceFSpecContext {
          val session = Factory.session(user)

          Post(s"/v1/sessions.delete", Ids(ids = Seq(session.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertSessionDeleted(session.id)
          }
        }
      }

      "if session belongs to a different user" should {
        "do not delete the session and return 204" in new SessionDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(merchant).create
          val competitorSession = Factory.session(competitorUser)

          Post(s"/v1/sessions.delete", Ids(ids = Seq(competitorSession.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertSessionNotDeleted(competitorSession.id)
          }
        }
      }
    }
  }
}
