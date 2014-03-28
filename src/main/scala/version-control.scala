
import sbt._
import scala.collection._

package org.scommon.sbt.settings {
  trait VersionControl {
    def currentCommit(): String
    def shortenedCurrentCommit(): String
    def branchName(): String
    def help: String
  }
}


//Unnamed package

import org.scommon.sbt.settings._

object VersionControl {
  object Default extends VersionControl {
    override def currentCommit(): String =
      "000000"

    override def shortenedCurrentCommit(): String =
      "000000"

    override def branchName(): String =
      "unknown"

    override def help: String =
      ""
  }

  object Git extends VersionControl {
    def firstOrThrow(command: String): String = {
      val stdout = mutable.ArrayBuffer[String]()
      val exit_code = Process(command) ! new ProcessLogger {
        override def info(s: => String): Unit = stdout += s
        override def error(s: => String): Unit = {}
        override def buffer[T](f: => T): T = f
      }
      require(exit_code == 0, "Invalid command")
      stdout.headOption.getOrElse("")
    }

    def currentCommit() =
      firstOrThrow("git rev-parse HEAD")

    def shortenedCurrentCommit() =
      firstOrThrow("git rev-parse --short HEAD")

    def branchName() =
      firstOrThrow("git rev-parse --abbrev-ref HEAD")

    override def help: String =
      "Please initialize the git repository"
  }
}
