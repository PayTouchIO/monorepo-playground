package io.paytouch.core.resources.modifiersets

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ModifierSetsDeleteFSpec extends ModifierSetsFSpec {

  abstract class ModifierSetDeleteResourceFSpecContext extends ModifierSetResourceFSpecContext {
    def assertModifierSetDoesntExist(id: UUID) = modifierSetDao.findById(id).await should beNone
    def assertModifierSetExists(id: UUID) = modifierSetDao.findById(id).await should beSome
  }

  "POST /v1/modifier_sets.delete" in {

    "if request has valid token" in {
      "if modifierSet doesn't exist" should {
        "do nothing and return 204" in new ModifierSetDeleteResourceFSpecContext {
          val nonExistingModifierSetId = UUID.randomUUID

          Post(s"/v1/modifier_sets.delete", Ids(ids = Seq(nonExistingModifierSetId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertModifierSetDoesntExist(nonExistingModifierSetId)
          }
        }
      }

      "if modifierSet belongs to the merchant" should {
        "delete the modifierSet and return 204" in new ModifierSetDeleteResourceFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          Factory.modifierOption(modifierSet).create
          Factory.modifierOption(modifierSet).create

          Post(s"/v1/modifier_sets.delete", Ids(ids = Seq(modifierSet.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertModifierSetDoesntExist(modifierSet.id)
            modifierOptionDao.findByModifierSetId(modifierSet.id).await ==== Seq.empty
          }
        }
      }

      "if modifierSet belongs to a different merchant" should {
        "do not delete the modifierSet and return 204" in new ModifierSetDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorModifierSet = Factory.modifierSet(competitor).create

          Post(s"/v1/modifier_sets.delete", Ids(ids = Seq(competitorModifierSet.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertModifierSetExists(competitorModifierSet.id)
          }
        }
      }
    }
  }
}
