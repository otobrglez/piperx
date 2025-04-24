package com.github.otobrglez.piperx

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object TransformApp extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  // Example bucket name
  private val bucketName: String = "my-csvs"

  override def run: IO[Unit] = S3.mkAsyncClient.use { client =>
    for
      _ <- logger.info("Transforming remote CSVs")
      // TODO: Put example here.
    yield ()
  }
