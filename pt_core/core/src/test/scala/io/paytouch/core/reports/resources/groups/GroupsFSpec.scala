package io.paytouch.core.reports.resources.groups

import io.paytouch.core.reports.resources.ReportsFSpec
import io.paytouch.core.reports.views.GroupView

abstract class GroupsFSpec extends ReportsFSpec[GroupView] {

  def view = GroupView

  val fixtures = new GroupsFSpecContext

  class GroupsFSpecContext extends ReportsFSpecContext with GroupsFSpecFixtures

}
