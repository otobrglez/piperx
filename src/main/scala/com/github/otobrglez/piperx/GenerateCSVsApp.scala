package com.github.otobrglez.piperx

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.scalacheck.Gen
import fs2.Stream
import java.nio.file.Paths
import java.nio.file.Path as JPath
import fs2.io.file.{Files, Path}
import fs2.text

object CSV:
  private val logger = Slf4jLogger.getLogger[IO]

  def gen(maxColumns: Int = 100, maxRows: Int = 100): Gen[List[List[String]]] = for
    numberOfColumns <- Gen.chooseNum(1, maxColumns)
    columns         <- Gen.listOfN(numberOfColumns, Gen.alphaNumStr.map(s => s"col-$s"))
    numberOfRecord  <- Gen.chooseNum(1, maxRows)
    records         <- Gen.listOfN(numberOfRecord, Gen.listOfN(numberOfColumns, Gen.alphaNumStr))
  yield List(columns) ++ records

  def writeGenToFile(path: JPath, maxColumns: Int = 100, maxRows: Int = 100): IO[JPath] = for
    _ <- Stream
           .emits(gen(maxColumns, maxRows).sample.get)
           .map(_.map(s => s""""$s""""))
           .map(_.mkString(","))
           .intersperse("\n")
           .through(text.utf8.encode)
           .through(Files[IO].writeAll(Path(path.toString)))
           .compile
           .drain
    _ <- logger.info(s"Saved to $path")
  yield path

object GenerateCSVs extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  // Max number of files to generate
  private val maxNumberOfFiles   = 10
  private val maxNumberOfColumns = 100
  private val maxNumberOfRows    = 20_000

  def run: IO[Unit] = for
    _        <- logger.info("Generating CSVs")
    fileNames =
      Gen.listOfN(maxNumberOfFiles, Gen.stringOfN(40, Gen.alphaNumChar).map(s => s"data/file-$s.csv")).sample.get
    _        <-
      IO.parSequence(
        fileNames.map(fileName =>
          CSV
            .writeGenToFile(Paths.get(fileName).toAbsolutePath, maxNumberOfColumns, maxNumberOfRows)
            .flatTap(path => logger.info(s"Generated $path"))
        )
      )
  yield ()
