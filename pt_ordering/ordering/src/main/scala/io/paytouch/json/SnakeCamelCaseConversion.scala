package io.paytouch.json

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
  *
  * Snake-Camel case conversion is enabled by default unless an implicit [[SnakeCamelCaseConversion.False]] is in scope.
  */
sealed abstract class SnakeCamelCaseConversion

object SnakeCamelCaseConversion {
  object True extends SnakeCamelCaseConversion
  object False extends SnakeCamelCaseConversion
}
