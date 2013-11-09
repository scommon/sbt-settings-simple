
import sbt._
import Keys._

package org.scommon.sbt.settings {
  object ProjectTemplates {
    import CoreSettingsPlugin.SimpleSettings.{PUBLISH_ARTIFACT, DO_NOT_PUBLISH_ARTIFACT}

    object root {
      def apply(name: String, aggregate: ProjectReference*): Project =
        apply(name, "", PUBLISH_ARTIFACT, aggregate:_*)

      def apply(name: String, promptName: String, aggregate: ProjectReference*): Project =
        apply(name, promptName, PUBLISH_ARTIFACT, aggregate:_*)

      def apply(name: String, promptName: String, publishArtifact: PublishArtifactSpecification, aggregate: ProjectReference*): Project = Project(
        id        = ThisProject.root(name),
        base      = ThisProject.root.base(),
        settings  = ThisProject.root.settings(promptName, publishArtifact eq PUBLISH_ARTIFACT),

        aggregate = aggregate.toSeq
      )
    }

    object module {
      def apply(name: String):Project =
        apply("", name, "", PUBLISH_ARTIFACT)

      def apply(prefix: String, name: String): Project =
        apply(prefix, name, "", PUBLISH_ARTIFACT)

      def apply(prefix: String, name: String, promptName: String): Project =
        apply(prefix, name, promptName, PUBLISH_ARTIFACT)

      def apply(prefix: String, name: String, promptName: String, publishArtifact: PublishArtifactSpecification): Project = Project(
        id        = ThisProject.module(prefix, name),
        base      = ThisProject.module.base(name),
        settings  = ThisProject.module.settings(promptName, publishArtifact eq PUBLISH_ARTIFACT)
      )
    }
  }
}


//Unnamed package

import org.scommon.sbt.settings._
