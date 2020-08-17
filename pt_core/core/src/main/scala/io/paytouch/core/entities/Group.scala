package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class Group(
    id: UUID,
    name: String,
    customers: Option[Seq[CustomerMerchant]],
    customersCount: Option[Int],
    revenues: Option[Seq[MonetaryAmount]],
    visits: Option[Int],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Group
}

final case class GroupCreation(name: String, customerIds: Seq[UUID]) extends CreationEntity[Group, GroupUpdate] {
  def asUpdate = GroupUpdate(name = Some(name), customerIds = Some(customerIds))
}

final case class GroupUpdate(name: Option[String], customerIds: Option[Seq[UUID]]) extends UpdateEntity[Group]
