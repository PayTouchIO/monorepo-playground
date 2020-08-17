package io.paytouch.core.stubs

import java.util.UUID

import io.paytouch.core.clients.paytouch.ordering.entities.{ IdsToCheck, IdsUsage }
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.entities.UserContext

object PtOrderingStubData {

  private var catalogIdsMap: Map[MerchantRecord, Seq[UUID]] = Map.empty

  def recordCatalogIds(catalogIds: Seq[UUID], merchant: MerchantRecord) =
    synchronized {
      catalogIdsMap += merchant -> catalogIds
    }

  def inferIdsUsage(ids: IdsToCheck)(implicit user: UserContext): IdsUsage = {
    val (catalogIdsAcc, catalogIdsNotUsed, catalogIdsNonAcc) = inferCatalogIdsUsage(ids.catalogIds)
    IdsUsage(
      accessible = IdsToCheck(catalogIdsAcc),
      notUsed = IdsToCheck(catalogIdsNotUsed),
      nonAccessible = IdsToCheck(catalogIdsNonAcc),
    )
  }

  private def inferCatalogIdsUsage(
      catalogIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): (Seq[UUID], Seq[UUID], Seq[UUID]) =
    inferUsage(catalogIds)(catalogIdsMap, user)

  private def inferUsage(
      ids: Seq[UUID],
    )(
      merchantMap: Map[MerchantRecord, Seq[UUID]],
      user: UserContext,
    ): (Seq[UUID], Seq[UUID], Seq[UUID]) = {
    val accessible = merchantMap.view.filterKeys(_.id == user.merchantId).values.flatten.toSeq intersect ids
    val nonAccessible = merchantMap.view.filterKeys(_.id != user.merchantId).values.flatten.toSeq intersect ids
    val notUsed = ids diff accessible diff nonAccessible
    (accessible, nonAccessible, notUsed)
  }

}
