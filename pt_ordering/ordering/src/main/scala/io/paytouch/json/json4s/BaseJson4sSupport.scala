package io.paytouch.json.json4s

// modified from https://github.com/hseeberger/akka-http-json/tree/master/akka-http-json4s

import scala.reflect.Manifest

import org.json4s._
import org.json4s.native.JsonMethods.{ parse, pretty, render }
import org.json4s.native.Serialization

import io.paytouch.json.JsonSupportInterface

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
  */
trait BaseJson4sSupport extends JsonSupportInterface {
  type JValue = org.json4s.JValue
  val emptyJValue = JNothing
  val JString = org.json4s.JsonAST.JString
  val JField = org.json4s.JsonAST.JField

  def json4sFormats: Formats

  def fromEntityToJValue(a: Any): JValue = Extraction.decompose(a)(json4sFormats)
  def fromEntityToString[A <: AnyRef](a: A): String = Serialization.write(a)(json4sFormats)
  def fromJsonStringToEntity[A](json: String)(implicit mf: Manifest[A]): A =
    Serialization.read[A](json)(json4sFormats, mf)
  def fromJsonStringToJson(json: String): JValue = parse(json)
  def fromJsonToEntity[A](json: JValue)(implicit mf: Manifest[A]): A = json.extract[A](json4sFormats, mf)
  def fromJsonToString(json: JValue) = pretty(render(json)(json4sFormats))

  def findKeyInJsonString(json: JValue, key: String): JValue = json \ key

  def camelizeKeys(json: JValue): JValue = json.camelizeKeys
  def snakeKeys(json: JValue): JValue = json.snakeKeys

  implicit class RichJValue(json: JValue) {
    // Note that json4s will not transform fields with keys starting with an '_'
    def snakeKeys: JValue = addInitialUnderscoreToDashes.snakizeKeys.removeInitialUnderscoreToDashes

    def addInitialUnderscoreToDashes: JValue =
      json.transformField {
        case JField(nm, x) if nm.contains("-") => JField(s"_$nm", x)
      }

    def removeInitialUnderscoreToDashes: JValue =
      json.transformField {
        case JField(nm, x) if nm.contains("-") && nm.startsWith("_") => JField(nm.substring(1), x)
      }
  }
}
