// This file affects the project/plugins.sbt level settings on client projects

ThisBuild / organization := "io.paytouch"
ThisBuild / scalaVersion := "2.12.12" // Scala version used by sbt and not by the client projects
ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / useSuperShell := false
ThisBuild / autoStartServer := false

ThisBuild / scalacOptions ++=
  Seq("-encoding", "UTF-8") ++ Seq(
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Xfatal-warnings",
  )

lazy val `sbt-paytouch` =
  project
    .in(file("."))
    .enablePlugins(SbtPlugin) // sets sbtPlugin := true
    .settings(
      update / evictionWarningOptions := EvictionWarningOptions.empty,
      Compile / console / scalacOptions --= Seq(
        "-Wunused:_",
        "-Xfatal-warnings",
      ),
      Test / console / scalacOptions :=
        (Compile / console / scalacOptions).value,
    )

// From here the things that used to be in project/plugins.sbt:

libraryDependencies ++= Seq(
  // not sure if we need this
  "org.yaml" % "snakeyaml" % "1.26",
)

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.permutive" % "sbt-liquibase" % "1.2.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.7.0")
