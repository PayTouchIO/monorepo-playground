package io.paytouch.core.resources

import scala.collection.immutable

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

import cats.data.Validated.{ Invalid, Valid }

import io.paytouch.core.data.extensions.DynamicSortBySupport.Sortings
import io.paytouch.core.entities.{ ApiResponse, _ }
import io.paytouch.core.errors._
import io.paytouch.core.expansions.BaseExpansions
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.json.serializers.CustomUnmarshallers
import io.paytouch.core.utils._
import io.paytouch.core.utils.FindResult.FindResult
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.ResultType
import io.paytouch.core.ValidationHeader
import io.paytouch.utils._

trait JsonResource
    extends Authentication
       with JsonSupport
       with CorsSupport
       with CustomUnmarshallers
       with Locking
       with StrictEntitiesDirectories {
  def completeAsOptApiResponse[T <: ExposedEntity](opt: Option[T]): Route =
    opt match {
      case Some(t) => complete(ApiResponse(t))
      case None    => complete(StatusCodes.NotFound)
    }

  def completeAsApiResponse[T <: ExposedEntity](t: T): Route =
    complete(ApiResponse(t))

  def completeAsApiResponse[T](t: T, `object`: String): Route =
    complete(ApiResponse(t, `object`))

  def completeAsApiResponse[T <: ExposedEntity](upsertionResult: ErrorsOr[Result[T]]): Route =
    (createdOrUpdatedResp[T] orElse handleErrors)(upsertionResult)

  def completeAsValidatedApiResponse[T <: ExposedEntity](validatedT: ErrorsOr[T]): Route =
    validatedT match {
      case Valid(t) => completeAsApiResponse(t)
      case i        => handleErrors(i)
    }

  def completeAsValidatedOptApiResponse[T <: ExposedEntity](validatedOptT: ErrorsOr[Option[T]]): Route =
    validatedOptT match {
      case Valid(t) => completeAsOptApiResponse(t)
      case i        => handleErrors(i)
    }

  def completeSeqAsApiResponse[T](t: Seq[T]): Route =
    complete(ApiResponse(t))

  def completeSeqAsApiResponse[T <: ExposedEntity](upsertionResult: ErrorsOr[Result[Seq[T]]]): Route =
    (createdOrUpdatedSeqResp[T] orElse handleErrors)(upsertionResult)

  def completeAsEmptyResponse(result: ErrorsOr[Unit]): Route =
    toStrict {
      (noContentResp orElse handleErrors)(result)
    }

  private def createdOrUpdatedResp[T <: ExposedEntity]: PartialFunction[ErrorsOr[(ResultType, T)], Route] = {
    case Valid((ResultType.Created, t)) => complete(StatusCodes.Created, ApiResponse(t))
    case Valid((ResultType.Updated, t)) => complete(StatusCodes.OK, ApiResponse(t))
  }

  private def createdOrUpdatedSeqResp[T <: ExposedEntity]: PartialFunction[ErrorsOr[(ResultType, Seq[T])], Route] = {
    case Valid((ResultType.Created, t)) => complete(StatusCodes.Created, ApiResponse(t))
    case Valid((ResultType.Updated, t)) => complete(StatusCodes.OK, ApiResponse(t))
  }

  private def noContentResp: PartialFunction[ErrorsOr[_], Route] = {
    case Valid(_) => complete(StatusCodes.NoContent)
  }

  def handleErrors: PartialFunction[ErrorsOr[_], Route] = {
    case Invalid(i) if i.exists(_.isInstanceOf[BadRequest]) =>
      respondWithHeader(ValidationHeader(i)) {
        complete(StatusCodes.BadRequest, Errors(i))
      }

    case Invalid(i) if i.exists(_.isInstanceOf[Unauthorized]) =>
      respondWithHeader(ValidationHeader(i)) {
        complete(StatusCodes.Unauthorized, Errors(i))
      }

    case Invalid(i) =>
      respondWithHeader(ValidationHeader(i)) {
        complete(StatusCodes.NotFound, Errors(i))
      }
  }

  def completeAsPaginatedApiResponse[T](findResult: FindResult[T])(implicit pagination: Pagination): Route =
    extractRequestContext { ctx =>
      val (data, count) = findResult
      val paginationLinks = PaginationLinks(pagination, ctx.request.uri, count)
      complete(PaginatedApiResponse(data, paginationLinks))
    }

  def completeAsPaginatedApiResp[T](
      implicit
      pagination: Pagination,
    ): PartialFunction[ErrorsOr[FindResult[T]], Route] = {
    case Valid(t) => completeAsPaginatedApiResponse(t)
  }

  def completeAsValidatedPaginatedApiResponse[T](
      findResult: ErrorsOr[FindResult[T]],
    )(implicit
      pagination: Pagination,
    ): Route =
    (completeAsPaginatedApiResp[T] orElse handleErrors)(findResult)

  def completeAsApiResponseWithMetadata[T, MD <: Metadata[_, _]](
      s: Seq[T],
      metadata: MD,
    )(implicit
      pagination: Pagination,
    ): Route =
    extractRequestContext(implicit ctx => complete(ApiResponseWithMetadata(s, metadata)))

  def paginateWithDefaults(perPage: Int)(f: Pagination => Route): Route =
    parameters("page".as[Int].?(1), "per_page".as[Int].?(perPage)).as(Pagination)(f)

  private def splitValues(params: Map[String, String]): Seq[String] =
    splitValues("expand[]", params)

  private def splitValues(key: String, params: Map[String, String]): Seq[String] =
    immutable
      .ArraySeq
      .unsafeWrapArray(
        params.getOrElse(key, "").toLowerCase.split(",").filterNot(_.isEmpty),
      )

  def sortBySupport(route: Sortings => Route) =
    parameterMap { params =>
      val sortings = Sortings.parse(splitValues("sort_by[]", params))
      route(sortings)
    }
  def expandParameters[T <: BaseExpansions](n: String)(f: Boolean => T)(t: T => Route) =
    parameterMap { params =>
      val values = splitValues(params)
      t(f(values.contains(n)))
    }

  def expandParameters[T <: BaseExpansions](n1: String, n2: String)(f: (Boolean, Boolean) => T)(t: T => Route) =
    parameterMap { params =>
      val values = splitValues(params)
      t(f(values.contains(n1), values.contains(n2)))
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
    )(
      f: (Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(f(values.contains(n1), values.contains(n2), values.contains(n3)))
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(f(values.contains(n1), values.contains(n2), values.contains(n3), values.contains(n4)))
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(f(values.contains(n1), values.contains(n2), values.contains(n3), values.contains(n4), values.contains(n5)))
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
      n15: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
          values.contains(n15),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
      n15: String,
      n16: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
          values.contains(n15),
          values.contains(n16),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
      n15: String,
      n16: String,
      n17: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
          values.contains(n15),
          values.contains(n16),
          values.contains(n17),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
      n15: String,
      n16: String,
      n17: String,
      n18: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
          values.contains(n15),
          values.contains(n16),
          values.contains(n17),
          values.contains(n18),
        ),
      )
    }

  def expandParameters[T <: BaseExpansions](
      n1: String,
      n2: String,
      n3: String,
      n4: String,
      n5: String,
      n6: String,
      n7: String,
      n8: String,
      n9: String,
      n10: String,
      n11: String,
      n12: String,
      n13: String,
      n14: String,
      n15: String,
      n16: String,
      n17: String,
      n18: String,
      n19: String,
    )(
      f: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
          Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) => T,
    )(
      t: T => Route,
    ) =
    parameterMap { params =>
      val values = splitValues(params)
      t(
        f(
          values.contains(n1),
          values.contains(n2),
          values.contains(n3),
          values.contains(n4),
          values.contains(n5),
          values.contains(n6),
          values.contains(n7),
          values.contains(n8),
          values.contains(n9),
          values.contains(n10),
          values.contains(n11),
          values.contains(n12),
          values.contains(n13),
          values.contains(n14),
          values.contains(n15),
          values.contains(n16),
          values.contains(n17),
          values.contains(n18),
          values.contains(n19),
        ),
      )
    }
}
