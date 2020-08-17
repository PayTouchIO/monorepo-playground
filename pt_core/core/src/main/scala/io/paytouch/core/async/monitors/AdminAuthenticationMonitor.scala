package io.paytouch.core.async.monitors

import java.time.ZonedDateTime

import akka.actor.Actor
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.AdminLogin

final case class SuccessfulAdminLogin(admin: AdminLogin, date: ZonedDateTime)

class AdminAuthenticationMonitor(implicit daos: Daos) extends Actor {

  val adminDao = daos.adminDao

  def receive: Receive = {
    case SuccessfulAdminLogin(admin, date) => recordLogin(admin, date)
  }

  def recordLogin(admin: AdminLogin, date: ZonedDateTime) =
    adminDao.recordLastLogin(admin.id, date)
}
