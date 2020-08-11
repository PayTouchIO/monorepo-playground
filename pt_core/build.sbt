addCommandAlias("root", "project pt_core")
addCommandAlias("loadSeeds", "seeds/run")

lazy val `pt_core` =
  project
    .in(file("."))
    .aggregate(core)

lazy val core =
  project
    .in(file("core"))
    .dependsOn(domain % Cctt)
