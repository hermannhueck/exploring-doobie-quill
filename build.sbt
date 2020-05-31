val projectName        = "exploring-doobie-quill"
val projectDescription = "Exploring Doobie and Quill for functional database access"
val projectVersion     = "0.1.0"

val scala213 = "2.13.2"

val scalaCompiler = "org.scala-lang" % "scala-compiler" % scala213
val scalaReflect  = "org.scala-lang" % "scala-reflect"  % scala213

import Dependencies._

lazy val coreLibraryDependencies =
  Seq(
    scalaCompiler,
    scalaReflect,
    shapeless,
    monixEval,
    fs2Core,
    circeCore,
    circeJawn,
    http4sCore,
    doobieCore,
    doobiePostgres,
    doobieH2,
    doobieHikari,
    doobieQuill,
    doobieSpecs2,
    doobieScalatest,
    quillCore,
    zio,
    munit,
    kindProjectorPlugin,
    betterMonadicForPlugin
  ) ++ Seq(
    scalaCheck
  ).map(_ % Test)

lazy val myInitialCommands       =
  s"""|
     |import scala.util.chaining._
     |import fs2._, cats.effect._, cats.effect.implicits._, cats.implicits._
     |import scala.concurrent.ExecutionContext.Implicits.global
     |import scala.concurrent.duration._
     |implicit val contextShiftIO: ContextShift[IO] = IO.contextShift(global)
     |implicit val timerIO: Timer[IO] = IO.timer(global)
     |println
     |""".stripMargin

lazy val commonSettings =
  Seq(
    version := projectVersion,
    scalaVersion := scala213,
    publish / skip := true,
    scalacOptions ++= ScalacOptions.defaultScalacOptions,
    Compile / console / scalacOptions := ScalacOptions.consoleScalacOptions,
    Test / console / scalacOptions := ScalacOptions.consoleScalacOptions,
    semanticdbEnabled := true,
    semanticdbVersion := "4.3.10",                                                // scalafixSemanticdb.revision,
    scalafixDependencies ++= Seq("com.github.liancheng" %% "organize-imports" % "0.3.0"),
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "100"), // -s = -minSuccessfulTests
    testFrameworks += new TestFramework("munit.Framework"),
    initialCommands := myInitialCommands
  )

lazy val root = (project in file("."))
  .aggregate(core)
  .settings(commonSettings)
  .settings(
    name := "root",
    description := "root project",
    sourceDirectories := Seq.empty
  )

lazy val core = (project in file("core"))
  .dependsOn(hutil)
  .settings(commonSettings)
  .settings(
    name := projectName,
    description := projectDescription,
    libraryDependencies ++= coreLibraryDependencies
  )

lazy val hutil = (project in file("hutil"))
  .settings(commonSettings)
  .settings(
    name := "hutil",
    description := "Hermann's Utilities",
    libraryDependencies ++= Seq(
      scalaCompiler,
      scalaReflect,
      catsEffect
    )
  )

// GraphBuddy support
// resolvers += Resolver.bintrayRepo("virtuslab", "graphbuddy")
// addCompilerPlugin("com.virtuslab.semanticgraphs" % "scalac-plugin" % "0.0.10" cross CrossVersion.full)
// scalacOptions += "-Yrangepos"
