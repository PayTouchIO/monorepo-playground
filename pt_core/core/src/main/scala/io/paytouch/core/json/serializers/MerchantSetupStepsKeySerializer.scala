package io.paytouch.core.json.serializers

import io.paytouch.core.entities.enums.MerchantSetupSteps
import org.json4s.CustomKeySerializer
import io.paytouch.core.utils.RichString._

case object MerchantSetupStepsKeySerializer
    extends CustomKeySerializer[MerchantSetupSteps](_ =>
      (
        {
          case x: String if MerchantSetupSteps.withNameInsensitiveOption(x.underscore).isDefined =>
            MerchantSetupSteps.withNameInsensitive(x.underscore)
        },
        {
          case u: MerchantSetupSteps => u.entryName
        },
      ),
    )
