package io.paytouch.ordering.resources

import java.util.UUID
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Rejection, RejectionHandler, Route }
import akka.http.scaladsl.model.StatusCodes
import io.paytouch.ordering.data.locking.EntityLock
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.utils._
import scala.concurrent.ExecutionContext
import com.softwaremill.macwire._

class LockingFSpec extends FSpec {
  abstract class LockingFSpecContext extends FSpecContext with Locking with TestExecutionContext {
    implicit val system = MockedRestApi.testAsyncSystem
    override implicit def ec: ExecutionContext = system.dispatcher
    lazy val redis = MockedRestApi.redis

    implicit def myRejectionHandler: RejectionHandler =
      RejectionHandler
        .newBuilder()
        .handleAll[LockingFailedRejection](rejections => complete(StatusCodes.ServiceUnavailable, ""))
        .result()

    val route = get {
      parameters("id") { id =>
        val uuid = UUID.fromString(id)
        lockEntity(ExposedName.Cart, uuid) {
          entityLock.locked(ExposedName.Cart, uuid).await ==== true
          complete("ok")
        }
      }
    }
  }

  "when lock is obtained" in {
    "request proceeds and lock is released" in new LockingFSpecContext {
      val id = UUID.randomUUID

      entityLock.locked(ExposedName.Cart, id).await ==== false

      Get(s"?id=$id") ~> route ~> check {
        assertStatusOK()
      }

      Thread.sleep(50)

      entityLock.locked(ExposedName.Cart, id).await ==== false
    }
  }

  "when lock cannot be obtained" in {
    "request is rejected" in new LockingFSpecContext {
      val id = UUID.randomUUID
      val lockId = UUID.randomUUID
      entityLock.lock(ExposedName.Cart, id, lockId).await ==== true

      Get(s"?id=$id") ~> Route.seal(route) ~> check {
        assertStatus(StatusCodes.ServiceUnavailable)
      }
    }
  }
}
