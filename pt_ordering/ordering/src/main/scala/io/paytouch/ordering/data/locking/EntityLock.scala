package io.paytouch.ordering.data.locking

import java.io.IOException
import java.util.concurrent.{ Callable, TimeUnit }
import java.util.UUID

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import akka.actor.ActorSystem
import akka.pattern.Patterns.after

import com.redis._
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.data.redis._
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.utils.Tagging._

// Uses the single instance version of the 'redlock' algorithm https://redis.io/topics/distlock
class EntityLock(val redis: ConfiguredRedis)(implicit val system: ActorSystem, val ec: ExecutionContext) {
  final val retryDuration = FiniteDuration(100, TimeUnit.MILLISECONDS)
  final val retryCount = 50
  final val lockTtl = 10.seconds

  // Retry once every 100ms, a maximum of 50 times = 5 seconds
  def lockWithRetry(
      name: ExposedName,
      uuid: UUID,
      lockId: UUID,
      ttl: Duration = lockTtl,
      remainingRetrys: Integer = retryCount,
    ): Future[Boolean] = {
    val future = lock(name, uuid, lockId, ttl)
    future.transformWith {
      case Success(true) => Future.successful(true)
      case _ if remainingRetrys > 0 =>
        val callable: Callable[Future[Int]] =
          () => Future.successful(1)

        after(retryDuration, system.scheduler, ec, callable).flatMap { _ =>
          lockWithRetry(name, uuid, lockId, ttl, remainingRetrys - 1)
        }
      case _ => Future.successful(false)
    }
  }

  def lock(
      name: ExposedName,
      uuid: UUID,
      lockId: UUID,
      ttl: Duration = lockTtl,
    ): Future[Boolean] = {
    val key = keyName(name, uuid)
    val value = lockId.toString()
    Future {
      try {
        val client = redis.connect()
        val result = client.set(key, value, whenSet = api.StringApi.NX, expire = ttl)
        client.disconnect
        result
      }
      catch {
        // redis-client catches and rethrows underlying exceptions as
        // RuntimeException. The only thing we want to handle is IOException,
        // i.e. if Redis is not running.
        case ex: RuntimeException if ex.getCause.isInstanceOf[IOException] =>
          true
      }
    }
  }

  def unlock(
      name: ExposedName,
      uuid: UUID,
      lockId: UUID,
    ): Future[Boolean] = {
    val key = keyName(name, uuid)
    Future {
      try {
        val client = redis.connect()
        val result = client.get(key) match {
          // Locked by us
          case Some(value: String) if value == lockId.toString() =>
            client.del(key)
            true

          // Locked by someone else
          case Some(_: String) => false

          // Not locked, TTL expired or an error occured
          case _ => true
        }
        client.disconnect
        result
      }
      catch {
        case ex: RuntimeException if ex.getCause.isInstanceOf[IOException] =>
          true
      }
    }
  }

  def locked(name: ExposedName, uuid: UUID): Future[Boolean] = {
    val key = keyName(name, uuid)
    Future {
      try {
        val client = redis.connect()
        val result = client.get(key) match {
          case Some(_: String) => true
          case None            => false
        }
        client.disconnect
        result
      }
      catch {
        case ex: RuntimeException if ex.getCause.isInstanceOf[IOException] =>
          false
      }
    }
  }

  private def keyName(name: ExposedName, uuid: UUID) = s"lock-entity-${name}-${uuid}"
}
