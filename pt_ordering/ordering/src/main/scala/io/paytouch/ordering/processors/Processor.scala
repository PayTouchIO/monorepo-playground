package io.paytouch.ordering.processors

import io.paytouch.ordering.messages.entities.PtOrderingMsg
import scala.concurrent._

trait Processor {
  def execute: PartialFunction[PtOrderingMsg[_], Future[Any]]
}
