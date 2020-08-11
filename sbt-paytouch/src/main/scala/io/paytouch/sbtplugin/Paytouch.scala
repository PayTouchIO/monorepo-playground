package io.paytouch
package sbtplugin

import sbt.{ io => _, _ }
import sbt.Keys._
import sbt.nio.Keys._

import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

import bloop.integrations.sbt._

import Util._

object Paytouch extends AutoPlugin {
  // Everything inside of this object can be used without imports in client projects.
  object autoImport extends Dependencies.Binary with Dependencies.Source {
    val Cctt = sbtplugin.Util.Cctt
  }

  import autoImport._

  // The plugin will be automatically enabled by the clients (I think).
  // To disable use project.disablePlugins(Paytouch).
  override lazy val trigger = allRequirements

  // An sbt plugin can depend on other plugins.
  override lazy val requires = plugins.JvmPlugin

  // Global / settings
  override lazy val globalSettings: Seq[Setting[_]] =
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

  // ThisBuild / settings
  override lazy val buildSettings: Seq[Setting[_]] =
    Seq(
      organization := "io.paytouch", // note: used to be just paytouch
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
          "-language:_",
          "-unchecked",
          // "-Wunused:_", // todo we will enable these one by one in the future
          "-Xfatal-warnings",
          "-Ymacro-annotations",
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

  lazy val dependencySettings: Seq[Def.Setting[_]] = Seq(
    // io.paytouch dependencies are not allowed here, since they would be circular
    libraryDependencies ++= Seq(
      `com.github.alexarchambault`.`scalacheck-shapeless_1.14`,
      `org.scalacheck`.`scalacheck`,
      `org.scalatest`.`scalatest`,
      `org.scalatestplus`.`scalacheck-1-14`,
      `org.typelevel`.`discipline-scalatest`,
    ).map(_ % Test),
  )
}
