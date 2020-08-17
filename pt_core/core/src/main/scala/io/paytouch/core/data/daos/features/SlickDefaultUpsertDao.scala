package io.paytouch.core.data.daos.features

trait SlickDefaultUpsertDao extends SlickUpsertDao {
  type Upsertion = Update
}
