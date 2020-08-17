package io.paytouch.core.messages.entities

import java.util.UUID
import java.time.ZonedDateTime

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class PasswordResetRequested(eventName: String, payload: PasswordResetPayload)
    extends PtNotifierMsg[PasswordResetData]

final case class PasswordResetPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: PasswordResetData,
    recipientEmail: String,
  ) extends EmailEntityPayloadLike[PasswordResetData]

final case class PasswordResetData(
    token: PasswordResetToken,
    userInfo: UserInfo,
    merchant: Merchant,
  )

object PasswordResetRequested {

  val eventName = "password_reset_requested"

  def apply(
      merchantId: UUID,
      token: PasswordResetToken,
      userInfo: UserInfo,
      merchant: Merchant,
    ): PasswordResetRequested = {
    val data = PasswordResetData(
      token,
      userInfo,
      merchant,
    )

    val payload = PasswordResetPayload(
      token.classShortName,
      merchantId,
      data,
      userInfo.email,
    )

    PasswordResetRequested(eventName, payload)
  }
}

object WelcomePasswordResetRequested {

  val eventName = "welcome_password_reset_requested"

  def apply(
      merchantId: UUID,
      token: PasswordResetToken,
      userInfo: UserInfo,
      merchant: Merchant,
    ): PasswordResetRequested = {
    val data = PasswordResetData(
      token,
      userInfo,
      merchant,
    )

    val payload = PasswordResetPayload(
      token.classShortName,
      merchantId,
      data,
      userInfo.email,
    )

    PasswordResetRequested(eventName, payload)
  }
}
