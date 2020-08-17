package io.paytouch.ordering.graphql.datatypes

import sangria.schema._

trait MapDataType {
  def deriveMapObjectType[Ctx, Key, Value](
      keyField: String,
      valueField: String,
      name: String,
    )(implicit
      keyOutputType: OutputType[Key],
      valueOutputType: OutputType[Value],
    ): ObjectType[Ctx, (Key, Value)] =
    ObjectType(
      name,
      s"A map with key $keyField and value $valueField",
      fields[Ctx, (Key, Value)](
        Field(keyField, keyOutputType, resolve = _.value._1),
        Field(valueField, valueOutputType, resolve = _.value._2),
      ),
    )
}
