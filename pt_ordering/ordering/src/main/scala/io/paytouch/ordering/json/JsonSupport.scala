package io.paytouch.ordering.json

import io.paytouch.json.BaseJsonSupport
import io.paytouch.json.json4s.BaseJson4sSupport

object JsonSupport extends JsonSupport

trait JsonSupport extends BaseJsonSupport with BaseJson4sSupport with Json4Formats
