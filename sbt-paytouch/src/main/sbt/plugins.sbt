// You are looking at a symbolic link to sbt-paytouch/src/main/sbt/plugins.sbt

// In the future we will use a binary dependency on the plugin like so:
// addSbtPlugin("paytouch" % "sbt-paytouch" % "0.0.1-SNAPSHOT")

// but for now we will use a source code dependency to ease our CI setup (ie no need to publish the plugin):
lazy val root =
  project
    .in(file("."))
    .dependsOn(`sbt-paytouch`)

lazy val `sbt-paytouch` =
  ProjectRef(file("../../sbt-paytouch"), "sbt-paytouch")

update / evictionWarningOptions := EvictionWarningOptions.empty
