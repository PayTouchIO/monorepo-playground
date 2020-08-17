package io.paytouch.core.entities

import java.time.LocalTime

import io.paytouch.core.data.model.ShiftRecord
import io.paytouch.core.utils.PaytouchSpec
import org.specs2.specification.{ Scope => SpecScope }

class ShiftRecordSpec extends PaytouchSpec {

  abstract class ShiftRecordSpecContext extends SpecScope

  "ShiftRecord" should {
    "isContainedBy should work as expected" in {
      "when time overlaps between days" in new ShiftRecordSpecContext {
        val shift = random[ShiftRecord].copy(startTime = LocalTime.of(20, 0), endTime = LocalTime.of(4, 0))

        shift.isContainedBy(start = LocalTime.of(19, 0), end = LocalTime.of(5, 0)) ==== true
        shift.isContainedBy(start = LocalTime.of(19, 0), end = LocalTime.of(3, 0)) ==== false
        shift.isContainedBy(start = LocalTime.of(8, 0), end = LocalTime.of(14, 0)) ==== false
        shift.isContainedBy(start = LocalTime.of(14, 0), end = LocalTime.of(21, 0)) ==== false
        shift.isContainedBy(start = LocalTime.of(3, 0), end = LocalTime.of(6, 0)) ==== false
        shift.isContainedBy(start = LocalTime.of(8, 0), end = LocalTime.of(9, 0)) ==== false
      }
    }

    "when time does not overlap between days" in new ShiftRecordSpecContext {
      val shift = random[ShiftRecord].copy(startTime = LocalTime.of(9, 0), endTime = LocalTime.of(18, 0))

      shift.isContainedBy(start = LocalTime.of(8, 0), end = LocalTime.of(19, 0)) ==== true
      shift.isContainedBy(start = LocalTime.of(14, 0), end = LocalTime.of(18, 0)) ==== false
      shift.isContainedBy(start = LocalTime.of(6, 0), end = LocalTime.of(7, 0)) ==== false
      shift.isContainedBy(start = LocalTime.of(20, 0), end = LocalTime.of(21, 0)) ==== false
      shift.isContainedBy(start = LocalTime.of(17, 0), end = LocalTime.of(6, 0)) ==== false
    }
  }

}
