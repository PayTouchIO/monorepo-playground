package io.paytouch.core.services

import com.google.api.client.googleapis.auth.oauth2.{ GoogleIdToken, GoogleIdTokenVerifier }
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.{ withTag, GoogleAuthClientId }

import scala.concurrent._
import scala.util.{ Success, Try }

class GoogleAuthenticationService(
    clientId: String withTag GoogleAuthClientId,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) {

  def verify(idTokenString: String): Future[Either[Throwable, Option[GoogleIdToken.Payload]]] =
    Future {
      import java.util.Collections
      val verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance)
        .setAudience(Collections.singletonList(clientId))
        .build

      Try(verifier.verify(idTokenString))
        .transform(s => Success(Right(Option(s).map(_.getPayload))), f => Success(Left(f)))
        .get
    }
}
