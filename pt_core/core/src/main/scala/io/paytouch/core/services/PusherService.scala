package io.paytouch.core.services

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Hex

import io.paytouch.core._
import io.paytouch.core.entities._
import io.paytouch.core.errors.{ InvalidPusherChannel, InvalidPusherSocketId }
import io.paytouch.core.utils.RegexUtils._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

class PusherService(key: String withTag PusherKey, secret: String withTag PusherSecret) {
  val channelWithMerchantIdRe = s"""\\Aprivate-($singleUuidRe)-$channelNameRe\\Z""".r
  val channelWithMerchantIdAndLocationIdRe = s"""private-($singleUuidRe)-($singleUuidRe)-$channelNameRe\\Z""".r

  def authenticate(
      pusherAuthentication: PusherAuthentication,
    )(implicit
      user: UserContext,
    ): ErrorsOr[PusherToken] = {
    val channel = validateChannel(pusherAuthentication.channelName)
    val socketId = validateSocketId(pusherAuthentication.socketId)

    Multiple.combine(channel, socketId) { case (chan, sId) => generateToken(chan, sId) }
  }

  private def sign(stringToSign: String): String = {
    val sha256_HMAC = Mac.getInstance("HmacSHA256")
    val secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256")

    sha256_HMAC.init(secretKey)

    Hex.encodeHexString(sha256_HMAC.doFinal(stringToSign.getBytes("UTF-8")))
  }

  def validateChannel(channel: String)(implicit user: UserContext): ErrorsOr[String] =
    channel match {
      case c if c.length > 200 => Multiple.failure(InvalidPusherChannel(channel, s"Channel too long"))
      case channelWithMerchantIdRe(merchantId) if merchantId == user.merchantId.toString =>
        Multiple.success(channel)
      case channelWithMerchantIdAndLocationIdRe(merchantId, locationId)
          if merchantId == user.merchantId.toString && user.locationIds.exists(lId => lId.toString == locationId) =>
        Multiple.success(channel)
      case _ => Multiple.failure(InvalidPusherChannel(channel, s"Invalid merchant id and/or location id"))
    }

  def validateSocketId(socketId: String): ErrorsOr[String] =
    if (socketIdRe.findFirstIn(socketId).isEmpty)
      Multiple.failure(InvalidPusherSocketId(socketId))
    else
      Multiple.success(socketId)

  private def generateToken(channelName: String, socketId: String) = {
    val stringToSign = s"$socketId:$channelName"
    val signature = sign(stringToSign)
    PusherToken(s"$key:$signature")
  }
}
