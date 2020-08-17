package io.paytouch.core.utils

import io.paytouch.core._
import io.paytouch.core.async.importers.parsers.ValidationError
import io.paytouch.utils.ValidatedUtils

// In the future this file will be removed

// Muliple.ErrorsOr[A]
object Multiple extends ValidatedUtils[errors.Error]

// MultipleExtraction.ErrorsOr[A]
object MultipleExtraction extends ValidatedUtils[ValidationError]
