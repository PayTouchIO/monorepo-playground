package io.paytouch.core.clients.auth0

import com.auth0.jwk._
import java.security.PublicKey
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import JwtVerificationError.JwkError
import scala.concurrent._

class JwkClient()(implicit ec: ExecutionContext) {
  private var providers: AtomicReference[Map[String, JwkProvider]] =
    new AtomicReference[Map[String, JwkProvider]](Map.empty)

  private val MaxSetAttempts = 10

  def getPublicKey(issuer: String, keyId: String): Future[Either[JwtVerificationError, PublicKey]] =
    Future {
      val provider = providers.get.getOrElse(issuer, buildProvider(issuer))
      try Right(provider.get(keyId).getPublicKey)
      catch {
        case e: Exception => Left(JwkError(e))
      }
    }

  protected def buildProvider(issuer: String): JwkProvider = {
    val provider = new JwkProviderBuilder(issuer)
      .cached(10, 24, TimeUnit.HOURS) // Cache 10 keys for a maximum of 24 hours each
      .rateLimited(10, 1, TimeUnit.MINUTES) // Make a maximum of 10 requests per 1 minute
      .build()

    var updated = false
    var attempts = 0
    while (!updated) {
      val oldProviders = providers.get()
      val updatedProviders = oldProviders + (issuer -> provider)

      if (providers.compareAndSet(oldProviders, updatedProviders))
        updated = true
      else {
        attempts += 1

        if (attempts > MaxSetAttempts)
          throw new RuntimeException("JwkProvider couldn't set update providers in a timely fashion")
      }
    }

    provider
  }
}
