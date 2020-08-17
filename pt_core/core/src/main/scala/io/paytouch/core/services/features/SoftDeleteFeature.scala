package io.paytouch.core.services.features

import io.paytouch.core.data.daos.features.SlickSoftDeleteDao

trait SoftDeleteFeature extends DeleteFeature {
  override type Dao <: SlickSoftDeleteDao
}
