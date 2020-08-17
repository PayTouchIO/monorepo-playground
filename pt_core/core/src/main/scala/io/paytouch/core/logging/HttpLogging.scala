package io.paytouch.core.logging

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.ServiceConfigurations
import io.paytouch.logging._

trait HttpLogging extends BaseHttpLogging with JsonSupport {
  val logPostResponse = ServiceConfigurations.logPostResponse
  val responseTimeout = ServiceConfigurations.responseTimeout
}
