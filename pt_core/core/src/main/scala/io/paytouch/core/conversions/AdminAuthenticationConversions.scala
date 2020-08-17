package io.paytouch.core.conversions

import java.util.UUID

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import io.paytouch.core.data.model.{ AdminUpdate => AdminUpdateRecord }

trait AdminAuthenticationConversions {

  def fromGooglePayloadToUpdate(payload: GoogleIdToken.Payload): AdminUpdateRecord =
    AdminUpdateRecord(
      id = None,
      firstName = Some(payload.get("given_name").toString),
      lastName = Some(payload.get("family_name").toString),
      email = Some(payload.getEmail),
      password = Some(UUID.randomUUID.toString),
      lastLoginAt = None,
    )
}
