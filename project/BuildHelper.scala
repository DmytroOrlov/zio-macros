import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  val zioVersion = "1.0.0-RC17"

  private val testDeps = Seq(
    "dev.zio" %% "zio-test"     % zioVersion % "test",
    "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
  )

  private def compileOnlyDeps(scalaVersion: String) = {
    val stdCompileOnlyDeps = Seq(
      "com.github.ghik" %% "silencer-lib" % "1.4.2" % "provided",
      compilerPlugin("org.typelevel"   %% "kind-projector"  % "0.10.3"),
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.2")
    )
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, x)) if x <= 12 =>
        stdCompileOnlyDeps ++ Seq(
          compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full))
        )
      case _ => stdCompileOnlyDeps
    }
  }

  private def compilerOptions(scalaVersion: String, optimize: Boolean) = {
    val stdOptions = Seq(
      //"-Ymacro-debug-lite",
      //"-Ymacro-debug-verbose",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-language:higherKinds",
      "-language:existentials",
      "-explaintypes",
      "-Yrangepos",
      "-Xsource:2.13",
      "-Xlint:_,-type-parameter-shadow",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    )

    val optimizerOptions =
      if (optimize)
        Seq(
          "-opt:l:inline"
        )
      else Seq.empty

    val extraOptions = CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused",
          "-Ymacro-annotations"
        ) ++ optimizerOptions
      case Some((2, 12)) =>
        Seq(
          "-Ypartial-unification",
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        ) ++ optimizerOptions
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import"
        )
      case _ => Seq.empty
    }

    stdOptions ++ extraOptions
  }

  val buildInfoSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfoZioMacros"
  )

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      crossScalaVersions := Seq("2.13.0", "2.12.8", "2.11.12"),
      scalaVersion in ThisBuild := crossScalaVersions.value.head,
      scalacOptions := compilerOptions(scalaVersion.value, optimize = !isSnapshot.value),
      libraryDependencies ++= compileOnlyDeps(scalaVersion.value) ++ testDeps,
      parallelExecution in Test := true,
      incOptions ~= (_.withLogRecompileOnMacro(true)),
      autoAPIMappings := true,
      unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library"),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )

  def macroSettings() =
    Seq(
      scalacOptions --= Seq("-deprecation", "-Xfatal-warnings"),
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value % "provided",
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
      )
    )

  def examplesSettings() =
    Seq(
      skip in publish := true,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % zioVersion,
        "io.7mind.izumi" %% "distage-core" % "0.10.0-M6",
      ),
      scalacOptions ++= Seq(
        "-language:reflectiveCalls",
      )
    )

  def testSettings() =
    Seq(
      skip in publish := true,
      scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, x)) if x <= 11 => Seq("-Ywarn-unused:false")
          case _                       => Seq("-Ywarn-unused:-explicits,_")
        }
      }
    )
}
