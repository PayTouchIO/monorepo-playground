package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.utils.{ DefaultFixtures, UtcTime, FixtureDaoFactory => Factory }

class SessionDaoSpec extends DaoSpec {

  abstract class SessionDaoSpecContext extends DaoSpecContext with DefaultFixtures {
    def reloadSession(id: UUID) = sessionDao.findById(id).await.get

    def createAndSetSessionUpdatedAt(updatedAt: ZonedDateTime) = {
      val baseSession = Factory.session(user, source)
      sessionDao
        .updateUpdatedAt(baseSession.id, updatedAt)
        .map(_ => reloadSession(baseSession.id))
        .await
    }
  }

  "SessionDao" in {
    "field updatedAt" should {
      "update after 5 minutes since last update" in new SessionDaoSpecContext {
        val session = createAndSetSessionUpdatedAt(UtcTime.now.minusMinutes(6))
        val myJwtToken = generateUserJsonWebToken(session)

        sessionDao.access(myJwtToken).await

        // for performance reasons `access()` triggers the record update and returns without waiting for the future to complete
        afterAWhile {
          val reloadedSession = reloadSession(session.id)
          reloadedSession.updatedAt isAfter session.updatedAt should beTrue
          reloadedSession ==== session.copy(updatedAt = reloadedSession.updatedAt)
        }
      }

      "not update before 5 minutes since last update" in new SessionDaoSpecContext {
        val session = createAndSetSessionUpdatedAt(UtcTime.now.minusMinutes(4))
        val myJwtToken = generateUserJsonWebToken(session)

        sessionDao.access(myJwtToken).await

        val reloadedSession = reloadSession(session.id)
        reloadedSession === session
      }
    }
  }
}
