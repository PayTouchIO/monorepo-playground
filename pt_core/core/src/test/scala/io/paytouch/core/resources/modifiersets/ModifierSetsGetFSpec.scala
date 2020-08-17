package io.paytouch.core.resources.modifiersets

import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ModifierSetsGetFSpec extends ModifierSetsFSpec {

  abstract class ModifierSetsGetFSpecContext extends ModifierSetResourceFSpecContext

  "GET /v1/modifier_sets.get?modifier_set=<id>" in {

    "if the request has a valid token" should {

      "return a modifier set with its options ordered by position and name" in new ModifierSetsGetFSpecContext {
        val modifierSet = Factory.modifierSet(merchant).create

        val product = Factory.simpleProduct(merchant).create
        Factory.modifierSetProduct(modifierSet, product).create
        Factory.modifierSetLocation(modifierSet, london, active = Some(true)).create
        Factory.modifierSetLocation(modifierSet, rome, active = Some(false)).create

        val option1 = Factory.modifierOption(modifierSet, name = Some("Ketchup"), position = Some(1)).create
        val option2 = Factory.modifierOption(modifierSet, name = Some("Mayonnaise"), position = Some(2)).create
        val option3 = Factory.modifierOption(modifierSet, name = Some("BBQ Sauce"), position = Some(2)).create

        Get(s"/v1/modifier_sets.get?modifier_set_id=${modifierSet.id}&expand[]=products_count,locations")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertResponse(
            responseAs[ApiResponse[ModifierSet]].data,
            productsCount = Some(1),
            locations = Some(Seq(london, rome)),
            options = Some(Seq(option1, option3, option2)),
          )
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ModifierSetsGetFSpecContext {
        val modifierSet = Factory.modifierSet(merchant).create

        Get(s"/v1/modifier_sets.get?modifier_set_id=${modifierSet.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
