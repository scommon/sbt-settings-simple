import sbt._

package org.scommon.sbt.settings {
  trait CompilerSettings extends Settings {
    def scalaVersion : String
    def scalacOptions: Traversable[String]
    def javacOptions : Traversable[String]
  }
}


//Unnamed package

import org.scommon.sbt.settings._
