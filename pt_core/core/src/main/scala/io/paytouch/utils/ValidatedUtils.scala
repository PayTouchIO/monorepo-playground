package io.paytouch.utils

import scala.concurrent._

import cats._
import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._

// In the future this file will be removed
trait ValidatedUtils[EE] extends LazyLogging {
  final type ErrorsOr[+A] = ValidatedNel[EE, A]

  def failure[A](error: EE): ErrorsOr[A] = {
    logger.debug("{}", error)
    Validated.Invalid(error).toValidatedNel
  }

  def failure[A](errors: Nel[EE]): ErrorsOr[A] = {
    logger.debug("{}", errors.toList)
    Validated.Invalid(errors)
  }

  def success[A](t: A): ErrorsOr[A] =
    Validated.Valid(t)

  def successOpt[T](t: T): ErrorsOr[Option[T]] =
    Validated.Valid(t.some)

  def empty[T]: ErrorsOr[Option[T]] =
    Validated.Valid(none)

  def sequence[A](vds: Iterable[ErrorsOr[Seq[A]]]): ErrorsOr[Seq[A]] =
    genericSequence(vds)(_ ++ _, Seq.empty)

  private def genericSequence[A](vds: Iterable[ErrorsOr[A]])(f: (A, A) => A, default: A): ErrorsOr[A] = {
    @scala.annotation.tailrec
    def loop(remaining: Iterable[ErrorsOr[A]], soFar: ErrorsOr[A]): ErrorsOr[A] =
      remaining match {
        case Nil => soFar
        case head :: tail =>
          loop(tail, combine(head, soFar)(f))
      }

    loop(vds, Validated.Valid(default))
  }

  def futSequence[A](
      vds: Iterable[Future[ErrorsOr[Seq[A]]]],
    )(implicit
      ec: ExecutionContext,
    ): Future[ErrorsOr[Seq[A]]] =
    Future.sequence(vds).map(sequence)

  def combineSeq[A](validations: Seq[ErrorsOr[A]]): ErrorsOr[Seq[A]] =
    Traverse[List].sequence[ErrorsOr, A](validations.toList)

  def combine[A, B, XX](vd1: ErrorsOr[A], vd2: ErrorsOr[B])(f: (A, B) => XX): ErrorsOr[XX] =
    Apply[ErrorsOr].map2(vd1, vd2)(f)

  def combine[A, B, C, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
    )(
      f: (A, B, C) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map3(vd1, vd2, vd3)(f)

  def combine[A, B, C, D, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
    )(
      f: (A, B, C, D) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map4(vd1, vd2, vd3, vd4)(f)

  def combine[A, B, C, D, E, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
    )(
      f: (A, B, C, D, E) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map5(vd1, vd2, vd3, vd4, vd5)(f)

  def combine[A, B, C, D, E, F, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
    )(
      f: (A, B, C, D, E, F) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map6(vd1, vd2, vd3, vd4, vd5, vd6)(f)

  def combine[A, B, C, D, E, F, G, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
    )(
      f: (A, B, C, D, E, F, G) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map7(vd1, vd2, vd3, vd4, vd5, vd6, vd7)(f)

  def combine[A, B, C, D, E, F, G, H, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
    )(
      f: (A, B, C, D, E, F, G, H) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map8(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8)(f)

  def combine[A, B, C, D, E, F, G, H, I, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
    )(
      f: (A, B, C, D, E, F, G, H, I) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map9(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
    )(
      f: (A, B, C, D, E, F, G, H, I, L) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map10(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map11(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map12(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map13(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map14(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map15(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr].map16(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
      vd17: ErrorsOr[S],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr]
      .map17(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
      vd17: ErrorsOr[S],
      vd18: ErrorsOr[T],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr]
      .map18(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17, vd18)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
      vd17: ErrorsOr[S],
      vd18: ErrorsOr[T],
      vd19: ErrorsOr[U],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr]
      .map19(vd1, vd2, vd3, vd4, vd5, vd6, vd7, vd8, vd9, vd10, vd11, vd12, vd13, vd14, vd15, vd16, vd17, vd18, vd19)(f)

  def combine[A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V, XX](
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
      vd17: ErrorsOr[S],
      vd18: ErrorsOr[T],
      vd19: ErrorsOr[U],
      vd20: ErrorsOr[V],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr]
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
      vd1: ErrorsOr[A],
      vd2: ErrorsOr[B],
      vd3: ErrorsOr[C],
      vd4: ErrorsOr[D],
      vd5: ErrorsOr[E],
      vd6: ErrorsOr[F],
      vd7: ErrorsOr[G],
      vd8: ErrorsOr[H],
      vd9: ErrorsOr[I],
      vd10: ErrorsOr[L],
      vd11: ErrorsOr[M],
      vd12: ErrorsOr[N],
      vd13: ErrorsOr[O],
      vd14: ErrorsOr[P],
      vd15: ErrorsOr[Q],
      vd16: ErrorsOr[R],
      vd17: ErrorsOr[S],
      vd18: ErrorsOr[T],
      vd19: ErrorsOr[U],
      vd20: ErrorsOr[V],
      vd21: ErrorsOr[Z],
    )(
      f: (A, B, C, D, E, F, G, H, I, L, M, N, O, P, Q, R, S, T, U, V, Z) => XX,
    ): ErrorsOr[XX] =
    Apply[ErrorsOr]
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
