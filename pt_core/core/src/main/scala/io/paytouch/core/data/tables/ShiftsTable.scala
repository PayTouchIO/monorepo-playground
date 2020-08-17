package io.paytouch.core.data.tables

import java.time.{ LocalDate, LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.model.ShiftRecord
import io.paytouch.core.data.model.enums.{ FrequencyInterval, ShiftStatus }
import shapeless.{ Generic, HNil }
import slickless._

class ShiftsTable(tag: Tag) extends SlickMerchantTable[ShiftRecord](tag, "shifts") with OneToOneLocationColumns {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def locationId = column[UUID]("location_id")

  def startDate = column[LocalDate]("start_date")
  def endDate = column[LocalDate]("end_date")
  def startTime = column[LocalTime]("start_time")
  def endTime = column[LocalTime]("end_time")

  def unpaidBreakMins = column[Option[Int]]("unpaid_break_mins")
  def repeat = column[Boolean]("repeat")
  def frequencyInterval = column[Option[FrequencyInterval]]("frequency_interval")
  def frequencyCount = column[Option[Int]]("frequency_count")

  def sunday = column[Option[Boolean]]("sunday")
  def monday = column[Option[Boolean]]("monday")
  def tuesday = column[Option[Boolean]]("tuesday")
  def wednesday = column[Option[Boolean]]("wednesday")
  def thursday = column[Option[Boolean]]("thursday")
  def friday = column[Option[Boolean]]("friday")
  def saturday = column[Option[Boolean]]("saturday")

  def status = column[Option[ShiftStatus]]("status")
  def bgColor = column[Option[String]]("bg_color")
  def sendShiftStartNotification = column[Boolean]("send_shift_start_notification")
  def notes = column[Option[String]]("notes")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = {
    val shiftGeneric = Generic[ShiftRecord]
    (id :: merchantId :: userId :: locationId ::
      startDate :: endDate :: startTime :: endTime ::
      unpaidBreakMins :: repeat :: frequencyInterval :: frequencyCount ::
      sunday :: monday :: tuesday :: wednesday :: thursday :: friday :: saturday ::
      status :: bgColor :: sendShiftStartNotification :: notes ::
      createdAt :: updatedAt :: HNil).<>(
      (dbRow: shiftGeneric.Repr) => shiftGeneric.from(dbRow),
      (caseClass: ShiftRecord) => Some(shiftGeneric.to(caseClass)),
    )
  }
}
