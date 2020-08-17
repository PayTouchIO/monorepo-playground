package io.paytouch.core.utils

import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.entities.enums.{ MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.utils.FutureHelpers
import org.specs2.matcher.{ Matchers, MustThrownExpectations }

trait SetupStepsAssertions extends MustThrownExpectations with Matchers with FutureHelpers {

  def assertSetupStepCompleted(merchant: MerchantRecord, step: MerchantSetupSteps) =
    eventually {
      val merchantEntity =
        MockedRestApi
          .merchantService
          .findById(merchant.id)(MerchantExpansions.none.copy(withSetupSteps = true))
          .await
          .get
      val status = merchantEntity.setupSteps.getOrElse(Map.empty).get(step)
      status ==== Some(MerchantSetupStatus.Completed)
    }

}
