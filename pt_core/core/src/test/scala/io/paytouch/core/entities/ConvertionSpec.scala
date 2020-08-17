package io.paytouch.core.entities

import io.paytouch.core.utils.PaytouchSpec
import org.specs2.ScalaCheck

abstract class ConvertionSpec extends PaytouchSpec with ScalaCheck {
  @scala.annotation.nowarn("msg=Auto-application")
  implicit val userContext = random[UserContext]
}
