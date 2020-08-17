package io.paytouch.ordering.utils.validation

import cats._
import cats.data._
import cats.implicits._

trait ValidationCombine[EE] {
  implicit val nelSemigroup: Semigroup[NonEmptyList[EE]] = SemigroupK[NonEmptyList].algebra[EE]

  type Validation[T] = ValidatedNel[EE, T]

  def combineAll[A](validations: Seq[Validation[A]]): Option[Validation[A]] =
    Semigroup.combineAllOption(validations)(SemigroupK[Validation].algebra[A])

  def sequence[A](validations: Seq[Validation[A]]): Validation[Seq[A]] =
    Traverse[List].sequence[Validation, A](validations.toList)

  def combine[A, B, XX](vd1: Validation[A], vd2: Validation[B])(f: (A, B) => XX): Validation[XX] =
    Apply[Validation].map2(vd1, vd2)(f)

  def combine[A, B, C, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
    )(
      f: (A, B, C) => XX,
    ): Validation[XX] =
    Apply[Validation].map3(vd1, vd2, vd3)(f)

  def combine[A, B, C, D, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
    )(
      f: (A, B, C, D) => XX,
    ): Validation[XX] =
    Apply[Validation].map4(vd1, vd2, vd3, vd4)(f)

  def combine[A, B, C, D, E, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
    )(
      f: (A, B, C, D, E) => XX,
    ): Validation[XX] =
    Apply[Validation].map5(vd1, vd2, vd3, vd4, vd5)(f)

  def combine[A, B, C, D, E, F, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
    )(
      f: (A, B, C, D, E, F) => XX,
    ): Validation[XX] =
    Apply[Validation].map6(vd1, vd2, vd3, vd4, vd5, vd6)(f)

  def combine[A, B, C, D, E, F, G, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
    )(
      f: (A, B, C, D, E, F, G) => XX,
    ): Validation[XX] =
    Apply[Validation].map7(vd1, vd2, vd3, vd4, vd5, vd6, vd7)(f)

  def combine[A, B, C, D, E, F, G, H, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
    )(
      f: (A, B, C, D, E, F, G, H) => XX,
    ): Validation[XX] =
    Apply[Validation].map8(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8)(f)

  def combine[A, B, C, D, E, F, G, H, I, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
    )(
      f: (A, B, C, D, E, F, G, H, I) => XX,
    ): Validation[XX] =
    Apply[Validation].map9(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
    )(
      f: (A, B, C, D, E, F, G, H, I, L) => XX,
    ): Validation[XX] =
    Apply[Validation].map10(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M) => XX,
    ): Validation[XX] =
    Apply[Validation].map11(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N) => XX,
    ): Validation[XX] =
    Apply[Validation].map12(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O) => XX,
    ): Validation[XX] =
    Apply[Validation].map13(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P) => XX,
    ): Validation[XX] =
    Apply[Validation].map14(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q) => XX,
    ): Validation[XX] =
    Apply[Validation].map15(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R) => XX,
    ): Validation[XX] =
    Apply[Validation].map16(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
      vd17: Validation[S],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S) => XX,
    ): Validation[XX] =
    Apply[Validation]
      .map17(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
      vd17: Validation[S],
      vd18: Validation[T],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T) => XX,
    ): Validation[XX] =
    Apply[Validation]
      .map18(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17, vd18)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
      vd17: Validation[S],
      vd18: Validation[T],
      vd19: Validation[U],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U) => XX,
    ): Validation[XX] =
    Apply[Validation]
      .map19(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17, vd18, vd19)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
      vd17: Validation[S],
      vd18: Validation[T],
      vd19: Validation[U],
      vd20: Validation[V],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V) => XX,
    ): Validation[XX] =
    Apply[Validation]
      .map20(
        vd1,
        vd2,
        vd3,
        vd4,
        vd5,
        vd6,
        vd7,
        vd8,
        vd9,
        vd10,
        vd11,
        vd12,
        vd13,
        vd14,
        vd15,
        vd16,
        vd17,
        vd18,
        vd19,
        vd20,
      )(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V, Z, XX](
      vd1: Validation[A],
      vd2: Validation[B],
      vd3: Validation[C],
      vd4: Validation[D],
      vd5: Validation[E],
      vd6: Validation[F],
      vd7: Validation[G],
      vd8: Validation[H],
      vd9: Validation[I],
      vd10: Validation[L],
      vd11: Validation[M],
      vd12: Validation[N],
      vd13: Validation[O],
      vd14: Validation[P],
      vd15: Validation[Q],
      vd16: Validation[R],
      vd17: Validation[S],
      vd18: Validation[T],
      vd19: Validation[U],
      vd20: Validation[V],
      vd21: Validation[Z],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V, Z) => XX,
    ): Validation[XX] =
    Apply[Validation]
      .map21(
        vd1,
        vd2,
        vd3,
        vd4,
        vd5,
        vd6,
        vd7,
        vd8,
        vd9,
        vd10,
        vd11,
        vd12,
        vd13,
        vd14,
        vd15,
        vd16,
        vd17,
        vd18,
        vd19,
        vd20,
        vd21,
      )(f)

}
