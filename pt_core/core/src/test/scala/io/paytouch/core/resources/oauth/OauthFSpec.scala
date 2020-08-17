package io.paytouch.core.resources.oauth

import io.paytouch.core.utils._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class OauthFSpec extends FSpec {

  abstract class OauthResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val oauthAppDao = daos.oauthAppDao
    val oauthAppSessionDao = daos.oauthAppSessionDao
    val oauthCodeDao = daos.oauthCodeDao

    val validRedirectUri = "http://amaka.external/oauth/redirect"
    val invalidRedirectUri = "http://other.external"
    val oauthApp = Factory
      .oauthApp(redirectUris = Some("http://amaka.external/"))
      .create
  }
}
