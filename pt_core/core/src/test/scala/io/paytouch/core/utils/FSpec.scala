package io.paytouch.core.utils

import java.io.File
import java.net.URL
import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import scala.concurrent._
import scala.jdk.CollectionConverters._

import akka.actor._
import akka.http.scaladsl.model.{ HttpMethods, _ }
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Route
import akka.testkit.TestProbe

import awscala.s3.{ Bucket, PutObjectResult, S3Client => AWSS3Client }

import cats.implicits._

import com.amazonaws.services.s3.model.{ ObjectListing, S3ObjectSummary }

import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import org.specs2.specification.Scope

import io.paytouch.implicits._

import io.paytouch.core.{ RestApi, ValidationHeaderName, ServiceConfigurations => Config }
import io.paytouch.core.async.sqs.SQSMessageSender
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.clients.auth0.Auth0Client
import io.paytouch.core.clients.urbanairship.WalletClient
import io.paytouch.core.data.daos.{ ConfiguredTestDatabase, DaoSpec, SessionDao }
import io.paytouch.core.data.model.LocationRecord
import io.paytouch.core.data.redis.ConfiguredRedis
import io.paytouch.core.entities.{ JsonWebToken, MerchantContext, UserContext }
import io.paytouch.core.entities.enums.{ CatalogType, ContextSource, LoginSource }
import io.paytouch.core.logging.MdcActor
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.ServiceConfigurations._
import io.paytouch.core.stubs._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.{ FileSupport, TestExecutionContext }
import io.paytouch.utils.Tagging._

abstract class FSpec extends DaoSpec with PaytouchRouteSpec with FileSupport with EncryptionSupport {
  val bcryptRounds = Config.bcryptRounds

  abstract class FSpecContext extends Scope {
    val routes = MockedRestApi.routes

    val sealedRoutes = Route.seal(routes)(
      implicitly,
      implicitly,
      MockedRestApi.myRejectionHandler,
      MockedRestApi.customExceptionHandler,
    )

    val mockMessageSender = MockedRestApi.mockMessageSenderProbe

    val mockAWSS3Client = MockedRestApi.mockAWSS3Client
    val mockBucket = MockedRestApi.imageMockBucket

    def assertErrorCode(expectedErrorCode: String): MatchResult[String] = {
      val maybeHeaderValue = header(ValidationHeaderName)

      maybeHeaderValue must beSome
      maybeHeaderValue.get.value() must contain(expectedErrorCode)
    }

    def assertErrorMessage(expectedErrorMessage: String): MatchResult[String] = {
      val actual = expectedErrorMessage // TODO

      actual must contain(expectedErrorMessage)
    }

    def assertErrorCodesAtLeastOnce(expectedErrorCodes: String*) =
      atLeastOnce(expectedErrorCodes)(assertErrorCode)

    def assertErrorCodesForeach(expectedErrorCodes: String*) =
      foreach(expectedErrorCodes)(assertErrorCode)

    def assertStatusCreated() = assertStatus(StatusCodes.Created)
    def assertStatusOK() = assertStatus(StatusCodes.OK)
    def assertStatus(expectedStatus: StatusCode) = status should beStatus(expectedStatus)

    private def beStatus(expectedStatus: StatusCode): Matcher[StatusCode] = { s: StatusCode =>
      (
        s == expectedStatus,
        s"$status == $expectedStatus",
        s"$status != $expectedStatus (response: $response)",
      )
    }
  }

  def createFileUploadEntityFromFile(file: File, fieldName: String): Future[RequestEntity] = {
    require(file.exists)

    Multipart
      .FormData
      .fromPath(fieldName, ContentTypes.`text/plain(UTF-8)`, file.toPath, chunkSize = 1000)
      .toEntity
      .pure[Future]
  }

  def MultiformDataRequest(
      target: Uri,
      file: File,
      fieldName: String,
    ): HttpRequest = {
    val request = for {
      e <- createFileUploadEntityFromFile(file, fieldName)
    } yield HttpRequest(HttpMethods.POST, uri = target, entity = e)
    request.await
  }

