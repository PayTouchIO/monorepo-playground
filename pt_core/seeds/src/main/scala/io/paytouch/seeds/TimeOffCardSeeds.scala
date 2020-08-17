package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object TimeOffCardSeeds extends Seeds {

  lazy val timeOffCardDao = daos.timeOffCardDao

  def load(employees: Seq[UserRecord])(implicit user: UserRecord): Future[Seq[TimeOffCardRecord]] = {

    val timeOffCards = employees.flatMap { employee =>
      (1 to TimeOffCardsPerEmployee).map { idx =>
        val startAt = genZonedDateTime.instance
        val endAt = startAt.plusMinutes(genInt.instance * 5)
        TimeOffCardUpdate(
          id = Some(s"Shifts $idx ${employee.id}".toUUID),
          merchantId = Some(employee.merchantId),
          userId = Some(employee.id),
          paid = Some(genBoolean.instance),
          `type` = Some(genTimeOffType.instance),
          notes = Some(randomWords(5, allCapitalized = false)),
          startAt = Some(startAt),
          endAt = if (genBoolean.instance) Some(endAt) else None,
        )
      }
    }

    timeOffCardDao.bulkUpsert(timeOffCards).extractRecords
  }
}
