package io.paytouch.core.services

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.RequestContext
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.{ withTag, HmacSecret, PtCoreUrl }

class HmacService(val hmacSecret: String withTag HmacSecret, val ptCoreUrl: String withTag PtCoreUrl)
    extends LazyLogging {

  def generateUri(basePath: String, params: Map[String, String]): Uri = {
    val token = generateToken(basePath, params)
    val path = generatePath(basePath, params ++ Map("token" -> token))
    Uri(s"$ptCoreUrl$path")
  }

  def verifyUrl(requestContext: RequestContext): Boolean = {
    val uri = requestContext.request.uri
    val query = uri.query()
    val incomingToken = query.getOrElse("token", "")

    val queryParams = query.toMap - "token"

    val expectedToken = generateToken(uri.path.toString, queryParams)
    logger.debug(
      s"[HMAC] uri={}, query={}, queryParams={}, expectedToken={}, incomingToken={}, hmacSecret={}",
      uri,
      query,
      queryParams,
      expectedToken,
      incomingToken,
      hmacSecret,
    )

    incomingToken == expectedToken
  }

  private def generatePath(basePath: String, params: Map[String, String]) = {
    val queryPart =
      params.toSeq.sortBy { case (key, _) => key }.map { case (key, value) => s"$key=$value" }.mkString("&")
    s"$basePath?$queryPart"
  }

  private def generateToken(basePath: String, params: Map[String, String]): String = {
    val path = generatePath(basePath, params)
    hmac(path)
  }

  private def hmac(string: String): String = {
    val secret = new SecretKeySpec(hmacSecret.getBytes, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secret)
    val result: Array[Byte] = mac.doFinal(string.getBytes)
    result.map("%02x" format _).mkString
  }
}
