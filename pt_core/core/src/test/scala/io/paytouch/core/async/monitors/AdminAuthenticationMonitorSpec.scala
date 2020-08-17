package io.paytouch.core.async.monitors

import akka.actor.Props
import io.paytouch.core.entities.{ AdminLogin, AdminLoginCredentials }
import io.paytouch.core.utils.{ AdminFixtures, UtcTime }

class AdminAuthenticationMonitorSpec extends MonitorSpec {

  abstract class AdminAuthenticationMonitorSpecContext extends MonitorSpecContext with AdminFixtures {
    val adminDao = daos.adminDao

    lazy val monitor = monitorSystem.actorOf(Props(new AdminAuthenticationMonitor))
  }

  "AdminAuthenticationMonitor" should {

    "record last login" in new AdminAuthenticationMonitorSpecContext {
      val adminLogin = AdminLogin.fromRecord(admin)
      val date = UtcTime.now
      monitor ! SuccessfulAdminLogin(adminLogin, date)

      afterAWhile {
        val record = adminDao.findByEmail(admin.email).await.get
        record.lastLoginAt ==== Some(date)
      }
    }
  }
}
