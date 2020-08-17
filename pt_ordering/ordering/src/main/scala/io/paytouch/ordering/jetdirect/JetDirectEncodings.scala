package io.paytouch.ordering.jetdirect

import java.security.MessageDigest
import java.util.Base64

import io.paytouch.ordering.data.model.JetdirectConfig

trait JetdirectEncodings {

  protected def calculateJetdirectHashCode(
      orderNumber: String,
      amount: String,
    )(implicit
      jetDirectConfig: JetdirectConfig,
    ): String =
    // FROM DOCS: TerminalID+TransactionAmount+JetdirectSecurityToken+UniqueOrderNumber
    encodeSHA512(jetDirectConfig.terminalId, amount, jetDirectConfig.securityToken, orderNumber)

  def calculateJetdirectReturnHashCode(
      orderNumber: String,
      amount: String,
      responseText: String,
    )(implicit
      jetDirectConfig: JetdirectConfig,
    ): String =
    // FROM DOCS: TerminalID+TransactionAmount+JetdirectSecurityToken+UniqueOrderNumber+responseText
    encodeSHA512(jetDirectConfig.terminalId, amount, jetDirectConfig.securityToken, orderNumber, responseText)

  private def encodeSHA512(text: String*): String = {
    val md = MessageDigest.getInstance("SHA-512")
    Hex.valueOf(md.digest(text.mkString("").getBytes("UTF-8")))
  }

  object Hex {
    def valueOf(buf: Array[Byte]): String = buf.map("%02x" format _).mkString
  }
}
