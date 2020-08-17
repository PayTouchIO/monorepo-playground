package io.paytouch.ordering.resources.features

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller

import io.paytouch.ordering._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

trait StandardResource extends ParametersResource with JsonResource {
  type Context <: AppContext

  def resourcePath: String
  def paramName: String

  def routes: Route

  protected def contextAuthentication(f: Context => Route): Route

  def getRoute[T <: ExposedEntity](f: Context => UUID => Future[Option[T]]) =
    (path(s"$resourcePath.get") & get) {
      parameters(s"$paramName".as[UUID]) { id =>
        contextAuthentication(context => onSuccess(f(context)(id))(result => completeAsOptApiResponse(result)))
      }
    }

  def listRoute[T](f: Context => Pagination => Future[FindResult[T]]) =
    (path(s"$resourcePath.list") & get) {
      paginateWithDefaults(30) { implicit pagination =>
        contextAuthentication { context =>
          onSuccess(f(context)(pagination))((entities, count) => completeAsPaginatedApiResponse(entities, count))
        }
      }
    }

  def createRoute[C: FromRequestUnmarshaller, T <: ExposedEntity](
      f: Context => (UUID, C) => Future[UpsertionResult[T]],
    ) =
    defaultPostRoute("create", f)

  def updateRoute[U: FromRequestUnmarshaller, T <: ExposedEntity](
      f: Context => (UUID, U) => Future[UpsertionResult[T]],
    ) =
    defaultPostRoute("update", f)

  protected def defaultPostRoute[I: FromRequestUnmarshaller, T <: ExposedEntity](
      verbPath: String,
      f: Context => (UUID, I) => Future[UpsertionResult[T]],
    ) =
    (path(s"$resourcePath.$verbPath") & post) {
      parameters(paramName.as[UUID]) { id =>
        entity(as[I]) { insert =>
          contextAuthentication { context =>
            onSuccess(f(context)(id, insert))(result => completeAsApiResponse(result))
          }
        }
      }
    }

  def updateActiveRoute(f: Context => Seq[UpdateActiveItem] => Future[ValidatedData[Unit]]) =
    path(s"$resourcePath.update_active") {
      post {
        entity(as[Seq[UpdateActiveItem]]) { updates =>
          contextAuthentication { implicit context =>
            onSuccess(f(context)(updates))(result => completeAsEmptyResponse(result))
          }
        }
      }
    }
}
