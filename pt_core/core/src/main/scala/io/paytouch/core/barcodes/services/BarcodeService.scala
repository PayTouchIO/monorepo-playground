package io.paytouch.core.barcodes.services

import java.io.{ File, FileOutputStream }
import java.util.UUID

import scala.concurrent._
import scala.jdk.CollectionConverters._

import awscala.s3.Bucket

import com.google.zxing.{ EncodeHintType, BarcodeFormat => ZxingBarcodeFormat }
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.pdf417.PDF417Writer
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core._
import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.entities.enum.BarcodeFormat
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.entities._

class BarcodeService(
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3ImagesBucket,
  )(implicit
    val ec: ExecutionContext,
  ) extends LazyLogging {

  implicit private lazy val bucket = uploadBucket

  private lazy val ImgFormat = "png"

  def generate(metadata: BarcodeMetadata)(implicit merchant: MerchantContext): Future[String] =
    checkIfExisting(metadata) flatMap {
      case Some(url) => Future.successful(url)
      case None =>
        for {
          file <- createBarcodeImgFile(metadata)
          url <- uploadToS3(metadata, file)
          _ <- Future(file.delete)
        } yield url
    }

  private def checkIfExisting(metadata: BarcodeMetadata)(implicit merchant: MerchantContext): Future[Option[String]] =
    s3Client.getUrlKey(toS3Key(metadata))

  private def createBarcodeImgFile(metadata: BarcodeMetadata)(implicit merchant: MerchantContext): Future[File] = {
    import BarcodeFormat._
    logger.info(s"[Merchant ${merchant.id}] Starting generation of barcode ${metadata.format}")
    metadata.format match {
      case PDF417  => createPDF417Barcode(metadata)
      case Code128 => createCode128Barcode(metadata)
    }
  }

  private def createPDF417Barcode(metadata: BarcodeMetadata): Future[File] =
    Future {
      val file = File.createTempFile(s"barcode-PDF417-${UUID.randomUUID}", ImgFormat)
      val writer = new PDF417Writer
      val hints = Map(EncodeHintType.MARGIN -> metadata.margin).asJava
      val matrix = writer.encode(metadata.value, ZxingBarcodeFormat.PDF_417, metadata.width, metadata.height, hints)
      MatrixToImageWriter.writeToStream(matrix, ImgFormat, new FileOutputStream(file))
      file
    }

  private def createCode128Barcode(metadata: BarcodeMetadata): Future[File] =
    Future {
      val file = File.createTempFile(s"barcode-Code128-${UUID.randomUUID}", ImgFormat)
      val writer = new Code128Writer
      val hints = Map(EncodeHintType.MARGIN -> metadata.margin).asJava
      val matrix = writer.encode(metadata.value, ZxingBarcodeFormat.CODE_128, metadata.width, metadata.height, hints)
      MatrixToImageWriter.writeToStream(matrix, ImgFormat, new FileOutputStream(file))
      file
    }

  private def uploadToS3(metadata: BarcodeMetadata, file: File)(implicit merchant: MerchantContext): Future[String] = {
    logger.info(s"[Merchant ${merchant.id}] Uploading barcode ${metadata.format} to S3")
    val key = toS3Key(metadata)
    s3Client.uploadPublicFileToBucket(key, file)
  }

  private def toS3Key(metadata: BarcodeMetadata)(implicit merchant: MerchantContext): String = {
    val encodedValueAsUUID = UUID.nameUUIDFromBytes(metadata.value.getBytes)
    val filename = s"$encodedValueAsUUID.${metadata.width}x${metadata.height}.m${metadata.margin}.$ImgFormat"
    val tokens = List(merchant.id.toString, "barcodes", metadata.format.entryName, filename)
    tokens.mkString("/")
  }

}
