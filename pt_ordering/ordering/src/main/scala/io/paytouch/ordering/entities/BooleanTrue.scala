package io.paytouch.ordering.entities

sealed trait BooleanWithDefault {
  def bool: Boolean
}

object BooleanWithDefault {

  implicit def toBoolean(bwd: BooleanWithDefault): Boolean = bwd.bool

  implicit def toBooleanOpt(bwd: BooleanWithDefault): Option[Boolean] =
    Some(toBoolean(bwd))
}

final case class BooleanTrue(bool: Boolean) extends BooleanWithDefault

object BooleanTrue {

  implicit def fromBoolean(bool: Boolean): BooleanTrue =
    new BooleanTrue(bool)
}

final case class BooleanFalse(bool: Boolean) extends BooleanWithDefault

object BooleanFalse {

  implicit def fromBoolean(bool: Boolean): BooleanFalse =
    new BooleanFalse(bool)
}
