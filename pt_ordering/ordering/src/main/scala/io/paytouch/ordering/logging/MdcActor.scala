package io.paytouch.ordering.logging

import io.paytouch.logging._
import io.paytouch.ordering.clients.paytouch.core.JwtTokenKeys

class MdcActor extends BaseMdcActor with JwtTokenKeys {
  implicit val executionContext = context.dispatcher
}
