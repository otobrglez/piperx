package com.github.otobrglez.piperx

import cats.effect.{IO, Resource}
import fs2.io.file.Path
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import cats.effect.{IO, IOApp, Resource}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import scala.jdk.CollectionConverters.*
import java.nio.file.{Path as JPath, Paths as JPaths}
import java.net.URI
import fs2.io.file.{Files, Path}
import fs2.Stream
import cats.syntax.all.*

import java.net.URI

object S3:
  type BucketName = String
  private val logger = Slf4jLogger.getLogger[IO]

  final case class Client(private val client: S3AsyncClient) extends AutoCloseable:
    def buckets: IO[List[Bucket]] = IO
      .fromCompletableFuture(IO(client.listBuckets()))
      .map(_.buckets().asScala.toList)

    def createBucket(bucketName: BucketName): IO[CreateBucketResponse] =
      IO.fromCompletableFuture(IO(client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())))
        .flatTap(response => logger.info(s"Created bucket: $bucketName"))

    def upload(bucketName: BucketName)(path: Path): IO[PutObjectResponse] =
      IO.fromCompletableFuture(
        IO(client.putObject(PutObjectRequest.builder().bucket(bucketName).key(path.toString).build(), path.toNioPath))
      ).flatTap(response => logger.info(s"Uploaded $path"))

    def close(): Unit = client.close()

  def mkAsyncClient: Resource[IO, Client] =
    Resource
      .fromAutoCloseable(IO {
        val builder     = S3AsyncClient.crtBuilder()
        // This is to use localstack with Docker
        val endpointURI = new URI("http://127.0.0.1:4566")
        builder.endpointOverride(endpointURI)
        builder.build()
      }.map(Client(_)))
      .evalTap(client => logger.info(s"Created"))
