package io.paytouch.core.async.monitors

import java.time.ZonedDateTime

import scala.concurrent._

import akka.actor.Actor

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource

final case class SuccessfulLogin(
    user: UserLogin,
    loginSource: LoginSource,
    date: ZonedDateTime,
  )

class AuthenticationMonitor(implicit daos: Daos) extends Actor {
  val userDao = daos.userDao

  def receive: Receive = {
    case SuccessfulLogin(user, loginSource, date) =>
      loginSource match {
        case LoginSource.PtDashboard => userDao.recordDashboardLogin(user.email, date)
        case LoginSource.PtRegister  => userDao.recordRegisterLogin(user.email, date)
        case LoginSource.PtTickets   => userDao.recordTicketsLogin(user.email, date)
        case _                       => Future.successful(true)
      }
  }
}
