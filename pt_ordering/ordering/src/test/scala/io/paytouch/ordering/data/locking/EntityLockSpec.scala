package io.paytouch.ordering.data.locking

import scala.concurrent._
import scala.concurrent.duration._

import com.softwaremill.macwire._

import com.redis._

import java.util.concurrent.TimeUnit
import java.util.UUID

import org.specs2.specification.Scope

import io.paytouch.ordering.data.redis._
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.utils._
import io.paytouch.utils.Tagging._

class EntityLockSpec extends PaytouchSpec {
  implicit val system = MockedRestApi.testAsyncSystem
  override implicit def ec: ExecutionContext = system.dispatcher

  abstract class EntityLockSpecContext extends Scope {
    implicit val redis: ConfiguredRedis = MockedRestApi.redis
    val entityLock: EntityLock = wire[EntityLock]
  }

  "when it can't connect to Redis" should {
    trait BadRedisFixtures extends EntityLockSpecContext {
      val host: String withTag RedisHost = "localhost".taggedWith[RedisHost]
      val port: Int withTag RedisPort = 12345.taggedWith[RedisPort]
      override implicit val redis = new ConfiguredRedis(host, port)
      override val entityLock: EntityLock = wire[EntityLock]
    }

    "it always obtains a lock" in new BadRedisFixtures {
      val id = UUID.randomUUID
      val lockId1 = UUID.randomUUID
      val lockId2 = UUID.randomUUID
      entityLock.lock(ExposedName.Cart, id, lockId1).await ==== true
      entityLock.lock(ExposedName.Cart, id, lockId2).await ==== true
    }

    "it always releases a lock" in new BadRedisFixtures {
      val id = UUID.randomUUID
      val lockId = UUID.randomUUID
      entityLock.unlock(ExposedName.Cart, id, lockId).await ==== true
    }

    "the entity is always unlocked" in new BadRedisFixtures {
      val id = UUID.randomUUID
      val lockId = UUID.randomUUID
      entityLock.lock(ExposedName.Cart, id, lockId).await ==== true
      entityLock.locked(ExposedName.Cart, id).await ==== false
    }
  }

  "it can be locked and unlocked" in new EntityLockSpecContext {
    val id = UUID.randomUUID
    val lockId = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id, lockId).await ==== true
    entityLock.locked(ExposedName.Cart, id).await ==== true

    entityLock.unlock(ExposedName.Cart, id, lockId).await ==== true
    entityLock.locked(ExposedName.Cart, id).await ==== false
  }

  "it can't be locked while another lock is active for the entity" in new EntityLockSpecContext {
    val id = UUID.randomUUID
    val lockId1 = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id, lockId1).await ==== true

    val lockId2 = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id, lockId2).await ==== false

    entityLock.unlock(ExposedName.Cart, id, lockId1).await ==== true

    entityLock.lock(ExposedName.Cart, id, lockId2).await ==== true
  }

  "it can be locked while another lock is active for a different entity" in new EntityLockSpecContext {
    val id1 = UUID.randomUUID
    val lockId1 = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id1, lockId1).await ==== true

    val id2 = UUID.randomUUID
    val lockId2 = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id2, lockId2).await ==== true
  }

  "it expires after the ttl" in new EntityLockSpecContext {
    val ttl = 500.millis
    val id = UUID.randomUUID
    val lockId = UUID.randomUUID
    entityLock.lock(ExposedName.Cart, id, lockId, ttl).await ==== true
    entityLock.locked(ExposedName.Cart, id).await ==== true

    Thread.sleep(100)
    entityLock.locked(ExposedName.Cart, id).await ==== true

    Thread.sleep(500)
    entityLock.locked(ExposedName.Cart, id).await ==== false
  }

  "lock with retry" should {
    "retry until the lock is available" in new EntityLockSpecContext {
      val id = UUID.randomUUID
      val lockId1 = UUID.randomUUID
      entityLock.lock(ExposedName.Cart, id, lockId1).await ==== true

      val lockId2 = UUID.randomUUID
      val future = entityLock.lockWithRetry(ExposedName.Cart, id, lockId2)
      future.isCompleted ==== false

      Thread.sleep(150)
      future.isCompleted ==== false
      entityLock.unlock(ExposedName.Cart, id, lockId1).await ==== true
      entityLock.locked(ExposedName.Cart, id).await ==== false

      Thread.sleep(150)
      future.isCompleted ==== true
      future.await ==== true
      entityLock.locked(ExposedName.Cart, id).await ==== true
    }

    "return false when the lock cannot be obtained" in new EntityLockSpecContext {
      val ttl = 500.millis
      val id = UUID.randomUUID
      val lockId1 = UUID.randomUUID
      entityLock.lock(ExposedName.Cart, id, lockId1).await ==== true

      val lockId2 = UUID.randomUUID
      val future = entityLock.lockWithRetry(ExposedName.Cart, id, lockId2, ttl, 1)
      future.isCompleted ==== false

      Thread.sleep(50)
      future.isCompleted ==== false
      entityLock.locked(ExposedName.Cart, id).await ==== true

      Thread.sleep(100)
      future.isCompleted ==== true
      future.await ==== false
    }
  }
}
