package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import cats.Show

trait Parameters {
  type Parameter = (String, Seq[String])

  implicit val showString: Show[String] = Show.show(identity)
  implicit val showUuid: Show[UUID] = Show.fromToString
  implicit val showBoolean: Show[Boolean] = Show.fromToString

  implicit def valToOptParameter[T](args: (String, T))(implicit show: Show[T]): Option[Parameter] = {
    val (key, value) = args
    Some(key -> Seq(show.show(value)))
  }

  implicit def seqToOptParameter[T](args: (String, Seq[T]))(implicit show: Show[T]): Option[Parameter] = {
    val (key, values) = args
    Some(key -> values.map(show.show))
  }

  implicit def optionToOptParameter[T](args: (String, Option[T]))(implicit show: Show[T]): Option[Parameter] = {
    val (key, value) = args
    value.flatMap(v => key -> v)
  }

  def filterParameters(params: Option[Parameter]*): String =
    filterParameters(params.flatten.toMap)

  private def filterParameters(params: Map[String, Seq[String]]): String =
    params.map { case (name, values) => s"$name=${values.mkString(",")}" }.mkString("&")

  def expandParameter(expansions: String*): String =
    s"expand[]=${expansions.mkString(",")}"
}
