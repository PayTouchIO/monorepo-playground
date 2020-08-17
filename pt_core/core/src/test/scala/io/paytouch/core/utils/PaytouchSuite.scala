package io.paytouch.core.utils

import org.specs2._

trait PaytouchSuite extends mutable.Specification with ScalaCheck {
  final protected type MatchResult[+A] = matcher.MatchResult[A]
}
