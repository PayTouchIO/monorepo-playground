package io.paytouch.core.entities

import java.util.UUID

// To handle new and legacy API calls both parameters are optional, with no
// validation.  Once dashboard has moved over we can remove the
// `modifierSetIds` parameter and make `modifierSets` not optional.
final case class ProductModifierSetsAssignment(
    modifierSetIds: Option[Seq[UUID]] = None,
    modifierSets: Option[Seq[EntityOrdering]] = None,
  )
