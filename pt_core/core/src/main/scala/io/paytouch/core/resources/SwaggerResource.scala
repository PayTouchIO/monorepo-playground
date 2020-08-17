package io.paytouch.core.resources

import java.util.{ ArrayList => JavaArray }

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.server.Route

import org.yaml.snakeyaml.Yaml

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.json.JsonSupport._
import io.paytouch.json.SnakeCamelCaseConversion

trait SwaggerResource extends JsonResource with SwaggerUtils {
  implicit val disabledSnakeCamelConversion = SnakeCamelCaseConversion.False

  val swaggerRoutes: Route = path("swagger") {
    parameter("secret" ! "4f188d6538e4362ffe5473be77ceba3e") {
      get { ctx =>
        val swaggerJson = readSwaggerAsJson(ctx.request.uri.authority, "swagger.yml")
        ctx.complete(swaggerJson)
      }
    }
  }

}

trait SwaggerUtils {
  private type JavaMap = java.util.Map[String, AnyRef]

  protected def readSwaggerAsJson(authority: Authority, path: String): JValue = {
    val file = getClass.getClassLoader.getResource(path).openStream()
    val yaml = new Yaml()
    val swaggerMap = convertToScalaMap(yaml.load(file).asInstanceOf[JavaMap])
    JsonSupport.fromEntityToJValue(swaggerMap).transformField {
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
