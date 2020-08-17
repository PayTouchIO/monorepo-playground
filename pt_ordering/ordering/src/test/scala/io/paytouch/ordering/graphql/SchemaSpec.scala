package io.paytouch.ordering.graphql

import org.json4s.JsonAST.{ JField, JObject }

import sangria.ast.Document
import sangria.execution.deferred.DeferredResolver
import sangria.execution.Executor
import sangria.marshalling.json4s.native._

import io.paytouch.implicits._

import io.paytouch.ordering.graphql.schema.{ Fetchers, GraphQLSchema }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.utils.{ DaoSpec, MockedRestApi, StringHelper }

trait SchemaSpec extends DaoSpec with JsonSupport {
  abstract class SchemaSpecContext extends DaoSpecContext with StringHelper {
    lazy val ptCoreClient = MockedRestApi.ptCoreClient

    def executeQuery(query: Document, vars: JObject = JObject()) = {
      val context = GraphQLContext(MockedRestApi.graphQLService)
      val result = Executor.execute(
        schema = GraphQLSchema.instance,
        queryAst = query,
        variables = vars,
        userContext = context,
        deferredResolver = Fetchers.all,
      )
      result.await
    }

    def addRemove(
        json: JValue,
        fieldsToRemove: Seq[String] = Seq.empty,
        fieldsToInclude: Seq[String] = Seq.empty,
      ) = includeFields(removeFields(json, fieldsToRemove), fieldsToInclude)

    def parseAsEntity[T <: AnyRef](command: String, entity: T) = {
      val wrapper = { s: String => s""" { "$command": $s } """ }
      parseAsEntityWithWrapper(wrapper, entity)
    }

    def parseAsEntityWithWrapper[T <: AnyRef](
        wrapper: String => String,
        entity: T,
        fieldsToRemove: Seq[String] = Seq.empty,
        fieldsToInclude: Seq[String] = Seq.empty,
      ) = {
      val json = addRemove(fromEntityToJValue(entity), fieldsToRemove, fieldsToInclude)
      val jsonAsString = s"""{ "data": ${wrapper(fromJsonToString(json))} } """
      parseAsSnakeCase(jsonAsString)
    }

    def parseAsSnakeCase(jsonAsString: String) =
      snakeKeys(fromJsonStringToJson(jsonAsString))

    private def removeFields(json: JValue, fieldNames: Seq[String]) =
      if (fieldNames.isEmpty) json
      else
        json.removeField {
          case JField(f, _) if fieldNames.contains(f.underscore) => true
          case _                                                 => false
        }

    private def includeFields(json: JValue, fieldNames: Seq[String]) =
      if (fieldNames.isEmpty) json
      else
        json.removeField {
          case JField(f, _) if fieldNames.contains(f.underscore) => false
          case _                                                 => true
        }
  }
}
