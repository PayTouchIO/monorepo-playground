package io.paytouch.core.utils

import java.security.MessageDigest

import com.github.t3hnar.bcrypt._
import io.paytouch.core.{ ServiceConfigurations => Config }

trait EncryptionSupport extends BcryptEncryptionSupport with Sha1EncryptionSupport

trait BcryptEncryptionSupport {

  val bcryptRounds: Int

  protected def bcryptEncrypt(text: String) = text.bcrypt(bcryptRounds)

}

trait Sha1EncryptionSupport {

  protected def sha1Encrypt(text: String) = {
    val md = MessageDigest.getInstance("SHA-1")
    md.digest(text.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

}
