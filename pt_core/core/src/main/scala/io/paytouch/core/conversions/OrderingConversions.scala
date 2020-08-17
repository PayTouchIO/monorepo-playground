package io.paytouch.core.conversions

import io.paytouch.core.data.model.ModelOrdering
import io.paytouch.core.entities.EntityOrdering

trait OrderingConversions {

  def convertOrdering(ordering: Seq[EntityOrdering]) = ordering.map(o => ModelOrdering(o.id, o.position))
}
