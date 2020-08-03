// In the future we will use a binary dependency on the plugin like so:
// addSbtPlugin("io.paytouch" % "sbt-paytouch" % "0.0.1-SNAPSHOT")

// But for now we will use a source code dependency to ease our CI setup (ie no need to publish the plugin):
lazy val root =
  project
    .in(file("."))
    .dependsOn(`sbt-paytouch`)

lazy val `sbt-paytouch` =
  ProjectRef(file("../../sbt-paytouch"), "sbt-paytouch")
