package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.{ CustomerSource, CustomerSourceAlias }

final case class CustomerFilters(
    locationId: Option[UUID] = None,
    groupId: Option[UUID] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
    loyaltyProgramId: Option[UUID] = None,
    source: Option[Seq[CustomerSource]] = None,
  ) extends BaseFilters

object CustomerFilters {
  def forList(
      locationId: Option[UUID] = None,
      groupId: Option[UUID] = None,
      query: Option[String] = None,
      source: Option[Seq[CustomerSourceAlias]] = None,
      updatedSince: Option[ZonedDateTime] = None,
    ): CustomerFilters =
    CustomerFilters(
      locationId = locationId,
      groupId = groupId,
      query = query,
      source = source.orElse(Some(Seq(CustomerSourceAlias.Visible))).map(_.map(_.targets).reduce(_ ++ _).toSeq),
      updatedSince = updatedSince,
    )

  def forGet(loyaltyProgramId: Option[UUID], updatedSince: Option[ZonedDateTime]) =
    CustomerFilters(
      loyaltyProgramId = loyaltyProgramId,
      updatedSince = updatedSince,
    )
}
