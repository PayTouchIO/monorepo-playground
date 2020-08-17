package io.paytouch.ordering.resources.graphql

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.ordering.graphql.GraphQLRequest
import io.paytouch.ordering.utils.FSpec

class GraphQLFSpec extends FSpec {

  "GET /graphql" in {

    "should return the GraphiQL interface" in new FSpecContext {
      Get("/graphql") ~> routes ~> check {
        assertStatusOK()
      }
    }
  }

  "POST /graphql" in {

    "should execute a simple GraphQL query" in new FSpecContext {
      val q = """{ __type(name: "String!") { name } }"""
      val graphQLQuery = GraphQLRequest(query = q)

      Post("/graphql", graphQLQuery) ~> routes ~> check {
        assertStatusOK()
      }
    }
  }
}
