package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleScope, ArticleType, ImageSize }
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.entities.enums.{ ModifierSetType, UnitType }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.errors.ClientError

class ModifierSetsListSpec extends PtCoreClientSpec {
  abstract class ModifierSetsListSpecContext extends CoreClientSpecContext {
    val locationId: UUID = "478a122b-37fc-318c-a70f-f95cdd699b78"

    val modifierSetId: UUID = "1b163c2e-7ca9-4500-b7ac-70541129f5f6"

    val expectedExpansions = "expand[]=locations"
    val paging = "per_page=100"

    val listByIdsFilters = s"id[]=$modifierSetId"

    def params(filters: String) = s"$filters&$expectedExpansions&$paging"

    def assertModifierSetsListByIds(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.GET,
        "/v1/modifier_sets.list",
        authToken,
        queryParams = Some(params(listByIdsFilters)),
      )

  }

  "CoreClient" should {

    "call modifierSetsListByIds" should {

      "parse a modifier sets list" in new ModifierSetsListSpecContext with ProductFixture {
        val response = when(modifierSetsListByIds(Seq(modifierSetId)))
          .expectRequest(implicit request => assertModifierSetsListByIds)
          .respondWith(modifierSetsFileName)
        response.await.map(_.data) ==== Right(expectedModifierSets)
      }

      "parse rejection" in new ModifierSetsListSpecContext {
        val endpoint =
          completeUri(s"/v1/modifier_sets.list?${params(listByIdsFilters)}")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(modifierSetsListByIds(Seq(modifierSetId)))
          .expectRequest(implicit request => assertModifierSetsListByIds)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait ProductFixture { self: ModifierSetsListSpecContext =>
    val modifierSetsFileName = "/core/responses/modifier_sets_list.json"

    private val modifierLocationOverrides = Map(toUUID("478a122b-37fc-318c-a70f-f95cdd699b78") -> ItemLocation(true))
    private val modifierOptions = Seq(
      ModifierOption(
        id = "1b163c2e-7ca9-4500-b7ac-70541129f5f6",
        name = "Cow Chicken",
        price = 1 USD,
        position = 1,
        maximumCount = 1337.some,
        active = true,
      ),
    )

    private val modifiers: Seq[ModifierSet] =
      Seq(
        ModifierSet(
          id = "a508c3c2-9fae-34d6-83bb-bdd97413db93",
          `type` = ModifierSetType.Hold,
          name = "Chicken Aliquaexercitation",
          minimumOptionCount = 1,
          maximumOptionCount = None,
          singleChoice = false,
          force = false,
          locationOverrides = modifierLocationOverrides,
          options = modifierOptions,
        ),
      )
    val expectedModifierSets = modifiers
  }

}
