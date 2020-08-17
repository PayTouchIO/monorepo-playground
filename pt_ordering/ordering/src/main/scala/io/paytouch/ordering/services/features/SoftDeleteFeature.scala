package io.paytouch.ordering.services.features

import io.paytouch.ordering.data.daos.features.SlickSoftDeleteDao

trait SoftDeleteFeature extends DeleteFeature {

  type Dao <: SlickSoftDeleteDao

}
