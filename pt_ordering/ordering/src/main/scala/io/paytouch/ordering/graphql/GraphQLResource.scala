package io.paytouch.ordering.graphql

import scala.concurrent._
import scala.util._
import scala.util.control.NonFatal

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, Route }

import com.typesafe.scalalogging.LazyLogging

import org.json4s.JsonAST.JObject

import sangria.ast.Document
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.marshalling.json4s.native._
import sangria.parser.{ QueryParser, SyntaxError }
import sangria.schema.{ Context, EnumType }

import io.paytouch.json.SnakeCamelCaseConversion
import io.paytouch.ordering.graphql.schema._
import io.paytouch.ordering.json.JsonSupport

class GraphQLResource(val graphQLService: GraphQLService)(implicit ec: ExecutionContext)
    extends Directives
       with JsonSupport {
  implicit private val sccc: SnakeCamelCaseConversion =
    SnakeCamelCaseConversion.False

  lazy val routes: Route =
    pathPrefix("graphql") {
      post {
        entity(as[GraphQLRequest]) { graphQLRequest =>
          QueryParser.parse(graphQLRequest.query) match {
            case Success(queryAst) => complete(execute(graphQLRequest, queryAst))
            case Failure(error)    => complete(StatusCodes.BadRequest, formatError(error))
          }
        }
      } ~ pathEnd {
        get {
          getFromResource("graphql/graphiql.html")
        }
      }
    }

  private def execute(graphQLRequest: GraphQLRequest, queryAst: Document) =
    Executor
      .execute(
        schema = GraphQLSchema.instance,
        queryAst = queryAst,
        userContext = GraphQLContext(graphQLService),
        operationName = graphQLRequest.operationName,
        variables = graphQLRequest.variables.getOrElse(JObject()),
        deferredResolver = Fetchers.all,
        deprecationTracker = deprecationTracker,
        exceptionHandler = ExceptionHandler(onException = {
          // this forces the exception to be exposed to the end user, helping with debugging
          case (_, e: Throwable) => throw e
        }),
      )
      .map(StatusCodes.OK -> _)
      .recover {
        case error: QueryAnalysisError => StatusCodes.BadRequest -> error.resolveError
        case error: ErrorWithResolver  => StatusCodes.InternalServerError -> error.resolveError
      }

  def formatError(error: Throwable): JValue =
    error match {
      case syntaxError: SyntaxError =>
        val error = Map(
          "errors" -> Seq(
            Map(
              "message" -> syntaxError.getMessage,
              "locations" -> Seq(
                Map(
                  "line" -> syntaxError.originalError.position.line,
                  "column" -> syntaxError.originalError.position.column,
                ),
              ),
            ),
          ),
        )
        fromEntityToJValue(error)
      case NonFatal(e) =>
        formatError(e.getMessage)
      case e =>
        throw e
    }

  def formatError(message: String): JValue = {
    val error = Map(
      "errors" -> Seq(
        Map(
          "message" -> message,
        ),
      ),
    )
    fromEntityToJValue(error)
  }

  private val deprecationTracker = new DeprecationTracker with LazyLogging {

    def deprecatedFieldUsed[Ctx](ctx: Context[Ctx, _]) = {
      val reason = ctx.field.deprecationReason.map(r => s": $r").getOrElse("")
      logger.warn(s"Deprecated field '${ctx.parentType.name}.${ctx.field.name}' used at path '${ctx.path}'$reason.")
    }

    def deprecatedEnumValueUsed[T, Ctx](
        enum: EnumType[T],
        value: T,
        userContext: Ctx,
      ) =
      logger.warn(s"Deprecated enum value '$value' used of enum '${enum.name}'.")
  }

}
