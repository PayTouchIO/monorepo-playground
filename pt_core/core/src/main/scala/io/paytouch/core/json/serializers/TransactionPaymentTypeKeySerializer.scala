package io.paytouch.core.json.serializers

import io.paytouch.core.data.model.enums.TransactionPaymentType
import org.json4s.CustomKeySerializer
import io.paytouch.core.utils.RichString._

case object TransactionPaymentTypeKeySerializer
    extends CustomKeySerializer[TransactionPaymentType](_ =>
      (
        {
          case x: String if TransactionPaymentType.withNameInsensitiveOption(x.underscore).isDefined =>
            TransactionPaymentType.withNameInsensitive(x.underscore)
        },
        {
          case u: TransactionPaymentType => u.entryName
        },
      ),
    )
