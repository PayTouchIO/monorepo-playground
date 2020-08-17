package io.paytouch.core.validators

import java.io.File
import java.util.UUID

import cats.data.Validated.Valid
import com.sksamuel.scrimage.Image
import io.paytouch.core.data.daos.{ Daos, ImageUploadDao }
import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ ImageUploadUpsertionV1, UserContext }
import io.paytouch.core.errors.{
  InvalidImageUploadAssociation,
  InvalidImageUploadIds,
  NonAccessibleImageUploadIds,
  UnparsableImage,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._
import scala.util.Try

class ImageUploadValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[ImageUploadRecord] {

  type Dao = ImageUploadDao
  type Record = ImageUploadRecord

  protected val dao = daos.imageUploadDao
  val validationErrorF = InvalidImageUploadIds(_)
  val accessErrorF = NonAccessibleImageUploadIds(_)

  def validateImageUploadUpsertionV1(
      id: UUID,
      upsertion: ImageUploadUpsertionV1,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Image]] =
    for {
      record <- validateOneById(id)
      image <- Future.successful(loadImage(upsertion.file))
    } yield Multiple.combine(record, image) { case (_, img) => img }

  private def loadImage(file: File): ErrorsOr[Image] =
    Try {
      val img = Image.fromFile(file)
      Multiple.success(img)
    } getOrElse Multiple.failure(UnparsableImage())

  def accessByIdsAndImageUploadType(
      imageMap: Map[ImageUploadType, Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Record]]] = {
    val result = imageMap.map { case (imageType, imageIds) => accessByIdsAndImageUploadType(imageIds, imageType) }
    Future.sequence(result).map { validations =>
      validations.fold(Multiple.success(Seq.empty[Record]))((a, b) => Multiple.combine(a, b)(_ ++ _))
    }
  }

  def accessByIdsAndImageUploadType(
      ids: Seq[UUID],
      imageUploadType: ImageUploadType,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Record]]] =
    accessByIds(ids).map {
      case Valid(records) if !records.forall(_.objectType == imageUploadType) =>
        val invalidImageUploadIds = records.filterNot(_.objectType == imageUploadType).map(_.id)
        Multiple.failure(InvalidImageUploadAssociation(invalidImageUploadIds, imageUploadType))
      case x => x
    }
}
