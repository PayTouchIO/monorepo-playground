package io.paytouch.ordering.resources

import java.util.{ ArrayList => JavaArray }

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.server.{ Directives, Route }

import scala.jdk.CollectionConverters._
import scala.collection.mutable

import org.json4s.JsonAST.{ JField, JString, JValue }
import org.json4s.{ Extraction, Formats }

import org.yaml.snakeyaml.Yaml

import io.paytouch.json.SnakeCamelCaseConversion
import io.paytouch.ordering.json.JsonSupport

class SwaggerResource extends JsonSupport with Directives with SwaggerUtils {
  implicit val disabledSnakeCamelConversion = SnakeCamelCaseConversion.False

  val routes: Route = path("swagger") {
    parameter("secret" ! "dYLpW9vaMBA7ETyAagPnhJv3NU8CIxs2") {
      get { ctx =>
        val swaggerJson = readSwaggerAsJson(ctx.request.uri.authority, "swagger.yml")
        ctx.complete(swaggerJson)
      }
    }
  }
}

trait SwaggerUtils {
  private type JavaMap = java.util.Map[String, AnyRef]

  protected def readSwaggerAsJson(authority: Authority, path: String)(implicit formats: Formats): JValue = {
    val file = getClass.getClassLoader.getResource(path).openStream()
    val yaml = new Yaml()
    val swaggerMap = convertToScalaMap(yaml.load(file).asInstanceOf[JavaMap])

    Extraction
      .decompose(swaggerMap)
      .transformField {
        case JField("host", _) =>
          ("host", JString(Uri(authority = authority).toString.replace("//", "")))
      }
  }

  private def convertToScalaMap(m: JavaMap): mutable.Map[String, AnyRef] = {
    def convert(pair: (String, AnyRef)): (String, AnyRef) =
      pair match {
        case (key, value: JavaMap @unchecked) => (key, convertToScalaMap(value))
        case (key, values: JavaArray[_]) =>
          (
            key,
            values.asScala.map {
              case value: JavaMap @unchecked => convertToScalaMap(value)
              case value                     => value
            },
          )
        case _ => pair
      }

    m.asScala.map { case (key, value) => convert((key, value)) }
  }
}
