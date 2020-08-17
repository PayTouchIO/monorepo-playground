package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._
import scala.reflect.ClassTag
import scala.util.{ Failure, Success }

import akka.event.Logging

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ ImageUploadRecord, ImageUploadUpdate, MerchantRecord }
import io.paytouch.core.data.model.enums.BusinessType.{ QSR, Restaurant, Retail }
import io.paytouch.core.data.model.enums.MerchantMode._
import io.paytouch.core.entities._
import io.paytouch.core.expansions.UserRoleExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.generators.{ DataProvider, RestaurantDataProvider, RetailDataProvider }
import io.paytouch.core.IdsRef
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils._

import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.MerchantValidator

class SampleDataService(
    val adminReportService: AdminReportService,
    val brandService: BrandService,
    val systemCategoryService: SystemCategoryService,
    val customerMerchantService: CustomerMerchantService,
    val discountService: DiscountService,
    val groupService: GroupService,
    val locationService: LocationService,
    val modifierSetService: ModifierSetService,
    val orderSyncService: OrderSyncService,
    val partService: PartService,
    val productPartService: ProductPartService,
    val productService: ProductService,
    val shiftService: ShiftService,
    val stockService: StockService,
    val supplierService: SupplierService,
    val taxRateService: TaxRateService,
    val timeCardService: TimeCardService,
    val timeOffCardService: TimeOffCardService,
    val userRoleService: UserRoleService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LazyLogging {
  val merchantDao = daos.merchantDao
  protected val validator = new MerchantValidator

  def load(merchant: MerchantRecord)(implicit user: UserContext): Future[ErrorsOr[Int]] = {
    val merchantId = merchant.id
    val futT = for {
      _ <- merchantDao.markLoadingAsInProgress(merchantId)
      dataCount <- loadData(merchant)
      _ <- adminReportService.triggerUpdateReports(AdminReportFilters(merchantId))
    } yield dataCount
    futT.onComplete {
      case Success(Valid(_)) => merchantDao.markLoadingAsSuccessful(merchantId)
      case error =>
        error match {
          case Success(value) => logger.error(s"[Merchant $merchantId] sample data failed: $value")
          case Failure(ex) =>
            logger.error(s"[Merchant $merchantId] sample data failed: $ex. , ${Logging.stackTraceFor(ex)}")
        }
        merchantDao.markLoadingAsFailed(merchantId)
    }
    futT
  }

  private def loadData(merchant: MerchantRecord)(implicit user: UserContext): Future[ErrorsOr[Int]] =
    merchant.mode match {
      case Demo       => loadDemoData(merchant)(user)
      case Production => Future.successful(Multiple.success(0))
    }

  implicit private def toIdsRef(validRef: ErrorsOr[IdsRef]): IdsRef = validRef.getOrElse(Map.empty)

  private def selectDataProvider(merchant: MerchantRecord): DataProvider =
    merchant.businessType match {
      case QSR        => new RestaurantDataProvider
      case Retail     => new RetailDataProvider
      case Restaurant => new RestaurantDataProvider
    }

  private def loadDemoData(merchant: MerchantRecord)(user: UserContext): Future[ErrorsOr[Int]] = {
    implicit val dataProvider = selectDataProvider(merchant)
    loadDemoLocations(dataProvider, user).flatMap {
      case i @ Invalid(_) => Future.successful(i)
      case Valid(locationsRef) =>
        val allLocationIds = user.locationIds ++ locationsRef.values.map(UUID.fromString)
        implicit val userWithLocations = user.copy(locationIds = allLocationIds)
        for {
          imagesRef <- loadDemoImages
          locationSettingsRef <- loadDemoLocationSettings(locationsRef, imagesRef)
          taxRatesRef <- loadDemoTaxRates(locationsRef)
          discountsRef <- loadDemoDiscounts(locationsRef)
          brandsRef <- loadDemoBrands
          categoriesRef <- loadDemoCategories(locationsRef)
          suppliersRef <- loadDemoSuppliers(locationsRef)
          modifierSetsRef <- loadDemoModifierSets(locationsRef)
          productsRef <- loadDemoProducts(brandsRef, categoriesRef, imagesRef, locationsRef, suppliersRef, taxRatesRef)
          modifierSetProductsRef <- loadDemoModifierSetProducts(modifierSetsRef, productsRef)
          partsRef <- loadDemoParts(brandsRef, locationsRef, suppliersRef)
          productPartsRef <- loadDemoProductParts(partsRef, productsRef)
          stocksRef <- loadDemoStocks(locationsRef, productsRef, partsRef)
          userRolesRef <- loadDemoUserRoles
          usersRef <- loadDemoUsers(locationsRef, userRolesRef)
          customersRef <- loadDemoCustomers
          groupsRef <- loadDemoGroups(customersRef)
          ordersRef <- loadDemoOrders(customersRef, locationsRef, modifierSetsRef, productsRef, taxRatesRef)
          shiftsRef <- loadDemoShifts(locationsRef, usersRef)
          timeCardsRef <- loadDemoTimeCards(locationsRef)
          timeOffCardsRef <- loadDemoTimeOffCards(usersRef)
        } yield Multiple.combine(
          locationSettingsRef,
          taxRatesRef,
          discountsRef,
          brandsRef,
          categoriesRef,
          suppliersRef,
          modifierSetsRef,
          productsRef,
          partsRef,
          productPartsRef,
          modifierSetProductsRef,
          stocksRef,
          userRolesRef,
          usersRef,
          customersRef,
          groupsRef,
          ordersRef,
          shiftsRef,
          timeCardsRef,
          timeOffCardsRef,
        ) { case data => data.productIterator.size }
    }
  }

  private def loadDemoLocations(implicit provider: DataProvider, user: UserContext): Future[ErrorsOr[IdsRef]] =
    saveData[LocationCreation, Location](provider.generateLocationCreations, locationService.create, _.id)

  private def loadDemoLocationSettings(
      locationsRef: IdsRef,
      imagesRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    def creatorF(id: UUID, e: LocationSettingsUpdate) = locationService.updateSettings(id, e).mapNested(_ => (): Unit)
    saveAssignmentData(provider.generateLocationSettings(locationsRef, imagesRef), creatorF)
  }

  private def loadDemoImages(implicit provider: DataProvider, user: UserContext): Future[ErrorsOr[IdsRef]] = {
    def creatorF(id: UUID, e: ImageUploadUpdate) = daos.imageUploadDao.upsert(e).map(Valid(_))
    saveData[ImageUploadUpdate, ImageUploadRecord](provider.generateImages, creatorF, _.id)
  }

  private def loadDemoTaxRates(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[TaxRateCreation, TaxRate](provider.generateTaxRates(locationsRef), taxRateService.create, _.id)

  private def loadDemoDiscounts(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[DiscountCreation, Discount](provider.generateDiscounts(locationsRef), discountService.create, _.id)

  private def loadDemoBrands(implicit provider: DataProvider, user: UserContext): Future[ErrorsOr[IdsRef]] =
    saveData[BrandCreation, Brand](provider.generateBrands, brandService.create, _.id)

  private def loadDemoCategories(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[SystemCategoryCreation, Category](
      provider.generateCategories(locationsRef),
      systemCategoryService.create,
      _.id,
    )

  private def loadDemoSuppliers(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[SupplierCreation, Supplier](provider.generateSuppliers(locationsRef), supplierService.create, _.id)

  private def loadDemoModifierSets(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    val modifierOptionsRef = provider.randomRefs(3, "modifier-option")
    saveData[ModifierSetCreation, ModifierSet](
      provider.generateModifierSets(locationsRef, modifierOptionsRef),
      modifierSetService.create,
      _.id,
      modifierOptionsRef,
    )
  }

  private def loadDemoProducts(
      brandsRef: IdsRef,
      categoriesRef: IdsRef,
      imagesRef: IdsRef,
      locationsRef: IdsRef,
      suppliersRef: IdsRef,
      taxRatesRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    val variantsRef = provider.randomRefs(11, "variant-product") ++
      provider.randomRefs(4, "variant-type") ++ provider.randomRefs(11, "variant-option")
    val data =
      provider.generateProducts(
        brandsRef,
        categoriesRef,
        imagesRef,
        locationsRef,
        suppliersRef,
        taxRatesRef,
        variantsRef,
      )
    saveData[ProductCreation, Product](data, productService.create, _.id, variantsRef)
  }

  private def loadDemoStocks(
      locationsRef: IdsRef,
      productsRef: IdsRef,
      partsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    bulkSaveData(provider.generateStocks(locationsRef, productsRef, partsRef), stockService.bulkCreate)

  private def loadDemoModifierSetProducts(
      modifierSetsRef: IdsRef,
      productsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveAssignmentData(
      provider.generateModifierSetProducts(modifierSetsRef, productsRef),
      modifierSetService.assignProducts,
    )

  private def loadDemoUserRoles(implicit provider: DataProvider, user: UserContext): Future[ErrorsOr[IdsRef]] = {
    implicit val pagination = Pagination(page = 1, perPage = 100)
    userRoleService.findAll(NoFilters())(UserRoleExpansions.empty).map {
      case (entities, _) => Multiple.success(provider.extractUserRoleRefs(entities))
    }
  }

  private def loadDemoUsers(
      locationsRef: IdsRef,
      userRolesRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[UserCreation, User](provider.generateUsers(locationsRef, userRolesRef), userService.create, _.id)

  private def loadDemoCustomers(implicit provider: DataProvider, user: UserContext): Future[ErrorsOr[IdsRef]] = {
    def creatorF(id: UUID, c: CustomerMerchantUpsertion) = customerMerchantService.create(c)
    saveData[CustomerMerchantUpsertion, CustomerMerchant](provider.generateCustomers, creatorF, _.id)
  }

  private def loadDemoGroups(
      customersRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[GroupCreation, Group](provider.generateGroups(customersRef), groupService.create, _.id)

  private def loadDemoOrders(
      customersRef: IdsRef,
      locationsRef: IdsRef,
      modifierSetsRef: IdsRef,
      productsRef: IdsRef,
      taxRatesRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    val upsertionsRefR =
      provider.generateOrders(customersRef, locationsRef, modifierSetsRef, productsRef, taxRatesRef, userOwnersRef)
    for {
      result <- saveData[OrderUpsertion, Order](upsertionsRefR, orderSyncService.syncById, _.id)
      upsertionsRef <- upsertionsRefR
      _ <- orderSyncService.ensureOrderNumberInChronologicalOrder(upsertionsRef)
    } yield result
  }

  private def loadDemoParts(
      brandsRef: IdsRef,
      locationsRef: IdsRef,
      suppliersRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    val variantsRef = provider.randomRefs(6, "part-variant-product") ++
      provider.randomRefs(2, "part-variant-type") ++ provider.randomRefs(6, "part-variant-option")
    val data = provider.generateParts(brandsRef, locationsRef, suppliersRef, variantsRef)
    saveData[PartCreation, Product](data, partService.create, _.id, variantsRef)
  }

  private def loadDemoProductParts(
      partsRef: IdsRef,
      productsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveAssignmentData(
      provider.generateProductParts(partsRef, productsRef),
      productPartService.assignProductParts,
      Some("Seq[ProductPartAssignment]"),
    )

  private def loadDemoShifts(
      locationsRef: IdsRef,
      usersRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[ShiftCreation, Shift](
      provider.generateShifts(locationsRef, userOwnersRef, usersRef),
      shiftService.create,
      _.id,
    )

  private def loadDemoTimeCards(
      locationsRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[TimeCardCreation, TimeCard](
      provider.generateTimeCards(locationsRef, userOwnersRef),
      timeCardService.create,
      _.id,
    )

  private def loadDemoTimeOffCards(
      usersRef: IdsRef,
    )(implicit
      provider: DataProvider,
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    saveData[TimeOffCardCreation, TimeOffCard](provider.generateTimeOffCards(usersRef), timeOffCardService.create, _.id)

  private def saveAssignmentData[Assignment](
      providerF: => Future[Map[UUID, Assignment]],
      creatorF: (UUID, Assignment) => Future[ErrorsOr[Unit]],
      context: Option[String] = None,
    )(implicit
      ct: ClassTag[Assignment],
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] = {
    def pF = providerF.map(_.map { case (k, v) => "N/A" -> k -> v })
    def cF(id: UUID, e: Assignment) = creatorF(id, e).mapNested(r => (ResultType.Created, r))
    def idExtractor(x: Any) = UUID.randomUUID
    saveData(pF, cF, idExtractor, context = context)
  }

  private def bulkSaveData[Creation, Entity](
      providerF: => Future[Seq[Creation]],
      creatorF: Seq[Creation] => Future[ErrorsOr[Result[Seq[Entity]]]],
    )(implicit
      ct: ClassTag[Creation],
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    providerF.flatMap { creations =>
      val className = ct.runtimeClass.getSimpleName
      logger.info(s"[Sample Data Merchant ${user.merchantId}] loading ${creations.size} $className")
      creatorF(creations).map(_.map { case (_, entity) => entity }).mapNested(_ => Map.empty)
    }

  private def saveData[Creation, Entity](
      providerF: => Future[Map[(String, UUID), Creation]],
      creatorF: (UUID, Creation) => Future[ErrorsOr[Result[Entity]]],
      idExtractor: Entity => UUID,
      extraTransformations: IdsRef = Map.empty,
      context: Option[String] = None,
    )(implicit
      ct: ClassTag[Creation],
      user: UserContext,
    ): Future[ErrorsOr[IdsRef]] =
    providerF.flatMap { creationEntries =>
      val className = context.getOrElse(ct.runtimeClass.getSimpleName)
      logger.info(s"[Sample Data Merchant ${user.merchantId}] loading ${creationEntries.keySet.size} $className")
      val results = creationEntries.map {
        case ((alias, id), creation) =>
          creatorF(id, creation).map {
            _.map {
              case (_, entity) =>
                (alias, idExtractor(entity).toString)
            }
          }
      }
      Future.sequence(results).map(_.toList.sequence).mapNested(_.toMap ++ extraTransformations)
    }

  private def userOwnersRef(implicit provider: DataProvider, user: UserContext) =
    provider.extractIdRefs(Seq(user), "user-owner")(_.id)
}
