package io.paytouch.ordering.utils

import scala.concurrent._
import scala.concurrent.duration._

trait FutureHelpers extends TestExecutionContext {
  final implicit class AwaitableFuture[T](val future: Future[T]) {
    def await(implicit duration: FiniteDuration = 45.seconds): T =
      Await.result(future, duration)
  }
}
