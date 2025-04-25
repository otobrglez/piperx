package com.github.otobrglez.piperx

import cats.effect.{IO, IOApp, Resource}
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.s3.model.*

import java.nio.file.{Path as JPath, Paths as JPaths}
import scala.jdk.CollectionConverters.*

final case class CSVUploader(
  private val client: S3.Client,
  private val rootPath: JPath
):
  private val logger = Slf4jLogger.getLogger[IO]

  def uploadStream: Stream[IO, PutObjectResponse] = for
    _        <- Stream.eval(logger.info(s"Uploading from path ${rootPath.toAbsolutePath}"))
    response <- Files[IO]
                  .list(Path.fromNioPath(rootPath))
                  .filter(_.extName == ".csv")
                  .parEvalMapUnorderedUnbounded(client.uploadFile)
  yield response

  def upload: IO[Unit] = uploadStream.compile.drain

object CSVUploader:
  private val logger = Slf4jLogger.getLogger[IO]

  def make(client: S3.Client, rootPath: JPath): Resource[IO, CSVUploader] =
    Resource.make(IO(new CSVUploader(client, rootPath)))(uploader => IO.unit <* logger.info("Closed"))

object UploadApp extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  // Example bucket name
  private val bucketName: String = "my-csvs-x"

  // Path where CSVs live
  private val csvsPath: JPath = JPaths.get("./data/")

  override def run: IO[Unit] =
    S3.mkAsyncClient(bucketName)
      .flatMap(c => CSVUploader.make(c, csvsPath).map(up => c -> up))
      .use { (client, uploader) =>
        for
          _ <-
            client.createBucket.handleErrorWith: error =>
              logger.warn(s"Failed to create bucket: ${error.getMessage}")

          _ <- client.buckets.flatTap(buckets => logger.info(s"Buckets: ${buckets.mkString(", ")}"))
          _ <- uploader.upload
        yield ()
      }
