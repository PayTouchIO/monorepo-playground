package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.enums.ExposedName

final case class Ids(catalogIds: Seq[UUID])

final case class IdsUsage(
    accessible: Ids,
    notUsed: Ids,
    nonAccessible: Ids,
  ) extends ExposedEntity {
  val classShortName = ExposedName.IdsUsage
}

object IdsUsage {

  def apply(catalogs: UsageAnalysis): IdsUsage = {
    val accessible = Ids(catalogIds = catalogs.accessible)
    val notUsed = Ids(catalogIds = catalogs.notUsed)
    val nonAccessible = Ids(catalogIds = catalogs.nonAccessible)
    apply(accessible, notUsed, nonAccessible)
  }
}

final case class UsageAnalysis(
    accessible: Seq[UUID],
    notUsed: Seq[UUID],
    nonAccessible: Seq[UUID],
  )

object UsageAnalysis {

  def extract[Record <: SlickRecord](
      ids: Seq[UUID],
      records: Seq[Record],
    )(
      idsExtractor: Seq[Record] => Seq[UUID],
      matcher: Record => Boolean,
    ): UsageAnalysis = {
    val accessible = idsExtractor(records.filter(matcher))
    val nonAccessible = idsExtractor(records.filterNot(matcher))
    val notUsed = ids diff accessible diff nonAccessible
    new UsageAnalysis(accessible, notUsed, nonAccessible)
  }
}
