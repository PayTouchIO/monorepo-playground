lazy val `pt_core` =
  project
    .in(file("."))
    .settings(name := "pt_core")
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

lazy val delivery =
  project
    .in(file("server/02-delivery"))
    .dependsOn(core % Cctt)

lazy val persistence =
  project
    .in(file("server/02-persistence"))
    .dependsOn(core % Cctt)

lazy val server =
  project
    .in(file("server/04-server"))
    .dependsOn(delivery % Cctt)
    .dependsOn(persistence % Cctt)

lazy val client =
  project
    .in(file("client"))
    .dependsOn(delivery % Cctt)
