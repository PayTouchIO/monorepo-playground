package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class MerchantUpsertion(
    merchant: MerchantUpdate,
    userRoles: Seq[UserRoleUpdate],
    ownerUser: UserUpdate,
  )
