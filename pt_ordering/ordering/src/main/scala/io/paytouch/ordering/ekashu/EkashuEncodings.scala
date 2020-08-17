package io.paytouch.ordering.ekashu

import java.security.MessageDigest
import java.util.Base64

import io.paytouch.ordering.data.model.EkashuConfig

trait EkashuEncodings {

  protected def calculateEkashuHashCode(
      reference: String,
      amount: String,
    )(implicit
      ekashuConfig: EkashuConfig,
    ): String =
    // FROM DOCS: hash_key + ekashu_seller_id + ekashu_reference + ekashu_amount
    encodeSHA1(ekashuConfig.hashKey, ekashuConfig.sellerId, reference, amount)

  protected def calculateEkashuHashSuccessResult(transactionId: String)(implicit ekashuConfig: EkashuConfig): String =
    // FROM DOCS: hash_key + ekashu_seller_id + ekashu_transaction_id + 0
    encodeSHA1(ekashuConfig.hashKey, ekashuConfig.sellerId, transactionId, "0")

  protected def calculateEkashuHashFailureResult(transactionId: String)(implicit ekashuConfig: EkashuConfig): String =
    // FROM DOCS: hash_key + ekashu_seller_id + ekashu_transaction_id + 1
    encodeSHA1(ekashuConfig.hashKey, ekashuConfig.sellerId, transactionId, "1")

  private def encodeSHA1(text: String*): String = {
    val md = MessageDigest.getInstance("SHA-1")
    Base64.getEncoder.encodeToString(md.digest(text.mkString("").getBytes("UTF-8")))
  }

}
