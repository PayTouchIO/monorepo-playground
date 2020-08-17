package io.paytouch.core.utils

import scala.io.Source

import org.json4s.JsonAST.JArray

import io.paytouch.core.json.JsonSupport

trait JsonTemplateUtils extends JsonSupport {
  protected def loadAs[T: Manifest](path: String, transformations: Map[String, String] = Map.empty): T = {
    val json = loadAsJson(path, camelCaseConversion = true)
    val modifiedJson = modifyJson(json, transformations)
    fromJsonToEntity(modifiedJson)
  }

  protected def loadAsJson(path: String, camelCaseConversion: Boolean = false): JValue = {
    val contents = Source.fromURL(getClass.getResource(path)).mkString
    if (camelCaseConversion) camelizeKeys(fromJsonStringToJson(contents)) else fromJsonStringToJson(contents)
  }

  private def modifyJson(json: JValue, transformations: Map[String, String]) = {
    val jsonTransformations: Map[JValue, JValue] = transformations.map { case (k, v) => JString(k) -> JString(v) }
    json.transformField {
      case (f, v) if transformations.get(f).isDefined     => (transformations(f), v)
      case (f, v) if jsonTransformations.get(v).isDefined => (f, jsonTransformations(v))
      case (f, JArray(vs)) if vs.exists(v => jsonTransformations.get(v).isDefined) =>
        val updatedVs = vs.map(v => if (jsonTransformations.get(v).isDefined) jsonTransformations(v) else v)
        (f, JArray(updatedVs))
    }
  }
}
