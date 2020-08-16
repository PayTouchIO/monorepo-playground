package io.paytouch
package sbtplugin

import sbt.{ io => _, _ }
import sbt.Keys._
import sbt.nio.Keys._

import Util._

object Paytouch extends AutoPlugin {
  // Everything inside of this object can be used without imports in client projects.
  object autoImport extends Dependencies.Binary with Dependencies.Source {
    val Cctt = sbtplugin.Util.Cctt

    val assemblySettings = Paytouch.assemblySettings
    val dockerSettings = Paytouch.dockerSettings _
    val liquibaseSettings = Paytouch.liquibaseSettings
    val testCleanupSettings = Paytouch.testCleanupSettings
  }

  import autoImport._

  // The plugin will be automatically enabled by the clients (I think).
  // To disable use project.disablePlugins(Paytouch).
  override lazy val trigger = allRequirements

  // An sbt plugin can depend on other plugins.
  override lazy val requires = plugins.JvmPlugin

  // Global / settings
  override lazy val globalSettings: Seq[Setting[_]] = {
    import bloop.integrations.sbt._
    Seq(
      BloopKeys.bloopAggregateSourceDependencies := true,
      onChangedBuildSource := ReloadOnSourceChanges,
      cancelable := sys.env.getOrElse("SBT_CANCELABLE", "false").equalsIgnoreCase("true"),
      concurrentRestrictions ++= {
        val isCircleCi =
          sys.env.get("CIRCLECI").map(_.toBoolean)

        if (isCircleCi.contains(true))
          Seq(Tags.limitAll(1))
        else
          Seq.empty
      },
    )
  }

  // ThisBuild / settings
  override lazy val buildSettings: Seq[Setting[_]] =
    Seq(
      organization := "paytouch", // this should be io.paytouch or com.paytouch
      scalaVersion := "2.13.3",
      autoStartServer := false,
      includePluginResolvers := true,
      // turbo := true, // todo verify if we can leave it as true (it was causing issues)
      classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat, // todo verify if necessary
      watchBeforeCommand := Watch.clearScreen,
      watchTriggeredMessage := Watch.clearScreenOnTrigger,
      watchForceTriggerOnAnyChange := true,
      shellPrompt := { state =>
        s"${prompt(projectName(state))}> "
      },
      watchStartMessage := {
        case (iteration, ProjectRef(build, projectName), commands) =>
          Some {
            s"""|~${commands.map(styled).mkString(";")}
                |Monitoring source files for ${prompt(projectName)}...""".stripMargin
          }
      },
      update / evictionWarningOptions := EvictionWarningOptions.empty,
      Compile / console / scalacOptions --= Seq("-Wunused:_", "-Xfatal-warnings"),
      Test / console / scalacOptions := (Compile / console / scalacOptions).value,
      Test / parallelExecution := false,
      Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oSD"),
      scalacOptions ++= Seq("-encoding", "UTF-8") ++
        Seq(
          "-deprecation",
          "-feature",
          "-language:higherKinds",
          "-language:implicitConversions",
          "-target:jvm-1.8",
          "-unchecked",
          // "-Wunused:_", // We will enable these one by one in the future
          "-Xfatal-warnings",
          "-Ymacro-annotations",
          "-Yrangepos",
        ),
    )

  override lazy val projectSettings: Seq[Def.Setting[_]] = // format: off
             aliasSettings // format: on
      .union(dependencySettings)

  lazy val aliasSettings: Seq[Def.Setting[_]] = // format: off
             addCommandAlias("l", "projects") // format: on
      .union(addCommandAlias("ll", "projects"))
      .union(addCommandAlias("ls", "projects"))
      .union(addCommandAlias("cd", "project"))
      .union(addCommandAlias("c", "compile"))
      .union(addCommandAlias("ca", "test:compile"))
      .union(addCommandAlias("t", "test"))
      .union(addCommandAlias("r", "run"))
      .union(
        addCommandAlias(
          "up2date",
          "reload plugins; dependencyUpdates; reload return; dependencyUpdates",
        ),
      )
      .union {
        onLoadMessage +=
          s"""|
              |───────────────────────────────
              |    List of defined ${styled("aliases")}
              |────────────┬──────────────────
              |${styled("l")} | ${styled("ll")} | ${styled("ls")} │ projects
              |${styled("cd")}          │ project
              |${styled("root")}        │ cd root
              |${styled("c")}           │ compile
              |${styled("ca")}          │ compile all
              |${styled("t")}           │ test
              |${styled("r")}           │ run
              |${styled("up2date")}     │ dependencyUpdates
              |────────────┴──────────────────""".stripMargin
      }

