package io.paytouch.ordering.utils

import com.typesafe.config.ConfigFactory

import java.util.Currency

import scala.concurrent.duration._

import org.specs2.execute.AsResult
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification

import io.paytouch.ordering.json.JsonSupport

trait PaytouchSpec
    extends PaytouchSuite
       with FutureHelpers
       with TestExecutionContext
       with Matchers
       with JsonSupport
       with FixturesSupport {
  lazy val USD = Currency.getInstance("USD")

  private lazy val config = ConfigFactory.load()

  def afterAWhile[T: AsResult](result: => T): T =
    eventually(config.getInt("test.after_a_while_repetitions"), 100.millis)(result)
}
