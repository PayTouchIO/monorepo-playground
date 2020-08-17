package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object ShiftSeeds extends Seeds {

  lazy val shiftDao = daos.shiftDao

  def load(
      locations: Seq[LocationRecord],
      employees: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ShiftRecord]] = {

    val shifts = employees.flatMap { employee =>
      (1 to ShiftsPerEmployee).map { idx =>
        val startDate = genZonedDateTimeInTheFuture.instance.toLocalDate
        val endDate = startDate.plusDays(genInt.instance)
        val startTime = genZonedDateTimeInTheFuture.instance.toLocalTime
        val endTime = startTime.plusMinutes(genInt.instance * 5)
        ShiftUpdate(
          id = Some(s"Shifts $idx ${employee.id}".toUUID),
          merchantId = Some(employee.merchantId),
          userId = Some(employee.id),
          locationId = Some(locations.random.id),
          startDate = Some(startDate),
          endDate = Some(endDate),
          startTime = Some(startTime),
          endTime = Some(endTime),
          unpaidBreakMins = genOptInt.instance,
          repeat = genOptBoolean.instance,
          frequencyInterval = Some(genFrequencyInterval.instance),
          frequencyCount = genOptInt.instance,
          sunday = genOptBoolean.instance,
          monday = genOptBoolean.instance,
          tuesday = genOptBoolean.instance,
          wednesday = genOptBoolean.instance,
          thursday = genOptBoolean.instance,
          friday = genOptBoolean.instance,
          saturday = genOptBoolean.instance,
          status = Some(genShiftStatus.instance),
          bgColor = Some(genColor.instance),
          sendShiftStartNotification = genOptBoolean.instance,
          notes = Some(randomWords),
        )
      }
    }

    shiftDao.bulkUpsert(shifts).extractRecords
  }
}
