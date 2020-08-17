package io.paytouch.core.data.driver

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.str.PgStringSupport
import io.paytouch.core.utils.Formatters

trait CustomPostgresDriver
    extends ExPostgresProfile
       with PgDateSupport
       with PgDate2Support
       with PgStringSupport
       with CustomPgJsonSupport
       with CustomPgColumnsSupport {

  override val api: API = new API {}

  @scala.annotation.nowarn("msg=shadow")
  trait API
      extends super.API
      with SimpleDateTimeImplicits
      with DateTimeImplicits
      with PgStringImplicits
      with CustomJsonImplicits
      with CustomImplicits {
    override val date2TzDateTimeFormatter = Formatters.TimestampFormatter
  }

  val plainApi = new API with Date2DateTimePlainImplicits with CustomJsonPlainImplicits with CustomPlainImplicits {}
}

object CustomPostgresDriver extends CustomPostgresDriver
