package io.paytouch.utils

import scala.concurrent._
import scala.concurrent.duration._

trait FutureHelpers extends TestExecutionContext {
  final implicit class AwaitableFuture[T](val f: Future[T]) {
    def await(implicit duration: FiniteDuration = 45.seconds): T =
      Await.result(f, duration)
  }
}
