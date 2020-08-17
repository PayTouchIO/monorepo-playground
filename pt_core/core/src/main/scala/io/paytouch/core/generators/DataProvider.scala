package io.paytouch.core.generators

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.{ EntityRef, IdsRef, ServiceConfigurations }
import io.paytouch.core.calculations.LookupIdUtils
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.ImageUploadUpdate
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ Formatters, JsonTemplateUtils, UtcTime }
import io.paytouch.core.utils.Formatters._

trait DataProvider extends JsonTemplateUtils with LookupIdUtils {
  implicit val ec: ExecutionContext

  protected def directory: String
  protected def imageMap: Map[ImageUploadType, Int]

  private val basePath = s"/sampledata/$directory"

  def generateLocationCreations: Future[EntityRef[LocationCreation]] =
    Future {
      val data = loadAs[Seq[LocationCreation]](s"$basePath/locations.json")
      randomIds(data, "location")
    }

  def generateLocationSettings(locationsRef: IdsRef, imagesRef: IdsRef): Future[Map[UUID, LocationSettingsUpdate]] =
    Future {
      val transformations = locationsRef ++ imagesRef
      loadAs[Map[UUID, LocationSettingsUpdate]](s"$basePath/location_settings.json", transformations)
    }

  def generateTaxRates(locationsRef: IdsRef): Future[EntityRef[TaxRateCreation]] =
    Future {
      val data = loadAs[Seq[TaxRateCreation]](s"$basePath/tax_rates.json", locationsRef)
      randomIds(data, "tax-rate")
    }

  def generateDiscounts(locationsRef: IdsRef): Future[EntityRef[DiscountCreation]] =
    Future {
      val data = loadAs[Seq[DiscountCreation]](s"$basePath/discounts.json", locationsRef)
      randomIds(data, "discount")
    }

  def generateBrands: Future[EntityRef[BrandCreation]] =
    Future {
      val data = loadAs[Seq[BrandCreation]](s"$basePath/brands.json")
      randomIds(data, "brand")
    }

  def generateCategories(locationsRef: IdsRef): Future[EntityRef[SystemCategoryCreation]] =
    Future {
      val data = loadAs[Seq[SystemCategoryCreation]](s"$basePath/categories.json", locationsRef: IdsRef)
      randomIds(data, "category")
    }

  def generateSuppliers(locationsRef: IdsRef): Future[EntityRef[SupplierCreation]] =
    Future {
      val data = loadAs[Seq[SupplierCreation]](s"$basePath/suppliers.json", locationsRef)
      randomIds(data, "supplier")
    }

  def generateModifierSets(locationsRef: IdsRef, modifierOptionsRef: IdsRef): Future[EntityRef[ModifierSetCreation]] =
    Future {
      val transformations = locationsRef ++ modifierOptionsRef
      val data = loadAs[Seq[ModifierSetCreation]](s"$basePath/modifier_sets.json", transformations)
      randomIds(data, "modifier-set")
    }

  def generateProducts(
      brandsRef: IdsRef,
      categoriesRef: IdsRef,
      imagesRef: IdsRef,
      locationsRef: IdsRef,
      suppliersRef: IdsRef,
      taxRatesRef: IdsRef,
      variantsRef: IdsRef,
    ): Future[EntityRef[ProductCreation]] =
    Future {
      val transformations = brandsRef ++ categoriesRef ++ imagesRef ++ locationsRef ++
        suppliersRef ++ taxRatesRef ++ variantsRef
      val data = loadAs[Seq[ProductCreation]](s"$basePath/products.json", transformations)
      randomIds(data, "product")
    }

  def generateModifierSetProducts(
      modifierSetsRef: IdsRef,
      productsRef: IdsRef,
    ): Future[Map[UUID, ModifierSetProductsAssignment]] =
    Future {
      val transformations = modifierSetsRef ++ productsRef
      loadAs[Map[UUID, ModifierSetProductsAssignment]](s"$basePath/modifier_set_products.json", transformations)
    }

