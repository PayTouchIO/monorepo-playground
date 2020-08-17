package io.paytouch.seeds

import com.typesafe.config.ConfigFactory
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ImageUploadType

import scala.concurrent._
import scala.util.Random

object ProductImageUploadSeeds extends Seeds {
  import ProductImagesProvider._

  lazy val imageUploadDao = daos.imageUploadDao

  def load(products: Seq[ArticleRecord])(implicit user: UserRecord): Future[Seq[ImageUploadRecord]] = {

    val productsWithImages = (products.size * 0.80).toInt

    val imageUploads = products.random(productsWithImages).flatMap { product =>
      val imagesPerProduct = 1 + Random.nextInt(3)
      productImageMaps.random(imagesPerProduct).map { imageMap =>
        ImageUploadUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          urls = Some(imageMap),
          fileName = Some(s"$randomWord.png"),
          objectId = Some(product.id),
          objectType = Some(ImageUploadType.Product),
        )
      }
    }

    imageUploadDao.bulkUpsert(imageUploads).extractRecords
  }
}

object ProductImagesProvider {

  private val config = ConfigFactory.load("application.conf")

  private val BaseUrl = config.getString("productImages.baseUrl")
  private val BaseId = config.getString("productImages.baseId")
  private val Total = config.getInt("productImages.total")

  private val ids = (1 to Total).map(idx => "%s%02d".format(BaseId, idx))

  private def generateImageMap(id: String): Map[String, String] =
    ImageUploadType
      .Product
      .sizes
      .map { imageSize =>
        val fileName = imageSize.size.map(size => s"${size}x$size.png").getOrElse("original.png")
        imageSize.entryName -> s"$BaseUrl/$id/$fileName"
      }
      .toMap

  val productImageMaps: Seq[Map[String, String]] = ids.map(generateImageMap)
}
