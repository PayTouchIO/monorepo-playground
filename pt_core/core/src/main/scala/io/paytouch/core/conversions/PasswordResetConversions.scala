package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.calculations.PasswordResetTokenUtils
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.utils._

trait PasswordResetConversions extends EncryptionSupport with PasswordResetTokenUtils {
  def toResetToken(user: model.UserRecord): model.PasswordResetTokenUpdate = {
    val id = UUID.randomUUID

    model.PasswordResetTokenUpdate(
      id = Some(id),
      userId = Some(user.id),
      key = Some(generateToken(id)),
      expiresAt = Some(UtcTime.now.plusHours(24)),
    )
  }

  def fromRecordToEntity(record: model.PasswordResetTokenRecord): PasswordResetToken =
    PasswordResetToken(
      id = record.id,
      userId = record.userId,
      key = record.key,
      expiresAt = record.expiresAt,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def toUserUpdate(payload: PasswordReset): model.UserUpdate =
    model.UserUpdate(
      id = Some(payload.userId),
      encryptedPassword = Some(bcryptEncrypt(payload.password)),
      merchantId = None,
      userRoleId = None,
      firstName = None,
      lastName = None,
      pin = None,
      email = None,
      dob = None,
      phoneNumber = None,
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
      avatarBgColor = None,
      active = None,
      hourlyRateAmount = None,
      overtimeRateAmount = None,
      paySchedule = None,
      auth0UserId = None,
      isOwner = None,
      deletedAt = None,
    )
}
