package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class BundleSetUpsertion(bundleSetUpdate: BundleSetUpdate, bundleOptionUpdates: Seq[BundleOptionUpdate])
    extends UpsertionModel[BundleSetRecord]
