package io.paytouch.ordering.logging

import io.paytouch.logging.BaseHttpLogging
import io.paytouch.ordering.ServiceConfigurations
import io.paytouch.ordering.json.JsonSupport

trait HttpLogging extends BaseHttpLogging with JsonSupport {
  val logPostResponse = ServiceConfigurations.logPostResponse
  val responseTimeout = ServiceConfigurations.responseTimeout
}
