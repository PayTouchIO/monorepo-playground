package io.paytouch.core.async

import io.paytouch.core.async.importers.parsers.EnrichedDataRow

package object importers {
  type Rows = Seq[EnrichedDataRow]
}
