package io.paytouch.core.services

import io.paytouch.core.entities.{ Country, State, TimeZone }
import io.paytouch.core.utils.{ StateUtils, TimezoneUtils }

object UtilService {
  object Geo extends StateUtils with TimezoneUtils
}
