package com.github.otobrglez.piperx

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2.{Pipe, Stream}

object CSVParser:

  def csvLines(columnWrap: String = "\"", emitHeader: Boolean = true): Pipe[IO, String, List[String]] =
    _.zipWithIndex.flatMap {
      case line -> index if index == 0 && !emitHeader => Stream.emits(Nil)
      case line -> _                                  => Stream.emit(line.split(",").toList)
    }.map(_.map(_.replaceAll(columnWrap, "")))

object TransformApp extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  // Example bucket name
  private val bucketName: String = "my-csvs-x"

  private def computeAndStreamToS3(client: S3.Client, key: String): Pipe[IO, List[String], Unit] = stream =>
    val computing = for line <- stream.zipWithIndex.map {
                                  case tpl @ (line, index) if index == 0 => line
                                  case line -> _                         => line.map(_.length.toString)
                                }
    yield line.map(c => s"""\"$c\"""").mkString(",")

    computing.through(client.streamLinesToObject(key))

  private def processing(client: S3.Client) = for
    // Stream of all objects (*.csv) in the bucket
    s3Object <- client.objectsStream().filter(_.key().endsWith(".csv"))

    // Compute new keys
    (key, statsKey) = s3Object.key -> s3Object.key.replace(".csv", "-stats.txt").replaceFirst("\\./", "")

    // Be friendly,..
    _ <- Stream.eval(logger.info(s"Processing $key"))

    // Stream each line of bucket parsed as CSV
    line <- client
              .objectLinesStream(key)
              .take(3)
              .through(CSVParser.csvLines(emitHeader = true))
              .evalTap(line => logger.info(s"Line: $line"))
              .broadcastThrough(
                computeAndStreamToS3(client, statsKey)

                // Or more...
                // computeAndStreamToS3(client, statsKey.replace(".txt", "-stats-2.txt"))
              )
  yield line

  override def run: IO[Unit] = S3.mkAsyncClient(bucketName).use { client =>
    for
      _   <- logger.info("Transforming remote CSVs")
      out <- processing(client).compile.drain
      _   <- logger.info(s"Done w/ ${out}")
    yield ()
  }
