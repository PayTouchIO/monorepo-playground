package io.paytouch.ordering.clients.google

import io.paytouch.ordering.clients.ClientSpec

trait GClientSpec extends ClientSpec {

  trait GClientSpecContext extends GClient with ClientSpecContext
}
