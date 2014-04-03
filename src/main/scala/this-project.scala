
import sbt._
import Keys._

package org.scommon.sbt.settings {
  object ThisProject {
    object root {
      def apply:String =
        apply("root")

      def apply(name:String):String =
        if ("" == name)
          "root"
        else
          name.toLowerCase()

      def base(path:String = ".")      = file(path)
      def version                      = Keys.version in ThisBuild
      def settings(promptName: String, shouldPublishArtifact: Boolean) =
        Defaults.defaultSettings ++
        Seq(
            AdditionalSettings.projectPromptName := promptName
          , publishArtifact := shouldPublishArtifact
        ) ++
        PublishSettings.defaults ++
        ReleaseProcessSettings.defaults ++
        BuildSettings.defaults ++
        MavenSettings.defaults
    }

    object module {
      def apply(name:String):String =
        apply("", name)

      def apply(prefix:String, name:String):String =
        if ("" == prefix)
          "%s".format(name)
        else
          "%s-%s".format(prefix.toLowerCase(), name)

      def base(path:String)            = root.base(path)
      def version                      = root.version
      def settings(promptName: String, shouldPublishArtifact: Boolean) =
        Defaults.defaultSettings ++
        Seq(
            AdditionalSettings.projectPromptName := promptName
          , publishArtifact := shouldPublishArtifact
        ) ++
        PublishSettings.defaults ++
        ReleaseProcessSettings.defaults ++
        BuildSettings.defaults ++
        MavenSettings.defaults
    }
  }
}

//Unnamed package

import org.scommon.sbt.settings._
