
import sbt._

package org.scommon.sbt.settings {
  trait VersionControl {
    def currentCommit(): String
    def shortenedCurrentCommit(): String
    def branchName(): String
  }
}


//Unnamed package

import org.scommon.sbt.settings._

object VersionControl {
  object Git extends VersionControl {
    def currentCommit() =
      Process("git rev-parse HEAD").lines.head

    def shortenedCurrentCommit() =
      Process("git rev-parse --short HEAD").lines.head

    def branchName() =
      Process("git rev-parse --abbrev-ref HEAD").lines.head
  }
}