  implicit class RichZonedDateTime(zdt: ZonedDateTime) {
    def toUtc: ZonedDateTime =
      zdt.withZoneSameInstant(ZoneId.of("UTC")).withFixedOffsetZone
  }
}

object MockedRestApi extends Mockito with RestApi with ConfiguredTestDatabase {
  implicit lazy val testAsyncSystem = ActorSystem("unit-test-async")
  implicit lazy val ec = testAsyncSystem.dispatcher

  lazy val slowOpsDb = db

  lazy val AppHeaderName = Config.AppHeaderName
  lazy val VersionHeaderName = Config.VersionHeaderName
  lazy val corsAllowOrigins = Config.allowOrigins

  lazy val testSqsConsumersSystem = testAsyncSystem

  lazy val system = testAsyncSystem
  lazy val asyncSystem = testAsyncSystem
  lazy val sqsConsumersSystem = testSqsConsumersSystem

  lazy val mockObjectListing = mock[ObjectListing]
  mockObjectListing.getObjectSummaries.returns(List.empty[S3ObjectSummary].asJava)

  lazy val mockAWSS3Client = mock[AWSS3Client]
  mockAWSS3Client.createBucket(s3ImagesBucket) returns imageMockBucket
  mockAWSS3Client.bucket(s3ImagesBucket) returns Some(imageMockBucket)
  mockAWSS3Client.listObjects(s3ImagesBucket) returns mockObjectListing

  mockAWSS3Client.createBucket(s3ExportsBucket) returns exportMockBucket
  mockAWSS3Client.bucket(s3ExportsBucket) returns Some(exportMockBucket)

  mockAWSS3Client.generatePresignedUrl(any, any, any) returns new URL("http://example.com")
  mockAWSS3Client.generatePresignedUrl(any) returns new URL("http://example.com")

  lazy val s3Client = new S3Client {
    override lazy val s3Client = mockAWSS3Client
  }

  lazy val imageMockBucket = mock[Bucket]
  imageMockBucket.name returns s3ImagesBucket
  imageMockBucket.putAsPublicRead(anyString, any)(any) returns mock[PutObjectResult]

  lazy val exportMockBucket = mock[Bucket]
  exportMockBucket.name returns s3ExportsBucket
  exportMockBucket.put(any, any)(any) returns mock[PutObjectResult]

  lazy val importMockBucket = mock[Bucket]
  importMockBucket.name returns s3ImportsBucket
  importMockBucket.put(any, any)(any) returns mock[PutObjectResult]

  lazy val mockMessageSenderProbe = new TestProbe(testAsyncSystem)

  override lazy val messageSender: ActorRef withTag SQSMessageSender =
    mockMessageSenderProbe.ref.taggedWith[SQSMessageSender]

  lazy val mdcActorProbe = new TestProbe(testAsyncSystem)

  implicit lazy val mdcActor: ActorRef withTag BaseMdcActor =
    mdcActorProbe.ref.taggedWith[BaseMdcActor]

  override lazy val messageHandler = new SQSMessageHandler(asyncSystem, messageSender)

  override lazy val walletClient = mock[WalletClient].smart

  override lazy val ptOrderingClient = new PtOrderingStubClient

  private lazy val redisHost = Config.redisHost
  private lazy val redisPort = Config.redisPort
  override lazy val redis: ConfiguredRedis = new ConfiguredRedis(redisHost, redisPort)

  override lazy val stripeClient = new StripeStubClient
  override lazy val stripeConnectClient = new StripeConnectStubClient

  override lazy val jwkClient = new JwkStubClient
  override lazy val auth0Client = new Auth0StubClient(auth0Config, jwkClient)
}

class MockedRestApiShutdown {
  akka.testkit.TestKit.shutdownActorSystem(MockedRestApi.testAsyncSystem)
}

trait MultipleLocationFixtures extends UserFixtures {
  lazy val london = Factory
    .location(
      merchant,
      name = Some("London"),
      zoneId = Some("Europe/London"),
      overrideNow = Some(UtcTime.now),
    )
    .create

  lazy val rome = Factory
    .location(
      merchant,
      name = Some("Rome"),
      zoneId = Some("Europe/Rome"),
      overrideNow = Some(UtcTime.now.plusSeconds(5)),
    )
    .create

