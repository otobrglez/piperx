package com.github.otobrglez.piperx

import cats.effect.{IO, IOApp, Resource}
import com.github.otobrglez.piperx.S3.BucketName
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.s3.model.*

import java.nio.file.{Path as JPath, Paths as JPaths}
import scala.jdk.CollectionConverters.*

final case class CSVUploader(
  private val client: S3.Client,
  private val bucketName: S3.BucketName,
  private val rootPath: JPath
):
  private val logger = Slf4jLogger.getLogger[IO]

  def uploadStream: Stream[IO, PutObjectResponse] = for
    _        <- Stream.eval(logger.info(s"Uploading from path ${rootPath.toAbsolutePath} to ${bucketName}"))
    response <- Files[IO]
                  .list(Path.fromNioPath(rootPath))
                  .filter(_.extName == ".csv")
                  .parEvalMapUnorderedUnbounded(client.upload(bucketName))
  yield response

  def upload: IO[Unit] = uploadStream.compile.drain

object CSVUploader:
  private val logger = Slf4jLogger.getLogger[IO]

  def make(client: S3.Client, bucketName: BucketName, rootPath: JPath): Resource[IO, CSVUploader] =
    Resource.make(IO(new CSVUploader(client, bucketName, rootPath)))(uploader => IO.unit <* logger.info("Closed"))

object UploadApp extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  // Example bucket name
  private val bucketName: String = "my-csvs"

  // Path where CSVs live
  private val csvsPath: JPath = JPaths.get("./data/")

  override def run: IO[Unit] =
    S3.mkAsyncClient
      .flatMap(c => CSVUploader.make(c, bucketName, csvsPath).map(up => c -> up))
      .use { (client, uploader) =>
        for
          _       <- client.createBucket(bucketName).void
          buckets <- client.buckets.flatTap(buckets => logger.info(s"Buckets: ${buckets.mkString(", ")}"))
          _       <- uploader.upload
        yield ()
      }