  def generateStocks(
      locationsRef: IdsRef,
      productsRef: IdsRef,
      partsRef: IdsRef,
    ): Future[Seq[StockCreation]] =
    Future {
      val transformations = locationsRef ++ productsRef ++ partsRef
      loadAs[Seq[StockCreation]](s"$basePath/stocks.json", transformations)
    }

  def generateUsers(locationsRef: IdsRef, userRolesRef: IdsRef): Future[EntityRef[UserCreation]] =
    Future {
      val transformations = locationsRef ++ userRolesRef ++ extractEmailRefs(1) ++ randomRefs(1, "word")
      val data = loadAs[Seq[UserCreation]](s"$basePath/users.json", transformations)
      randomIds(data, "user")
    }

  def generateCustomers: Future[EntityRef[CustomerMerchantUpsertion]] =
    Future {
      val data = loadAs[Seq[CustomerMerchantUpsertion]](s"$basePath/customers.json")
      randomIds(data, "customer")
    }

  def generateGroups(customersRef: IdsRef): Future[EntityRef[GroupCreation]] =
    Future {
      val data = loadAs[Seq[GroupCreation]](s"$basePath/groups.json", customersRef)
      randomIds(data, "group")
    }

  def generateShifts(
      locationsRef: IdsRef,
      userOwnersRef: IdsRef,
      usersRef: IdsRef,
    ): Future[EntityRef[ShiftCreation]] =
    Future {
      val transformations = locationsRef ++ userOwnersRef ++ usersRef ++ startEndLocalDateRef(1)
      val data = loadAs[Seq[ShiftCreation]](s"$basePath/shifts.json", transformations)
      randomIds(data, "shift")
    }

  def generateOrders(
      customersRef: IdsRef,
      locationsRef: IdsRef,
      modifierSetsRef: IdsRef,
      productsRef: IdsRef,
      taxRatesRef: IdsRef,
      userOwnersRef: IdsRef,
    ): Future[EntityRef[OrderUpsertion]] =
    Future {
      val transformations = customersRef ++ locationsRef ++ modifierSetsRef ++ productsRef ++
        taxRatesRef ++ userOwnersRef ++ dateRef(11) ++ randomRefs(1, "device") ++
        randomRefs(12, "payment-transaction") ++ randomRefs(31, "item") ++ randomRefs(44, "tax")
      val data = loadAs[Seq[OrderUpsertion]](s"$basePath/orders.json", transformations)
      randomIds(data, "order")
    }

  def generateParts(
      brandsRef: IdsRef,
      locationsRef: IdsRef,
      suppliersRef: IdsRef,
      variantsRef: IdsRef,
    ): Future[EntityRef[PartCreation]] =
    Future {
      val transformations = brandsRef ++ locationsRef ++ suppliersRef ++ variantsRef
      val data = loadAs[Seq[PartCreation]](s"$basePath/parts.json", transformations)
      randomIds(data, "part")
    }

  def generateProductParts(partsRef: IdsRef, productsRef: IdsRef): Future[Map[UUID, Seq[ProductPartAssignment]]] =
    Future {
      val transformations = partsRef ++ productsRef
      loadAs[Map[UUID, Seq[ProductPartAssignment]]](s"$basePath/product_parts.json", transformations)
    }

  def generateTimeCards(locationsRef: IdsRef, userOwnersRef: IdsRef): Future[EntityRef[TimeCardCreation]] =
    Future {
      val transformations = locationsRef ++ userOwnersRef ++ startEndDateRef(1)
      val data = loadAs[Seq[TimeCardCreation]](s"$basePath/time_cards.json", transformations)
      randomIds(data, "time-cards")
    }

  def generateTimeOffCards(usersRef: IdsRef): Future[EntityRef[TimeOffCardCreation]] =
    Future {
      val transformations = usersRef ++ startEndDateRef(1, 48)
      val data = loadAs[Seq[TimeOffCardCreation]](s"$basePath/time_off_cards.json", transformations)
      randomIds(data, "time-off-cards")
    }

