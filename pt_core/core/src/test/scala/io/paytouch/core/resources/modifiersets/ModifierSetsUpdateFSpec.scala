package io.paytouch.core.resources.modifiersets

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.core.data.model.enums.ModifierSetType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ModifierSetsUpdateFSpec extends ModifierSetsFSpec {
  abstract class ModifierSetsUpsertFSpecContext extends ModifierSetResourceFSpecContext

  "POST /v1/modifier_sets.update?modifier_set_id=<modifier-set-id>" in {
    "if the request has a valid token" should {
      "good" in {
        "update existing modifier set with related locations and options and remove those not in the upsertion" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)
          val modifierSetUpsertion = {
            val min = random[Int].abs

            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = random[Int].abs.some,
              maximumOptionCount = (random[Int] % 3) match {
                case 0 => (min + 10).abs.some.some
                case 1 => Some(None)
                case 2 => None
              },
              maximumSingleOptionCount = None,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(modifierSetUpsertion, modifierSet.id)
            assertResponse(responseAs[ApiResponse[ModifierSet]].data)

            assertItemLocationDoesntExist(modifierSet.id, london.id)
            modifierOptionDao.findById(modifierOption2.id).await ==== None
          }
        }

        "associate maximumSingleOptionCount" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)

          val min = 2
          val maxSingle = 3

          val modifierSetUpsertion =
            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = min.some,
              maximumOptionCount = None,
              maximumSingleOptionCount = maxSingle.some.some,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(modifierSetUpsertion, modifierSet.id)
            assertResponse(responseAs[ApiResponse[ModifierSet]].data, optionsMaximumCount = maxSingle.some)

            assertItemLocationDoesntExist(modifierSet.id, london.id)
            modifierOptionDao.findById(modifierOption2.id).await ==== None
          }
        }
      }

      "bad" in {
        "error out if min < 0" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)
          val modifierSetUpsertion = {
            val min = random[Int].abs

            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = -1.some,
              maximumOptionCount = (random[Int] % 3) match {
                case 0 => (min + 10).abs.some.some
                case 1 => Some(None)
                case 2 => None
              },
              maximumSingleOptionCount = None,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if max < 0" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)
          val modifierSetUpsertion = {
            val min = random[Int].abs

            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = 0.some,
              maximumOptionCount = -1.some.some,
              maximumSingleOptionCount = None,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if min > max" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)
          val modifierSetUpsertion = {
            val min = random[Int].abs

            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = 10.some,
              maximumOptionCount = 5.some.some,
              maximumSingleOptionCount = None,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if min not specified but max is" in new ModifierSetsUpsertFSpecContext {
          val modifierSet = Factory.modifierSet(merchant).create
          val modifierLocation1 = Factory.modifierSetLocation(modifierSet, rome).create
          val modifierLocation2 = Factory.modifierSetLocation(modifierSet, london).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = modifierOption1.id)
          val modifierSetUpsertion = {
            val min = random[Int].abs

            random[ModifierSetUpdate].copy(
              `type` = Some(ModifierSetType.Addon),
              minimumOptionCount = None,
              maximumOptionCount = 1.some.some,
              maximumSingleOptionCount = None,
              locationOverrides = Map(
                rome.id -> Some(ItemLocationUpdate(active = Some(false))),
                london.id -> None,
              ),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.update?modifier_set_id=${modifierSet.id}", modifierSetUpsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierMinimumOptionCountNotSpecified")
          }
        }
      }
    }
  }
}
