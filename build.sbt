import Dependencies.*
val scala3Version = "3.6.4"

ThisBuild / scalaVersion := scala3Version

lazy val root = project
  .in(file("."))
  .settings(
    name         := "piperx",
    scalaVersion := scala3Version,
    libraryDependencies ++= { catsAndFriends ++ aws ++ logging ++ json ++ testingLibs },
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-explain",
      "-Yretain-trees",
      "-Xmax-inlines:100",
      "-Ximplicit-search-limit:150000",
      "-language:implicitConversions",
      "-Wunused:all"
    )
  )

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("fix", ";scalafixAll")