  def generateImages(implicit user: UserContext): Future[EntityRef[ImageUploadUpdate]] =
    Future {
      imageMap.flatMap { case (imageType, n) => generateImagesWithType(n, imageType) }
    }

  private def generateImagesWithType(
      n: Int,
      objectType: ImageUploadType,
    )(implicit
      user: UserContext,
    ): EntityRef[ImageUploadUpdate] =
    (0 until n).map { idx =>
      val id = UUID.randomUUID
      val prefix = s"demo-${objectType.entryName}-${idx + 1}"
      val objectName = objectType.entryName.replaceAll("_", "-")
      val baseUrl = s"${ServiceConfigurations.demoImageUrl}/$directory/$objectName/$prefix"
      val imageMap: Map[String, String] = objectType
        .sizes
        .map { imageSize =>
          val fileName = imageSize.size.map(size => s"${size}x$size.png").getOrElse("original.png")
          imageSize.entryName -> s"$baseUrl/$fileName"
        }
        .toMap
      val update = ImageUploadUpdate(
        id = Some(id),
        merchantId = Some(user.merchantId),
        urls = Some(imageMap),
        fileName = Some(s"$prefix.png"),
        objectId = None,
        objectType = Some(objectType),
      )
      ref(s"$objectName-image", idx) -> id -> update
    }.toMap

  private def randomIds[T](data: Seq[T], context: String): EntityRef[T] =
    data.zipWithIndex.map { case (d, idx) => (ref(context, idx), UUID.randomUUID) -> d }.toMap

  def extractUserRoleRefs(userRoles: Seq[UserRole]): IdsRef =
    userRoles.map(userRole => ref(s"user-role", userRole.name.toLowerCase) -> userRole.id.toString).toMap

  private def extractEmailRefs(n: Int): IdsRef =
    randomLookupIds(n, "email").transform((key, id) => s"demo+$id@paytouch.com")

  def randomLookupIds(n: Int, context: String): IdsRef =
    (0 until n).map(idx => ref(context, idx) -> generateLookupId(UUID.randomUUID)).toMap

  def randomRefs(n: Int, context: String): IdsRef =
    extractIdRefs(0 until n, context)(_ => UUID.randomUUID)

  def extractIdRefs[T](seqT: Seq[T], context: String)(f: T => UUID): IdsRef =
    seqT.zipWithIndex.map { case (t, idx) => ref(context, idx) -> f(t).toString }.toMap

  private def ref(context: String, idx: Int): String = ref(context, (idx + 1).toString)

  private def ref(context: String, suffix: String): String = s"ref-$context-$suffix"

  private def startEndDateRef(n: Int, lengthInHours: Int = 8): IdsRef =
    (0 until n).flatMap { idx =>
      val days = idx + 1
      val start = UtcTime.now.minusDays(days)
      val end = start.plusHours(lengthInHours)
      Map(
        ref("date-start", idx) -> ZonedDateTimeFormatter.format(start),
        ref("date-end", idx) -> ZonedDateTimeFormatter.format(end),
      )
    }.toMap

  private def startEndLocalDateRef(n: Int, lengthInMonths: Int = 3): IdsRef =
    (0 until n).flatMap { idx =>
      val months = idx + 1
      val start = UtcTime.now.minusMonths(months)
      val end = start.plusMonths(lengthInMonths)
      Map(
        ref("local-date-start", idx) -> LocalDateFormatter.format(start),
        ref("local-date-end", idx) -> LocalDateFormatter.format(end),
      )
    }.toMap

  private def dateRef(n: Int): IdsRef =
    (0 until n).map { idx =>
      val days = idx % 7
      val date = UtcTime.now.minusDays(days)
      ref("date", idx) -> ZonedDateTimeFormatter.format(date)
    }.toMap

}
