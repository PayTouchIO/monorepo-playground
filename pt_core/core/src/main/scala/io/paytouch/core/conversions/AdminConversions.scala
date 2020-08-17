package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ AdminRecord, AdminUpdate => AdminUpdateModel }
import io.paytouch.core.entities.{ Admin => AdminEntity, AdminUpdate => AdminUpdateEntity }
import io.paytouch.core.utils.EncryptionSupport

trait AdminConversions extends EntityConversionNoUserContext[AdminRecord, AdminEntity] with EncryptionSupport {

  def fromRecordToEntity(record: AdminRecord): AdminEntity =
    AdminEntity(
      id = record.id,
      firstName = record.firstName,
      lastName = record.lastName,
      email = record.email,
      lastLoginAt = record.lastLoginAt,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(id: UUID, upsertion: AdminUpdateEntity): AdminUpdateModel =
    AdminUpdateModel(
      id = Some(id),
      firstName = upsertion.firstName,
      lastName = upsertion.lastName,
      password = upsertion.password.map(bcryptEncrypt),
      email = upsertion.email,
      lastLoginAt = None,
    )
}
