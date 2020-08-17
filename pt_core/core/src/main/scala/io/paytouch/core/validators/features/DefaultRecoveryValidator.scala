package io.paytouch.core.validators.features

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.validators.RecoveryValidatorUtils

trait DefaultRecoveryValidator[Record <: SlickMerchantRecord]
    extends DefaultValidator[Record]
       with RecoveryValidatorUtils
