import Dependencies._
import Util._

ThisBuild / organization := "io.paytouch"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.0.1-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:_",
  "-unchecked",
  "-Wunused:_",
  "-Xfatal-warnings",
  "-Ymacro-annotations"
)

lazy val `pt_ordering` =
  project
    .in(file("."))
    .settings(name := "pt_ordering")
    .settings(commonSettings: _*)
    .settings(dependencies: _*)
    .aggregate(
      core,
      delivery,
      persistence,
      server,
      client
    )

lazy val domain =
  ProjectRef(file("../domain"), "domain")

// lazy val misc =
//   ProjectRef(file("../misc"), "misc")

// lazy val protocol =
//   ProjectRef(file("../protocol"), "protocol")

// lazy val util =
//   ProjectRef(file("../util"), "util")

lazy val core =
  project
    .in(file("server/01-core"))
    .dependsOn(domain % Cctt)
    // .dependsOn(misc % Cctt)
    // .dependsOn(protocol % Cctt)
    // .dependsOn(util % Cctt)
    .settings(commonSettings: _*)

lazy val delivery =
  project
    .in(file("server/02-delivery"))
    .dependsOn(core % Cctt)
    .settings(commonSettings: _*)

lazy val persistence =
  project
    .in(file("server/02-persistence"))
    .dependsOn(core % Cctt)
    .settings(commonSettings: _*)

// works fine locally but fails on CI
// lazy val `pt_core/client` =
//   ProjectRef(file("../pt_core"), "client")

lazy val server =
  project
    .in(file("server/04-server"))
    .dependsOn(delivery % Cctt)
    .dependsOn(persistence % Cctt)
    // .dependsOn(`pt_core/client` % Cctt)
    .settings(commonSettings: _*)

lazy val client =
  project
    .in(file("client"))
    .dependsOn(delivery % Cctt)
    .settings(commonSettings: _*)

lazy val commonSettings = Seq(
  addCompilerPlugin(org.augustjune.`context-applied`),
  addCompilerPlugin(org.typelevel.`kind-projector`),
  Compile / console / scalacOptions --= Seq(
    "-Wunused:_",
    "-Xfatal-warnings"
  ),
  Test / console / scalacOptions :=
    (Compile / console / scalacOptions).value
)

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    // main dependencies
  ),
  libraryDependencies ++= Seq(
    com.github.alexarchambault.`scalacheck-shapeless_1.14`,
    org.scalacheck.scalacheck,
    org.scalatest.scalatest,
    org.scalatestplus.`scalacheck-1-14`,
    org.typelevel.`discipline-scalatest`
  ).map(_ % Test)
)
