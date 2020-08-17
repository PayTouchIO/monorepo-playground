package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._
import org.scalacheck.Gen

import scala.concurrent._

object TimeCardSeeds extends Seeds {

  lazy val timeCardDao = daos.timeCardDao

  def load(shifts: Seq[ShiftRecord])(implicit user: UserRecord): Future[Seq[TimeCardRecord]] = {

    val timeCards = shifts.flatMap { shift =>
      (1 to TimeCardsPerShift).map { idx =>
        val startAt = genZonedDateTime.instance
        val endAt = startAt.plusMinutes(genInt.instance * 5)
        TimeCardUpdate(
          id = Some(s"Time card $idx ${shift.id}".toUUID),
          merchantId = Some(user.merchantId),
          userId = Some(shift.userId),
          locationId = Some(shift.locationId),
          shiftId = Some(shift.id),
          deltaMins = Some(genInt.instance),
          totalMins = Some(genInt.instance),
          regularMins = Some(genInt.instance),
          overtimeMins = Some(genInt.instance),
          unpaidBreakMins = Gen.option(genInt).instance,
          notes = Some(randomWords(5, allCapitalized = false)),
          startAt = Some(startAt),
          endAt = if (genBoolean.instance) Some(endAt) else None,
        )
      }
    }

    timeCardDao.bulkUpsert(timeCards).extractRecords
  }
}
