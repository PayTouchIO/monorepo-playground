package io.paytouch.ordering.graphql.schema

import cats.implicits._

import sangria.schema._

import io.paytouch.ordering.graphql.GraphQLContext

object GraphQLSchema {
  val instance: Schema[GraphQLContext, Unit] =
    Schema(
      ObjectType(
        name = "Query",
        fields = List(
          CatalogSchema,
          CategorySchema,
          LocationSchema,
          MerchantByIdSchema,
          MerchantSchema,
          StoreSchema,
        ).map(_.fields).combineAll,
      ),
    )
}
