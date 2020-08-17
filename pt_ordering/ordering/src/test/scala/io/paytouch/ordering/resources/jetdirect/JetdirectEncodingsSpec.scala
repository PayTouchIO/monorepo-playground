package io.paytouch.ordering.resources.jetdirect

import io.paytouch.ordering.data.model.JetdirectConfig
import io.paytouch.ordering.jetdirect.JetdirectEncodings
import io.paytouch.ordering.utils.PaytouchSpec

class JetdirectEncodingsSpec extends PaytouchSpec with JetdirectEncodings {

  "calculateJetdirectHashCode" should {
    "return the expected value" in {
      val merchantId = ""
      val terminalId = "TESTTERMINAL"
      val key = "1234567890abcdefghijk"
      val securityToken = "1234567890ABCDEFGHIJKabcdefghijk"
      val reference = "158898-12528"
      val amount = "15.00"
      val expected =
        "269a00d768e293e2fa1a746f70379489d438ba10eacb62f2a30b530dd57c75d4f30fd7b544785f1f7db452acd868bb332b2e2048bf686ecee15d42a4b7b0add4"

      implicit val jc: JetdirectConfig =
        JetdirectConfig(merchantId = merchantId, terminalId = terminalId, key = key, securityToken = securityToken)

      calculateJetdirectHashCode(reference, amount) ==== expected
    }
  }

  "calculateJetdirectReturnHashCode" should {
    "return the expected value" in {
      val merchantId = ""
      val terminalId = "TESTTERMINAL"
      val key = "1234567890abcdefghijk"
      val securityToken = "1234567890ABCDEFGHIJKabcdefghijk"
      val orderNumber = "c3afc4d9-cb64-4dc2-87b9-1f876828f5ef"
      val amount = "254.00"
      val responseText = "APPROVED"
      val expected =
        "d3eb9b0c9df0094505c1985a5019882bc330deb400c6722aa9047f30358e7ce5bb27372945d56733268fd62902fe6a6e7852523d90ffb26d5c253baa1176c217"

      implicit val jc: JetdirectConfig =
        JetdirectConfig(merchantId = merchantId, terminalId = terminalId, key = key, securityToken = securityToken)

      calculateJetdirectReturnHashCode(orderNumber, amount, responseText) ==== expected
    }
  }
}
