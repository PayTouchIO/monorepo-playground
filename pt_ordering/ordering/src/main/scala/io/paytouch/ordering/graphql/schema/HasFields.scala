package io.paytouch.ordering.graphql.schema

import sangria.schema.Field

import io.paytouch.ordering.graphql.GraphQLContext

trait HasFields {
  def fields: List[Field[GraphQLContext, Unit]]
}
