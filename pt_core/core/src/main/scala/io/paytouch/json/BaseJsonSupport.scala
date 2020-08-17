package io.paytouch.json

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }

import scala.reflect.Manifest

trait BaseJsonSupport extends JsonUnmarshaller with JsonMarshaller with JsonSupportInterface

trait JsonSupportInterface {
  type JValue
  def emptyJValue: JValue

  def fromEntityToJValue(a: Any): JValue
  def fromEntityToString[A <: AnyRef](a: A): String
  def fromJsonStringToEntity[A](json: String)(implicit mf: Manifest[A]): A
  def fromJsonStringToJson(json: String): JValue
  def fromJsonToEntity[A](json: JValue)(implicit mf: Manifest[A]): A
  def fromJsonToString(JValue: JValue): String

  def findKeyInJsonString(json: JValue, key: String): JValue

  def camelizeKeys(json: JValue): JValue
  def snakeKeys(json: JValue): JValue
}

trait JsonUnmarshaller { self: JsonSupportInterface =>

  /**
    * HTTP entity => `A`
    *
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def jsonUnmarshaller[A: Manifest]: FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/json`).mapWithCharset {
      (data, charset) =>
        val input =
          if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        fromJsonToEntity[A](unmarshalToCamelCase(input))
    }
  protected def unmarshalToCamelCase(text: String): JValue =
    if (text.isEmpty) emptyJValue else camelizeKeys(fromJsonStringToJson(text))
}

trait JsonMarshaller { self: JsonSupportInterface =>

  implicit val m: ToResponseMarshaller[StatusCode] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { s =>
      val responseEntity: HttpEntity.Strict =
        if (s.allowsEntity) HttpEntity(ContentTypes.`application/json`, "{\"message\":\"" + s.defaultMessage + "\"}")
        else HttpEntity.Empty
      HttpResponse(s, entity = responseEntity)
    }

  /**
    * `A` => HTTP entity
    *
    * @tparam A type to encode, must be upper bounded by `AnyRef`
    * @return marshaller for any `A` value
    */
  implicit def jsonMarshaller[A <: AnyRef](
      implicit
      snakeCamelCaseConversion: SnakeCamelCaseConversion = SnakeCamelCaseConversion.True,
    ): ToEntityMarshaller[A] = {
    val toConditionalSnakeCase: JValue => JValue =
      if (snakeCamelCaseConversion == SnakeCamelCaseConversion.True) snakeKeys else identity
    Marshaller
      .StringMarshaller
      .wrap(MediaTypes.`application/json`)(s => fromJsonToString(toConditionalSnakeCase(fromEntityToJValue(s))))
  }

  def marshalToSnakeCase[A](msg: A) = fromJsonToString(snakeKeys(fromEntityToJValue(msg)))

}
