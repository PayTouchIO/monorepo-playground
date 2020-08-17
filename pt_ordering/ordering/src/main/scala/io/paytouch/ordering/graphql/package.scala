package io.paytouch.ordering

import sangria.macros.derive._
import sangria.schema._

import io.paytouch._

package object graphql {
  implicit def OpaqueStringType[O <: Opaque[String]](f: String => O): ScalarAlias[O, String] =
    ScalarAlias[O, String](
      StringType,
      _.value,
      value => Right(f(value)),
    )
}
