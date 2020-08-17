package io.paytouch.core.resources.modifiersets

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.core.data.model.enums.ModifierSetType
import io.paytouch.core.entities._

class ModifierSetsCreateFSpec extends ModifierSetsFSpec {
  abstract class ModifierSetsUpsertFSpecContext extends ModifierSetResourceFSpecContext

  "POST /v1/modifier_sets.create?modifier_set_id=<modifier-set-id>" in {
    "if the request has a valid token" should {
      "good" in {
        "create new modifier set with related locations and options and associate optionsMaximumCount" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = UUID.randomUUID)
          val modifierSetCreation = {
            val min = random[Int].abs

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = min.some,
              maximumOptionCount = if (random[Boolean]) (min + 10).abs.some.some else None,
              maximumSingleOptionCount = None,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(modifierSetCreation, newModifierSetId)
            assertResponse(responseAs[ApiResponse[ModifierSet]].data)
          }
        }

        "associate maximumSingleOptionCount" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = UUID.randomUUID)

          val min = random[Int].abs
          val max = min + 10
          val maxSingle = max - 5

          val modifierSetCreation =
            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = min.some,
              maximumOptionCount = max.some.some,
              maximumSingleOptionCount = maxSingle.some,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(modifierSetCreation, newModifierSetId)
            assertResponse(responseAs[ApiResponse[ModifierSet]].data, optionsMaximumCount = maxSingle.some)
          }
        }
      }

      "bad" in {
        "error out if min < 0" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion]
          val modifierSetCreation = {
            val min = random[Int].abs

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = -1.some,
              maximumOptionCount = if (random[Boolean]) (min + 10).abs.some.some else None,
              maximumSingleOptionCount = None,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if max < 0" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion]
          val modifierSetCreation = {
            val min = random[Int].abs

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = 0.some,
              maximumOptionCount = -1.some.some,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if min > max" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion]
          val modifierSetCreation = {
            val min = random[Int].abs

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = 10.some,
              maximumOptionCount = 5.some.some,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if min > max" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion]
          val modifierSetCreation = {
            val min = random[Int].abs

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = -1.some,
              maximumOptionCount = -5.some.some,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ModifierOptionErrors")
          }
        }

        "error out if neither min/max nor singleChoice/force are specified" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = UUID.randomUUID)
          val modifierSetCreation =
            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = None,
              maximumOptionCount = None,
              singleChoice = None,
              force = None,
              maximumSingleOptionCount = None,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NeitherModifierCountsNorLegacyBooleanFlagsAreSepcified")
          }
        }

        "error out if singleMax > max" in new ModifierSetsUpsertFSpecContext {
          val newModifierSetId = UUID.randomUUID
          val randomModifierOptionUpsertion = random[ModifierOptionUpsertion].copy(id = UUID.randomUUID)
          val modifierSetCreation = {
            val min = random[Int].abs
            val max = min + 10
            val maxSingle = max + 1

            random[ModifierSetCreation].copy(
              `type` = ModifierSetType.Addon,
              minimumOptionCount = min.some,
              maximumOptionCount = max.some.some,
              maximumSingleOptionCount = maxSingle.some,
              locationOverrides = Map(rome.id -> Some(ItemLocationUpdate(active = Some(true)))),
              options = Some(Seq(randomModifierOptionUpsertion)),
            )
          }

          Post(s"/v1/modifier_sets.create?modifier_set_id=$newModifierSetId", modifierSetCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("MaximumOptionCountMustNotBeSmallerThanMaximumSingleOptionCount")
          }
        }
      }
    }
  }
}
