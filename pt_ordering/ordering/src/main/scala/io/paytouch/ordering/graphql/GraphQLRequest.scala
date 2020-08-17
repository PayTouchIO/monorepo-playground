package io.paytouch.ordering.graphql

import io.paytouch.ordering.json.JsonSupport.JValue

final case class GraphQLRequest(
    query: String,
    variables: Option[JValue] = None,
    operationName: Option[String] = None,
  )
