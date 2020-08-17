package io.paytouch

import cats._
import cats.instances.all._

abstract class Opaque[Value] {
  def value: Value
}

object Opaque {
  sealed trait CanCastTo[F[_], A] extends SerializableProduct {
    def cast: F[A]
  }

  abstract class Iso[Value, ValueIso, OpaqueIso](
      toOpaqueIso: ValueIso => OpaqueIso,
      toValueIso: Value => ValueIso,
    ) extends Opaque[Value]
         with CanCastTo[Id, OpaqueIso] {
    final override lazy val cast: OpaqueIso =
      toOpaqueIso(toValueIso(value))
  }

  abstract class Prism[Value, ValuePrism, OpaquePrism](
      toOpaquePrism: ValuePrism => OpaquePrism,
      toValuePrism: Value => ValuePrism,
    ) extends Opaque[Value]
         with CanCastTo[Option, OpaquePrism] {
    final override lazy val cast: Option[OpaquePrism] =
      castOrLeft.toOption

    final lazy val castOrLeft: Either[Throwable, OpaquePrism] =
      castF[Either[Throwable, *]]

    final def castF[F[_]](implicit ae: ApplicativeError[F, Throwable]): F[OpaquePrism] =
      ae.catchNonFatal(toOpaquePrism(toValuePrism(value)))
  }
}

abstract class OpaqueCompanion[Value, O <: Opaque[Value]] extends Function1[Value, O] {
  final implicit val eq: Eq[O] =
    Eq.fromUniversalEquals

  final implicit def order(implicit ord: Order[Value]): Order[O] =
    Order.by(_.value)

  final implicit class OpaqueOps(o: O) {
    final def map(f: Value => Value): O =
      apply(f(o.value))
  }
}
