package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model.{ CustomerGroupUpdate, GroupRecord, GroupUpdate }

final case class GroupUpsertion(groupUpdate: GroupUpdate, customerGroupUpdates: Option[Seq[CustomerGroupUpdate]])
    extends UpsertionModel[GroupRecord]
