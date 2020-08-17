package io.paytouch.ordering.utils

import scala.concurrent.ExecutionContext

trait TestExecutionContext {
  implicit def ec: ExecutionContext = GlobalExecutionContext.ec
}

object GlobalExecutionContext {
  val ec = scala.concurrent.ExecutionContext.global
}