  lazy val locations = Seq(london, rome)
}

trait DefaultFixtures extends UserFixtures {
  lazy val london = Factory.location(merchant, zoneId = Some("Europe/London")).create

  lazy val locations = Seq(london)
}

trait UserFixtures extends BaseFixtures {
  def locations: Seq[LocationRecord]

  lazy val firstName = "John"
  lazy val lastName = "Doe"

  lazy val email = s"john-${UUID.randomUUID}@paytouch.io"
  lazy val userPin = "12345"

  lazy val user = Factory
    .user(
      merchant,
      firstName = Some(firstName),
      lastName = Some(lastName),
      password = Some(password),
      email = Some(email),
      locations = locations,
      userRole = Some(userRole),
      isOwner = Some(true),
      pin = Some(userPin),
    )
    .create

  lazy val initialSession = Factory.session(user, LoginSource.PtDashboard)
  lazy val jwtToken = Factory.valitTokenForSession(initialSession)
  lazy val authorizationHeader = Authorization(OAuth2BearerToken(jwtToken))
  lazy val dashboardAuthorizationHeader = authorizationHeader

  lazy val registerSession = Factory.session(user, LoginSource.PtRegister)
  lazy val registerJwtToken = Factory.valitTokenForSession(registerSession)
  lazy val registerAuthorizationHeader = Authorization(OAuth2BearerToken(registerJwtToken))

  lazy val userContext =
    UserContext(
      id = user.id,
      merchantId = user.merchantId,
      currency = merchant.currency,
      businessType = merchant.businessType,
      locationIds = locations.map(_.id),
      adminId = None,
      merchantSetupCompleted = false,
      source = ContextSource.PtDashboard,
      paymentProcessor = merchant.paymentProcessor,
    )
}

trait DisabledUserFixtures extends BaseFixtures {

  lazy val disabledUser =
    Factory.user(merchant, password = Some(password), userRole = Some(userRole), active = Some(false)).create

  val disabledJwtToken = Factory.createValidTokenWithSession(disabledUser)
  val disabledAuthorizationHeader = Authorization(OAuth2BearerToken(disabledJwtToken))
}

trait HasInvalidAuthorizationHeader {
  lazy val invalidAuthorizationHeader = Authorization(OAuth2BearerToken("invalidToken"))
}

trait AppTokenFixtures extends UserFixtures with HasInvalidAuthorizationHeader {
  def appToken(iss: String) =
    JsonWebToken(
      Map(
        idKey -> merchant.id.toString,
        issKey -> iss,
        iatKey -> thisInstant.getEpochSecond,
        audKey -> "",
        jtiKey -> "",
        adminKey -> "",
      ),
      JwtOrderingSecret,
    )

  lazy val orderingJwtToken = appToken("ptOrdering")

  lazy val appAuthorizationHeader = {
    user // ensure at least one owner exists
    Authorization(OAuth2BearerToken(orderingJwtToken))
  }
}

trait BaseFixtures extends JwtTokenGenerator with TestExecutionContext with HasInvalidAuthorizationHeader {
  val jwtSecret = Config.JwtSecret

  lazy val sessionDao: SessionDao = MockedRestApi.daos.sessionDao

  lazy val merchant = Factory.merchant.create
  lazy val password = s"${UUID.randomUUID}-password"
  lazy val userRole = Factory.userRole(merchant).create
  lazy val source = LoginSource.PtDashboard
  lazy val currency = merchant.currency
  lazy val defaultMenuCatalog =
    Factory.defaultMenuCatalog(merchant).create

  implicit val merchantContext = MerchantContext.extract(merchant)
}

trait AdminFixtures extends HasInvalidAuthorizationHeader {
  val adminPassword = s"${UUID.randomUUID}-admin-password"
  val admin = Factory.admin(password = Some(adminPassword)).create
  lazy val adminJwtToken = JsonWebToken(Map("uid" -> admin.id.toString), JwtSecret)
  lazy val adminAuthorizationHeader = Authorization(OAuth2BearerToken(adminJwtToken))
}
