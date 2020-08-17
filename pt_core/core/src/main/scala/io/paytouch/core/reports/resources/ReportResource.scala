package io.paytouch.core.reports.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directive0, RequestContext, Route }
import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.EngineResponse
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportListFilters, ReportTopFilters }
import io.paytouch.core.reports.views._
import io.paytouch.core.resources.JsonResource
import io.paytouch.core.services.VariantService

import scala.concurrent.ExecutionContext

trait ReportResource extends JsonResource {

  implicit def ec: ExecutionContext

  def variantService: VariantService

  def completeAsEngineResponse[T <: scala.Product](result: EngineResponse[T]): Route =
    (engineResponse[T] orElse handleErrors)(result)

  private def engineResponse[T]: PartialFunction[EngineResponse[T], Route] = {
    case Valid(t) => complete(StatusCodes.OK, t)
  }

  def countViewEndpoint[V <: ReportAggrView](
      method: => Directive0,
      view: V,
    )(
      f: UserContext => ReportAggrFilters[V] => Route,
    )(implicit
      umGroupBy: EnumUnmarshaller[view.GroupBy],
    ) =
    path(s"${view.endpoint}.count") {
      method {
        defaultParameters[view.GroupBy] { (from, to, locationId, groupBy, withInterval, forceInterval, orderTypes) =>
          idsParameter(view) { ids =>
            authenticate { implicit user =>
              ReportInterval.validateAndDetectInterval(from, to, withInterval, forceInterval) match {
                case Valid(interval) =>
                  val filters =
                    ReportAggrFilters(
                      view,
                      from,
                      to,
                      locationId,
                      groupBy,
                      fields = Seq.empty,
                      ids,
                      orderTypes,
                      interval,
                    )
                  f(user)(filters)
                case i @ Invalid(_) => handleErrors(i)
              }
            }
          }
        }
      }
    }

  def sumViewEndpoint[V <: ReportAggrView](
      method: => Directive0,
      view: V,
    )(
      f: UserContext => ReportAggrFilters[V] => Route,
    )(implicit
      umGroupBy: EnumUnmarshaller[view.GroupBy],
      umFields: SeqEnumUnmarshaller[view.Field],
    ) =
    path(s"${view.endpoint}.sum") {
      method {
        defaultParameters[view.GroupBy] { (from, to, locationId, groupBy, withInterval, forceInterval, orderTypes) =>
          parameters("field[]".as[Seq[view.Field]]) { fields =>
            idsParameter(view) { ids =>
              authenticate { implicit user =>
                ReportInterval.validateAndDetectInterval(from, to, withInterval, forceInterval) match {
                  case Valid(interval) =>
                    val filters =
                      ReportAggrFilters(view, from, to, locationId, groupBy, fields, ids, orderTypes, interval)
                    f(user)(filters)
                  case i @ Invalid(_) => handleErrors(i)
                }
              }
            }
          }
        }
      }
    }

  def averageViewEndpoint[V <: ReportAggrView](
      method: => Directive0,
      view: V,
    )(
      f: UserContext => ReportAggrFilters[V] => Route,
    )(implicit
      umGroupBy: EnumUnmarshaller[view.GroupBy],
      umFields: SeqEnumUnmarshaller[view.Field],
    ) =
    path(s"${view.endpoint}.average") {
      method {
        defaultParameters[view.GroupBy] { (from, to, locationId, groupBy, withInterval, forceInterval, orderTypes) =>
          parameters("field[]".as[Seq[view.Field]]) { fields =>
            idsParameter(view) { ids =>
              authenticate { implicit user =>
                ReportInterval.validateAndDetectInterval(from, to, withInterval, forceInterval) match {
                  case Valid(interval) =>
                    val filters =
                      ReportAggrFilters(view, from, to, locationId, groupBy, fields, ids, orderTypes, interval)
                    f(user)(filters)
                  case i @ Invalid(_) => handleErrors(i)
                }
              }
            }
          }
        }
      }
    }

  def topViewEndpoint[V <: ReportTopView](
      method: => Directive0,
      view: V,
    )(
      f: UserContext => ReportTopFilters[V] => Route,
    )(implicit
      umCriterion: SeqEnumUnmarshaller[view.OrderBy],
    ) =
    path(s"${view.endpoint}.top") {
      method {
        parameters(
          "from".as[LocalDateTime],
          "to".as[LocalDateTime],
          "location_id".as[UUID].?,
          "order_by[]".as[Seq[view.OrderBy]],
          "n".as[Int].?(5),
        ) { (from, to, locationId, orderByFields, n) =>
          authenticate { implicit user =>
            val interval = ReportInterval.NoInterval
            val filters = ReportTopFilters(view, from, to, locationId, interval, orderByFields, n)
            f(user)(filters)
          }
        }
      }
    }

  def listViewEndpoint[V <: ReportListView](
      method: => Directive0,
      view: V,
    )(
      f: RequestContext => UserContext => ReportListFilters[V] => Route,
    )(implicit
      umOrderBy: SeqEnumUnmarshaller[view.OrderBy],
      umOrderType: SeqEnumUnmarshaller[OrderType],
    ) =
    path(s"${view.endpoint}.list") {
      method {
        paginateWithDefaults(30) { implicit pagination =>
          parameters(
            "from".as[LocalDateTime],
            "to".as[LocalDateTime],
            "location_id".as[UUID].?,
            "order_by[]".as[Seq[view.OrderBy]].?,
            "field[]".as[Seq[view.OrderBy]],
            "id[]".as[Seq[UUID]].?,
            "category_id[]".as[Seq[UUID]].?,
            "order_types[]".as[Seq[OrderType]].?,
          ) { (from, to, locationId, orderByFields, fields, ids, categoryIds, orderTypes) =>
            extractRequestContext { implicit ctx =>
              authenticate { implicit user =>
                val filters =
                  ReportListFilters(view, from, to, locationId, fields, orderByFields, ids, categoryIds, orderTypes)
                f(ctx)(user)(filters)
              }
            }
          }
        }
      }
    }

  private def defaultParameters[GroupBy <: GroupByEnum](
      f: (LocalDateTime, LocalDateTime, Option[UUID], Option[GroupBy], Boolean, Option[ReportInterval],
          Option[Seq[OrderType]]) => Route,
    )(implicit
      umGroupBy: EnumUnmarshaller[GroupBy],
      umOrderType: SeqEnumUnmarshaller[OrderType],
    ): Route =
    parameters(
      "from".as[LocalDateTime],
      "to".as[LocalDateTime],
      "location_id".as[UUID].?,
      "group_by".as[GroupBy].?,
      "with_interval".as[Boolean].?(false),
      "force_interval".as[ReportInterval].?,
      "order_types[]".as[Seq[OrderType]].?,
    )(f)

  private def idsParameter[V <: ReportAggrView](view: V)(f: (Option[Seq[UUID]]) => Route): Route =
    if (view.mandatoryIdsFilter) parameters("id[]".as[Seq[UUID]])(ids => f(Some(ids)))
    else parameters("id[]".as[Seq[UUID]].?)(f)
}
