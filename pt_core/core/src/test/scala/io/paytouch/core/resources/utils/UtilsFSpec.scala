package io.paytouch.core.resources.utils

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

abstract class UtilsFSpec extends FSpec {
  abstract class UtilResourceFSpecContext extends FSpecContext with JsonSupport with DefaultFixtures
}
