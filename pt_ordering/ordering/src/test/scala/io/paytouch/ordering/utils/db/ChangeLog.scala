package io.paytouch.ordering.utils.db

import java.io.File

object ChangeLog {
  val changelog: String =
    getClass.getResource("/migrations/changelog.yml").getFile
}
