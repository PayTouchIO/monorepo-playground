package io.paytouch.core.utils

import io.paytouch.core.json.JsonSupport

import scala.io.Source

trait FixturesSupport extends JsonSupport {
  def loadJson(resourcePath: String) = Source.fromURL(getClass.getResource(resourcePath)).mkString
  def loadJsonAs[T: Manifest](resourcePath: String): T = readJsonAs[T](loadJson(resourcePath))

  def readJsonAs[T: Manifest](json: String): T = fromJsonStringToEntity[T](json)
}
