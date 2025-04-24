import sbt.*

import scala.collection.JavaConverters.*

object Dependencies {
  type Version = String
  type Modules = Seq[ModuleID]

  object Versions {
    val log4cats: Version      = "2.7.0"
    val scalaTest: Version     = "3.2.19"
    val doobie: Version        = "1.0.0-RC9"
    val sentry: Version        = "8.9.0"
    val sentryLogback: Version = sentry
    val sentryAgent: Version   = sentry
    val ical4j: Version        = "4.1.1"
    val quartz: Version        = "2.5.0"
    val zio: Version           = "2.1.17"
    val zioLogging: Version    = "2.5.0"
    val zioHttp: Version       = "3.2.0"
    val zioConfig: Version     = "4.0.4"
    val zioMetrics: Version    = "2.3.1"
    val postgresql: Version    = "42.7.5"
    val flyway: Version        = "11.7.2"
    val circe: Version         = "0.14.13"
  }

  lazy val catsAndFriends: Modules = Seq(
    "org.typelevel" %% "cats-core"   % "2.13.0",
    "org.typelevel" %% "cats-effect" % "3.6.1"
  ) ++ Seq(
    "co.fs2" %% "fs2-core",
    "co.fs2" %% "fs2-io",
    "co.fs2" %% "fs2-reactive-streams"
  ).map(_ % "3.12.0")

  lazy val aws: Modules = Seq(
    "software.amazon.awssdk"     % "s3"             % "2.31.28",
    "software.amazon.awssdk"     % "secretsmanager" % "2.31.28",
    "software.amazon.awssdk.crt" % "aws-crt"        % "0.38.1"
  )

  lazy val logging: Modules = Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.18"
  ) ++ Seq(
    "org.typelevel" %% "log4cats-core",
    "org.typelevel" %% "log4cats-slf4j"
  ).map(_ % Versions.log4cats)

  val testingLibs: Modules = Seq(
    "org.scalatest" %% "scalatest",
    "org.scalatest" %% "scalatest-flatspec"
  ).map(_ % "3.2.19") ++ Seq(
    "org.scalacheck" %% "scalacheck" % "1.18.1"
  )

  lazy val json: Modules = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % Versions.circe)

  lazy val projectResolvers: Seq[MavenRepository] = Seq(
    // Resolver.sonatypeOssRepos("snapshots"),
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
    "Java.net Maven2 Repository" at "https://download.java.net/maven/2/",
    "Artima Maven Repository" at "https://repo.artima.com/releases"
  )
}
