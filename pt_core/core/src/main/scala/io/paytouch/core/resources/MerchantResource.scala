package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import io.paytouch.implicits._
import io.paytouch.core.data.model.enums.{ BusinessType, MerchantMode }
import io.paytouch.core.data.model.PaymentProcessorConfig.WorldpayConfig
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.expansions._
import io.paytouch.core.filters.MerchantFilters
import io.paytouch.core.services._

trait MerchantResource extends JsonResource with AdminAuthentication {
  def merchantService: MerchantService
  def adminMerchantService: AdminMerchantService
  def passwordResetService: PasswordResetService

  val merchantAdminRoutes: Route =
    concat(
      path("merchants.create") {
        post {
          parameter("merchant_id".as[UUID]) { id =>
            entity(as[MerchantCreation]) { creation =>
              authenticateAdmin { implicit admin =>
                onSuccess(adminMerchantService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("merchants.update") {
        post {
          parameter("merchant_id".as[UUID]) { id =>
            entity(as[AdminMerchantUpdate]) { update =>
              authenticateAdmin { implicit admin =>
                onSuccess(adminMerchantService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("merchants.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("business_type".as[BusinessType].?, "q".?, "ids[]".as[Seq[UUID]].?).as(MerchantFilters) {
              filters =>
                expandParameters(
                  "setup_steps",
                  "owners",
                  "locations",
                  "legal_details",
                )(MerchantExpansions.apply) { expansions =>
                  sortBySupport { sortings =>
                    authenticateAdmin { implicit admin =>
                      onSuccess(adminMerchantService.findAll(filters)(expansions, sortings)) { (merchants, count) =>
                        completeAsPaginatedApiResponse(merchants, count)
                      }
                    }
                  }
                }
            }
          }
        }
      },
      path("merchants.get") {
        get {
          parameter("merchant_id".as[UUID]) { id =>
            authenticateAdmin { implicit admin =>
              onSuccess(adminMerchantService.findById(id)(MerchantExpansions.all)) { result =>
                completeAsOptApiResponse(result)
              }
            }
          }
        }
      },
    )

  val merchantPublicRoutes: Route =
    concat(
      path("merchants.create") {
        post {
          parameter("merchant_id".as[UUID]) { id =>
            entity(as[PublicMerchantCreation]) { creation =>
              authorize(merchantService.verifyUrl(_)) {
                onSuccess(adminMerchantService.create(id, creation.toMerchantCreation)) { result =>
                  completeAsApiResponse(result)
                }
              }
            }
          }
        }
      },
      path("merchants.send_welcome_email") {
        post {
          parameter("email".as[String]) { email =>
            passwordResetService.sendWelcomePasswordReset(email)
            complete(StatusCodes.NoContent)
          }
        }
      },
      path("merchants.create_and_send_welcome_email") {
        post {
          entity(as[PublicMerchantCreation]) { creation =>
            val result = adminMerchantService
              .create(UUID.randomUUID, creation.toMerchantCreation)
              .map(_.tapAs {
                passwordResetService.sendWelcomePasswordReset(creation.email)
              })
            onSuccess(result) { result =>
              completeAsApiResponse(result)
            }
          }
        }
      },
    )

  val merchantApiRoutes: Route =
    concat(
      path("merchants.skip_setup_step") {
        post {
          parameter("step".as[MerchantSetupSteps]) { step =>
            authenticate { implicit user =>
              onSuccess(merchantService.skipSetupStep(step))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("merchants.reset_setup_step") {
        post {
          parameter("step".as[MerchantSetupSteps]) { step =>
            authenticate { implicit user =>
              onSuccess(merchantService.resetSetupStep(step))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("merchants.switch_mode_to") {
        post {
          parameter("mode" ! "production") {
            authenticate { implicit user =>
              onSuccess(merchantService.switchModeTo(MerchantMode.Production)) { result =>
                completeAsValidatedOptApiResponse(result)
              }
            }
          }
        }
      },
      path("merchants.complete_setup_step") {
        post {
          parameter("step".as[MerchantSetupSteps]) { step =>
            authenticate { implicit user =>
              onSuccess(merchantService.completeSetupStep(step))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("merchants.switch_mode_to") {
        post {
          parameter("mode" ! "production") {
            authenticate { implicit user =>
              onSuccess(merchantService.switchModeTo(MerchantMode.Production)) { result =>
                completeAsValidatedOptApiResponse(result)
              }
            }
          }
        }
      },
      path("merchants.update") {
        post {
          parameter("merchant_id".as[UUID]) { id =>
            entity(as[ApiMerchantUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(merchantService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("merchants.me") {
        get {
          userOrAppAuthenticate { implicit user =>
            onSuccess(merchantService.findById(user.merchantId)(MerchantExpansions.all)) { result =>
              completeAsOptApiResponse(result)
            }
          }
        }
      },
      path("merchants.set_worldpay_config") {
        post {
          entity(as[WorldpayConfigUpsertion]) { upsertion =>
            authenticate(implicit user =>
              onSuccess(merchantService.setWorldpayConfig(upsertion))(completeAsEmptyResponse),
            )
          }
        }
      },
      path("merchants.reset_payment_processor") {
        post {
          authenticate(implicit user => onSuccess(merchantService.resetPaymentProcessor())(completeAsEmptyResponse))
        }
      },
    )
}
