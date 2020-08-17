package io.paytouch.core.swagger

import scala.util.chaining._

import cats.implicits._

import io.circe.parser._
import io.circe.schema.Schema

import io.swagger.util.Json
import io.swagger.parser._

import io.paytouch.core.utils.PaytouchSpec

class SwaggerSpec extends PaytouchSpec {
  "Swagger Documentation" should {
    "be well formed" in {
      val schema =
        Either.catchNonFatal {
          "https://schema.swagger.io/v2/schema.json"
            .pipe(scala.io.Source.fromURL)
            .mkString
            .pipe(Schema.loadFromString)
            .toEither
        }.joinRight

      val api =
        (new SwaggerParser)
          .read("swagger.yml")
          .pipe(Json.pretty)
          .pipe(parse)

      (schema.toEitherNel, api.toEitherNel)
        .parMapN(_ validate _)
        .map(_.toEither)
        .joinRight
        .leftMap(_.map(_.getMessage).toList.mkString("\n"))
        .fold(sys.error, _ => success)
    }
  }
}
