package io.paytouch.core.processors

import io.paytouch.core.messages.entities.SQSMessage
import scala.concurrent._

trait Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Any]]
}
