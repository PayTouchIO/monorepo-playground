package io.paytouch.seeds

import io.paytouch.core.data.model.{ GroupRecord, GroupUpdate, UserRecord }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object GroupSeeds extends Seeds {

  lazy val groupDao = daos.groupDao

  def load(implicit user: UserRecord): Future[Seq[GroupRecord]] = {
    val groupIds = groupIdsPerEmail(user.email)

    val groups = groupIds.map { groupId =>
      GroupUpdate(id = Some(groupId), merchantId = Some(user.merchantId), name = Some(randomWord))
    }

    groupDao.bulkUpsert(groups).extractRecords
  }
}
