package io.paytouch.core.resources

import java.io.File
import java.util.UUID

import scala.concurrent._
import scala.util._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.FileIO

import cats.data._

import io.paytouch.core._
import io.paytouch.core.errors._
import io.paytouch.core.json.serializers.CustomUnmarshallers
import io.paytouch.core.ServiceConfigurations._
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.utils.CorsSupport

trait FormDataResource extends Directives with Authentication with CorsSupport with CustomUnmarshallers {
  def saveCSV(id: UUID): Directive1[File] =
    saveFile(fieldName = "csv", filePrefix = s"import.$id", fileExtension = Some("csv")) map { case (_, file) => file }

  def saveImage(id: UUID): Directive1[(FileInfo, File)] =
    saveFile(fieldName = "img", filePrefix = s"image_upload.$id")

  private def saveFile(
      fieldName: String,
      filePrefix: String,
      fileExtension: Option[String] = None,
    ): Directive1[(FileInfo, File)] =
    extractRequestContext.flatMap { ctx =>
      import ctx.{ executionContext, materializer }

      fileUpload(fieldName).flatMap {
        case (fileInfo, bytes) =>
          val extension =
            fileExtension.getOrElse(fileInfo.fileName.split('.').last)
          val fileName = s"$filePrefix.${UtcTime.thisInstant.getEpochSecond}"
          val uploadDirectory = new File(uploadFolder)
          uploadDirectory.mkdirs()
          val destination = File.createTempFile(fileName, s".$extension", uploadDirectory)
          val uploadedFile: Future[File] = bytes.runWith(FileIO.toPath(destination.toPath)).map(_ => destination)

          onComplete[File](uploadedFile).flatMap {
            case Success(uploaded) =>
              provide(fileInfo, uploaded)

            case Failure(ex) =>
              destination.delete()
              failWith(ex)
          }
      }
    }

  def completeValidatedData[T](vdT: ErrorsOr[T]) = {
    import Validated._
    import StatusCodes._

    def toPrettyErrors(nel: NonEmptyList[Error]): String =
      nel.toList.mkString(",\n")

    vdT match {
      case Valid(_) =>
        complete(Created, "")

      case Invalid(i) if i.exists(_.isInstanceOf[BadRequest]) =>
        respondWithHeader(ValidationHeader(i)) {
          complete(BadRequest, toPrettyErrors(i))
        }

      case Invalid(i) =>
        respondWithHeader(ValidationHeader(i)) {
          complete(NotFound, toPrettyErrors(i))
        }
    }
  }
}
