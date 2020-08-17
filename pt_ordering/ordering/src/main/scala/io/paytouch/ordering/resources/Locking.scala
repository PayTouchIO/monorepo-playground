package io.paytouch.ordering.resources

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ Directive, Directive0, Directives, ValidationRejection }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import com.softwaremill.macwire._
import java.util.UUID
import io.paytouch.ordering.data.locking.EntityLock
import io.paytouch.ordering.data.redis.ConfiguredRedis
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.utils.LockingFailedRejection
import scala.concurrent._
import scala.util.{ Failure, Success }

trait Locking extends Directives {
  implicit def system: ActorSystem
  implicit def ec: ExecutionContext
  implicit def redis: ConfiguredRedis
  lazy val entityLock = wire[EntityLock]

  def lockEntity(name: ExposedName, uuid: UUID): Directive0 =
    extractRequestContext.flatMap { ctx =>
      val lockId = UUID.randomUUID
      // Delay execution until lock is obtained
      onSuccess(entityLock.lockWithRetry(name, uuid, lockId)).flatMap {
        case true =>
          mapResponse { resp =>
            // Attempt to unlock after request is complete (we don't care if it fails)
            entityLock.unlock(name, uuid, lockId)
            resp
          }
        case _ => reject(new LockingFailedRejection("Entity could not be locked"))
      }
    }
}
