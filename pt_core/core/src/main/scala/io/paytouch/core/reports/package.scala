package io.paytouch.core

import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.reports.entities.ReportResponse

package object reports {

  type EngineResponse[T] = ErrorsOr[ReportResponse[T]]

}
