package io.paytouch.core.utils

import scala.concurrent.duration._

import org.specs2.execute.AsResult
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito

import io.paytouch.utils._

abstract class PaytouchSpec
    extends PaytouchSuite
       with Mockito
       with FutureHelpers
       with ValidatedHelpers
       with FixturesSupport
       with FileSupport
       with TestExecutionContext
       with Matchers
       with FixtureRandomGenerators {
  args.report(
    failtrace = true,
    showtimes = true,
  )

  private val slowTests = Seq("ImageUpload", "Import", "Barcode", "Sample")

  private val className = this.getClass.getSimpleName

  if (slowTests.exists(className.contains))
    section("slow")

  def afterAWhile[T: AsResult](result: => T): T =
    eventually(retries = 30, sleep = 1.second)(result)
}
