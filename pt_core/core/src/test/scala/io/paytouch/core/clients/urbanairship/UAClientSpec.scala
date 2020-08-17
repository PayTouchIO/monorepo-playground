package io.paytouch.core.clients.urbanairship

import io.paytouch.core.clients.ClientSpec

trait UAClientSpec extends ClientSpec {

  trait UAClientSpecContext extends UAClient with ClientSpecContext
}
