package io.paytouch.core.stubs

import cats.implicits._
import java.security._
import io.paytouch.core.clients.auth0._
import pdi.jwt._
import scala.concurrent._

class JwkStubClient()(implicit ec: ExecutionContext) extends JwkClient {
  private var rsaKey: KeyPair = {
    val generatorRSA = KeyPairGenerator.getInstance(JwtUtils.RSA)
    generatorRSA.initialize(1024)
    generatorRSA.generateKeyPair
  }

  def recordKeyPair(keyPair: KeyPair) =
    synchronized {
      rsaKey = keyPair
    }

  def getPrivateKey: PrivateKey = rsaKey.getPrivate

  override def getPublicKey(issuer: String, kid: String) =
    Future.successful(rsaKey.getPublic.asRight)
}
