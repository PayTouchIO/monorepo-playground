package io.paytouch.core.resources.admin.reports

import io.paytouch.core.entities.{ Admin => AdminEntity }
import io.paytouch.core.utils._

abstract class ReportsFSpec extends FSpec {

  abstract class ReportsFSpecContext extends FSpecContext with AdminFixtures

}
