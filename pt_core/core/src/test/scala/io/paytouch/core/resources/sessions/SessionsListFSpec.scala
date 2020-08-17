package io.paytouch.core.resources.sessions

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource.PtDashboard
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SessionsListFSpec extends SessionsFSpec {

  abstract class SessionsListFSpecContext extends SessionResourceFSpecContext

  "GET /v1/sessions.list" in {
    "if request has valid token" should {
      "with no filters" should {
        "return a paginated list of sessions" in new SessionResourceFSpecContext {
          val session1 = Factory.session(user, PtDashboard)
          val session2 = Factory.session(user, PtDashboard)

          val otherUser = Factory.user(merchant).create
          val sessionOtherUser = Factory.session(otherUser, PtDashboard)

          Get("/v1/sessions.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val sessions = responseAs[PaginatedApiResponse[Seq[Session]]].data
            sessions.map(_.id) must containTheSameElementsAs(Seq(initialSession.id, session1.id, session2.id))

            assertResponse(sessions.find(_.id == initialSession.id).get, initialSession)
            assertResponse(sessions.find(_.id == session1.id).get, session1)
            assertResponse(sessions.find(_.id == session2.id).get, session2)
          }
        }
      }
      "with oauth2_app_name filter" should {
        "return a paginated list of sessions matching that app" in new SessionResourceFSpecContext {
          val oauthApp = Factory.oauthApp(name = Some("Amaka")).create

          val session1 = Factory.session(user, PtDashboard)
          Factory.oauthAppSession(merchant, oauthApp, session1).create
          val session2 = Factory.session(user, PtDashboard)

          val otherUser = Factory.user(merchant).create
          val sessionOtherUser = Factory.session(otherUser, PtDashboard)

          Get("/v1/sessions.list?oauth2_app_name=AmaKa").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val sessions = responseAs[PaginatedApiResponse[Seq[Session]]].data
            sessions.map(_.id) must containTheSameElementsAs(Seq(session1.id))

            assertResponse(sessions.find(_.id == session1.id).get, session1)
          }
        }
      }
    }
  }

}
