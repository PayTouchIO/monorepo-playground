package io.paytouch.ordering.resources.features

import scala.collection._

import akka.http.scaladsl.server._

import io.paytouch.ordering.entities.Pagination
import io.paytouch.ordering.expansions.BaseExpansions

trait ParametersResource extends Directives {
  def paginateWithDefaults(perPage: Int)(f: Pagination => Route): Route =
    parameters("page".as[Int] ? 1, "per_page".as[Int] ? perPage).as(Pagination)(f)

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

  private def splitValues(params: Map[String, String]): Seq[String] =
    immutable.ArraySeq.unsafeWrapArray(params.getOrElse("expand[]", "").toLowerCase.split(",").filterNot(_.isEmpty))
}
