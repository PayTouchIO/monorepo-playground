package io.paytouch.core.clients.paytouch

import io.paytouch.core.clients.ClientSpec
import io.paytouch.core.entities._
import io.paytouch.core.utils.DefaultFixtures

trait PtClientSpec extends ClientSpec { test =>
  trait PtClientSpecContext extends ClientSpecContext with DefaultFixtures { self: PaytouchClient =>
    implicit val u: UserContext = userContext
    override implicit val ec = test.ec
  }
}