  lazy val assemblySettings: Seq[Def.Setting[_]] = {
    import sbtassembly._
    import sbtassembly.AssemblyKeys._

    Seq(
      assembly / test := {},
      assembly / assemblyMergeStrategy := {
        case "module-info.class"                           => MergeStrategy.concat
        case PathList(ps @ _*) if ps.last == "logback.xml" => MergeStrategy.first
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
    )
  }

  // TODO: factor out the cleanup routines into a common spaces
  lazy val testCleanupSettings: Seq[Def.Setting[_]] = {
    def loadClassOrFailSilently(clasz: String)(implicit classLoader: ClassLoader): Unit =
      scala.util.Try(classLoader.loadClass(s"io.paytouch.$clasz").newInstance())

    Seq(
      Test / testOptions += Tests.Cleanup { implicit classLoader =>
        // core
        loadClassOrFailSilently("core.data.daos.ConfiguredDatabaseProviderShutdown")
        loadClassOrFailSilently("core.utils.MockedRestApiShutdown")

        // ordering
        loadClassOrFailSilently("ordering.utils.ConfiguredDatabaseProviderShutdown")
        loadClassOrFailSilently("ordering.utils.MockedRestApiShutdown")
      },
    )
  }

  lazy val liquibaseSettings = {
    import java.io.{ File, StringWriter }

    import com.permutive.sbtliquibase.SbtLiquibase.autoImport._
    import com.permutive.sbtliquibase.SbtLiquibase._

    Seq(
      liquibaseDriver := "org.postgresql.Driver",
      liquibaseChangelog := {
        // All existing migrations have been run using a liquibase plugin that used `path` instead of `absolutePath`.
        // The updated liquibase plugin uses `absolutePath`, so the code below forces it to behave in the original way.
        // This is required to avoid conflicts in migrations detection.
        // It also makes more sense since it doesn't depend on the host hierarchy.
        class FakeAbsolutePathFile(pathname: String) extends File(pathname: String) {
          override def getAbsolutePath: String = super.getPath
        }
        new FakeAbsolutePathFile(s"${name.value}/src/main/resources/migrations/changelog.yml")
      },
      liquibaseUrl := sys.env.getOrElse("POSTGRES_URI", "liquibase-unknown-db-uri"),
      liquibaseUsername := sys.env.getOrElse("POSTGRES_USER", "liquibase-unknown-db-user"),
      liquibasePassword := sys.env.getOrElse("POSTGRES_PASSWORD", "liquibase-unknown-db-password"),
    ) ++ {
      lazy val criticalDbOpsAllowed =
        taskKey[Boolean]("Checks if a db is eligible for critical operations (drop, automatic-db-migrations)")

      Seq(
        criticalDbOpsAllowed := Def.task {
          val liquibaseUrlValue = liquibaseUrl.value
          val allowedPatterns = Seq("localhost", "127.0.0.1", "dev", "test", "qa")
          allowedPatterns.exists(p => liquibaseUrlValue.toLowerCase.contains(p))
        }.value,
        liquibaseDropAll := Def.taskDyn {
          if (criticalDbOpsAllowed.value) {
            println("About to drop the database...")
            Def.task(liquibaseInstance.value().execAndClose(_.dropAll()))
          }
          else Def.task(println("Database url detected as not dev or test: drop of database not allowed!"))
        }.value,
        liquibaseUpdate := Def.taskDyn {
          def askForPermission =
            scala
              .io
              .StdIn
              .readLine("Are you sure you want to run a database migration? [yes/no] ")
              .toLowerCase == "yes"

          def hasPermission =
            criticalDbOpsAllowed.value || askForPermission

          def migrateOrBlowUp =
            Def.taskDyn {
              if (hasPermission)
                Def.task {
                  println("Running database migration...")

                  liquibaseInstance
                    .value()
                    .execAndClose(_.update(liquibaseContext.value))
                }
              else
                sys.error("Not running db migration, aborting!")
            }

          val dbUpToDate = {
            val statusReport = {
              val writer = new StringWriter()
              liquibaseInstance.value().reportStatus(true, liquibaseContext.value, writer)
              writer.toString
            }

            println(statusReport)

            statusReport.contains(s"${liquibaseUrl.value} is up to date")
          }

          if (dbUpToDate)
            Def.task(println("No database migration needed"))
          else
            migrateOrBlowUp
        }.value,
      )
    }
  }

  lazy val dependencySettings: Seq[Def.Setting[_]] = Seq(
    resolvers ++= Seq(
      Resolver.bintrayRepo("hseeberger", "maven"),
      Resolver.bintrayRepo("underscoreio", "libraries"),
      "jitpack".at("https://jitpack.io"),
    ),
    // io.paytouch dependencies are not allowed here, since they would be circular
    libraryDependencies ++= Seq(
      `com.github.alexarchambault`.`scalacheck-shapeless_1.14`,
      `org.scalacheck`.`scalacheck`,
      `org.scalatest`.`scalatest`,
      `org.scalatestplus`.`scalacheck-1-14`,
      `org.typelevel`.`discipline-scalatest`,
    ).map(_ % Test),
  )

  def dockerSettings(repository: String) = {
    import sbtdocker.DockerPlugin.autoImport._

    import sbtassembly.AssemblyKeys._

    Seq(
      docker / buildOptions := BuildOptions(cache = false),
      docker / imageNames := Seq("CIRCLE_SHA1", "CIRCLE_BRANCH", "CIRCLE_BUILD_NUM").map { envKey =>
        ImageName(
          registry = Some(sys.env("ECR_REPOSITORY_URL")),
          namespace = Some(organization.value),
          repository = repository, // pt_core or pt_ordering
          tag = Some(s"vlad-${sys.env(envKey)}"),
        )
      },
      docker / dockerfile := new Dockerfile {
        // The assembly task generates a fat JAR file
        val artifact: File = assembly.value
        val artifactTarget = artifact.name

        val secretsEntryPointTarget = "/secrets-entrypoint.sh"

        val datadogJarName = "dd-java-agent.jar"

        val changelogName = s"${name.value}/src/main/resources/migrations/changelog.yml"

        from("openjdk:8-jre-alpine")
        env(
          "JAVA_OPTS",
          s"-Duser.timezone=UTC -javaagent:$datadogJarName -XX:MaxJavaStackTraceDepth=-1 -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xms200M -Xmx2G",
        )
        runRaw("apk add --update python py-pip bash curl && rm -rf /var/cache/apk/* && pip install awscli")
        workDir("/app")
        add(artifact, artifactTarget)
        add(file("bin"), "bin")
        add(file(changelogName), changelogName)
        add(file("liquibase.jar"), ".")
        add(file(datadogJarName), ".")
        copy(file("dockerfiles/secrets-entrypoint.sh"), secretsEntryPointTarget)
        entryPoint(secretsEntryPointTarget)
      },
    )
  }
}
