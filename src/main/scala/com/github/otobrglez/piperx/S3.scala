package com.github.otobrglez.piperx

import cats.effect.IO.fromCompletableFuture
import cats.effect.{IO, Resource}
import fs2.{Pipe, Stream}
import fs2.io.file.Path
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.auth.credentials.{AwsCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import fs2.interop.reactivestreams.*
import cats.effect.kernel.Async

import java.net.URI
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

object S3:
  type BucketName = String
  private val logger = Slf4jLogger.getLogger[IO]

  final case class Client(private val client: S3AsyncClient, private val bucketName: BucketName) extends AutoCloseable:
    def buckets: IO[List[Bucket]] = fromCompletableFuture(IO(client.listBuckets()))
      .map(_.buckets().asScala.toList)

    def createBucket: IO[CreateBucketResponse] =
      fromCompletableFuture(IO(client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())))
        .flatTap(response => logger.info(s"Created bucket: $bucketName"))

    def uploadFile(path: Path): IO[PutObjectResponse] =
      fromCompletableFuture(
        IO(client.putObject(PutObjectRequest.builder().bucket(bucketName).key(path.toString).build(), path.toNioPath))
      ).flatTap(response => logger.info(s"Uploaded $path"))

    def objects(maybePrefix: Option[String] = None): IO[List[S3Object]] =
      val request = client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build())
      fromCompletableFuture(IO(request.toCompletableFuture)).map(_.contents().asScala.toList)

    def objectsStream(maybePrefix: Option[String] = None): Stream[IO, S3Object] =
      Stream.eval(objects(maybePrefix)).flatMap(Stream.emits)

    def objectBytes(key: String): IO[String] =
      val request  = GetObjectRequest.builder().bucket(bucketName).key(key).build()
      val response = client.getObject(request, AsyncResponseTransformer.toBytes)
      fromCompletableFuture(IO(response)).map(_.asByteArray).map { response =>
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response)).toString
      }

    def objectBytesStream(key: String, chunkSize: Int = 1000): Stream[IO, Byte] = for
      request <- Stream.emit(GetObjectRequest.builder().bucket(bucketName).key(key).build())
      response = fromCompletableFuture(IO(client.getObject(request, AsyncResponseTransformer.toPublisher)))
      byte    <-
        Stream
          .eval(response)
          .flatMap(fromPublisher[IO, ByteBuffer](_, chunkSize))
          .flatMap(bb => Stream.emits(bb.array()))
    yield byte

    def objectLinesStream(key: String, chunkSize: Int = 1000): Stream[IO, String] =
      objectBytesStream(key, chunkSize)
        .through(fs2.text.utf8.decode)
        .through(fs2.text.lines)

    def streamLinesToObject(key: String): Pipe[IO, String, Unit] = stream =>
      val bytes = stream
        .intersperse("\n")
        .through(fs2.text.utf8.encode)
        .chunks
        .map(chunk => ByteBuffer.wrap(chunk.toArray))

      Stream.resource:
        for
          _         <- Resource.eval(logger.info(s"Starting to write to S3 with key: $key"))
          publisher <- bytes.toUnicastPublisher
          request    = PutObjectRequest.builder().bucket(bucketName).key(key).build()
          body       = AsyncRequestBody.fromPublisher(publisher)
          response  <- Resource.eval(fromCompletableFuture(IO(client.putObject(request, body))))
          _         <- Resource.eval(logger.info(s"Finished writing to S3 with key: $key"))
        yield ()

    def close(): Unit = client.close()

  def mkAsyncClient(bucketName: BucketName): Resource[IO, Client] =
    Resource
      .fromAutoCloseable(IO {
        val builder = S3AsyncClient.crtBuilder()

        // This is to use localstack with Docker otherwise remove this.
        val endpointURI = new URI("http://127.0.0.1:4566")

        builder.credentialsProvider(
          // Some more sophisticated chain can be used here.
          StaticCredentialsProvider.create(new AwsCredentials:
            def accessKeyId()     = "test"
            def secretAccessKey() = "test"
          )
        )
        builder.endpointOverride(endpointURI)
        builder.build()
      }.map(Client(_, bucketName)))
      .evalTap(_ => logger.info(s"Created"))
