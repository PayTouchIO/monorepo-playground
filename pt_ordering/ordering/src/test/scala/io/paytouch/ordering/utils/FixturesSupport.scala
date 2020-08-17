package io.paytouch.ordering.utils

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

import io.paytouch.ordering.json.JsonSupport

trait FixturesSupport extends JsonSupport {
  def loadResource(resourcePath: String): String =
    Source.fromURL(getClass.getResource(resourcePath)).mkString

  def loadJsonAst(resourcePath: String): JValue =
    parse(loadResource(resourcePath))

  def loadJsonAs[T: Manifest](resourcePath: String): T =
    readJsonAs[T](loadResource(resourcePath))

  def readJsonAs[T: Manifest](json: String): T =
    Serialization.read[T](json)
}
