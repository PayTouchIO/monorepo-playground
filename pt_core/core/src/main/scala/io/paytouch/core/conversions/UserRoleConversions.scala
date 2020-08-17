package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ UserRoleRecord, UserRoleUpdate => UserRoleUpdateModel }
import io.paytouch.core.entities.{ UserContext, UserRole => UserRoleEntity, UserRoleUpdate => UserRoleUpdateEntity }

trait UserRoleConversions
    extends EntityConversion[UserRoleRecord, UserRoleEntity]
       with ModelConversion[UserRoleUpdateEntity, UserRoleUpdateModel] {

  def fromRecordsAndOptionsToEntities(
      records: Seq[UserRoleRecord],
      usersCountPerUserRole: Option[Map[UserRoleRecord, Int]],
      withPermissions: Boolean,
    ): Seq[UserRoleEntity] =
    records.map { record =>
      val usersCount = usersCountPerUserRole.map(_.getOrElse(record, 0))
      fromRecordAndOptionsToEntity(record, usersCount, withPermissions)
    }

  def fromRecordToEntity(record: UserRoleRecord)(implicit user: UserContext): UserRoleEntity =
    fromRecordAndOptionsToEntity(record, None, withPermissions = true)

  def fromRecordAndOptionsToEntity(
      record: UserRoleRecord,
      usersCount: Option[Int],
      withPermissions: Boolean,
    ): UserRoleEntity =
    UserRoleEntity(
      id = record.id,
      name = record.name,
      hasDashboardAccess = record.hasDashboardAccess,
      hasOnlineStorefrontAccess = record.hasOnlineStorefrontAccess,
      hasRegisterAccess = record.hasRegisterAccess,
      hasTicketsAccess = record.hasTicketsAccess,
      dashboard = if (withPermissions) Some(record.dashboard) else None,
      register = if (withPermissions) Some(record.register) else None,
      usersCount = usersCount,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(id: UUID, update: UserRoleUpdateEntity)(implicit user: UserContext): UserRoleUpdateModel =
    UserRoleUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      name = update.name,
      hasDashboardAccess = update.hasDashboardAccess,
      hasOnlineStorefrontAccess = update.hasOnlineStorefrontAccess,
      hasRegisterAccess = update.hasRegisterAccess,
      hasTicketsAccess = update.hasTicketsAccess,
      dashboard = update.dashboard,
      register = update.register,
    )
}
