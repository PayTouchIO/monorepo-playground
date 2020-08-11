ThisBuild / scalaVersion := "2.12.12"
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

update / evictionWarningOptions := EvictionWarningOptions.empty

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
