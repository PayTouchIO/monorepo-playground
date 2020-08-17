package io.paytouch.core.async.monitors

import akka.actor.Props

import io.paytouch.core.entities.LoginCredentials
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.utils.UtcTime

class AuthenticationMonitorSpec extends MonitorSpec {
  abstract class AuthenticationMonitorSpecContext extends MonitorSpecContext with MonitorStateFixtures {
    val userDao = daos.userDao

    lazy val monitor = monitorSystem.actorOf(Props(new AuthenticationMonitor))
  }

  "AuthenticationMonitor" should {

    "record a pt_dashboard login" in new AuthenticationMonitorSpecContext {
      val credentials = LoginCredentials(user.email, password, LoginSource.PtDashboard)
      val date = UtcTime.now
      val login = userDao.findUserLoginByEmail(user.email).await.get
      monitor ! SuccessfulLogin(login, credentials.source, date)

      afterAWhile {
        val record = userDao.findByEmail(user.email).await.get
        record.dashboardLastLoginAt ==== Some(date)
      }
    }

    "record a pt_register login" in new AuthenticationMonitorSpecContext {
      val credentials = LoginCredentials(user.email, password, LoginSource.PtRegister)
      val date = UtcTime.now
      val login = userDao.findUserLoginByEmail(user.email).await.get
      monitor ! SuccessfulLogin(login, credentials.source, date)

      afterAWhile {
        val record = userDao.findByEmail(user.email).await.get
        record.registerLastLoginAt ==== Some(date)
      }
    }

    "record a pt_tickets login" in new AuthenticationMonitorSpecContext {
      val credentials = LoginCredentials(user.email, password, LoginSource.PtTickets)
      val date = UtcTime.now
      val login = userDao.findUserLoginByEmail(user.email).await.get
      monitor ! SuccessfulLogin(login, credentials.source, date)

      afterAWhile {
        val record = userDao.findByEmail(user.email).await.get
        record.ticketsLastLoginAt ==== Some(date)
      }
    }
  }
}
