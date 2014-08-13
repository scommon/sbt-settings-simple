import sbt._

package org.scommon.sbt.settings {
  trait CrossCompileSettings extends Settings {
    def defaultVersion: Option[String]
    def compilers: Traversable[CompilerSettings]
    lazy val default: CompilerSettings = {
      val fromDefaultVersion = defaultVersion map (ver => compilers.find(_.scalaVersion == ver).getOrElse(throw new IllegalStateException(s"Attempting to find the default Scala compiler version. The provided default version ($ver) was not in the list of valid versions: ${compilers.map(_.scalaVersion).mkString(", ")}")))
      fromDefaultVersion.getOrElse(compilers.headOption.getOrElse(throw new IllegalStateException(s"Please provide at least one compiler settings")))
    }
  }
}


//Unnamed package

import org.scommon.sbt.settings._

import scala.collection._
import org.scommon.sbt.settings._

object CrossCompileSettings {
  def byVersion(version: String, compilerSettings: CrossCompileSettings): CompilerSettings = {
    val compilers = compilerSettings.compilers
    val default = compilerSettings.default

    compilers.find(_.scalaVersion == version).getOrElse(throw new IllegalStateException(s"Unable to find the provided version ($version) in the list of valid Scala compiler versions: ${compilerSettings.compilers.map(_.scalaVersion).mkString(", ")}"))
  }

  //See: http://stackoverflow.com/questions/12626197/conditional-scalacoptions-with-sbt
  val scalacOptions =
    sbt.Keys.scalacOptions in ThisBuild <<= Def.task {
      val ver = sbt.Keys.scalaVersion.value
      val compilerSettings = org.scommon.sbt.settings.compilerSettings.value
      byVersion(ver, compilerSettings).scalacOptions.toSeq
    }

  //See: http://stackoverflow.com/questions/12626197/conditional-scalacoptions-with-sbt
  val javacOptions =
    sbt.Keys.javacOptions in ThisBuild <<= Def.task {
      val ver = sbt.Keys.scalaVersion.value
      val compilerSettings = org.scommon.sbt.settings.compilerSettings.value
      byVersion(ver, compilerSettings).javacOptions.toSeq
    }

  val crossScalaVersions = {
    val setting = Def.setting {
      val compilerSettings = org.scommon.sbt.settings.compilerSettings.value
      if (compilerSettings.compilers.size > 1)
        compilerSettings.compilers.map(_.scalaVersion).toSeq
      else
        Seq(compilerSettings.default.scalaVersion)
    }

    Seq(
      sbt.Keys.crossScalaVersions in Global <<= setting
    )
  }

  val scalaVersion =
    sbt.Keys.scalaVersion in Global <<= Def.setting {
      val compilerSettings = org.scommon.sbt.settings.compilerSettings.value
      compilerSettings.default.scalaVersion
    }

  val defaults: Seq[sbt.Def.Setting[_]] =
    crossScalaVersions ++ Seq(
      scalaVersion
    , scalacOptions
    , javacOptions
  )
}