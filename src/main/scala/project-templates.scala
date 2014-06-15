
import sbt._
import Keys._

package org.scommon.sbt.settings {
  object ProjectTemplates {
    import Utils._

    def version = Keys.version in ThisBuild

    def defaultSettings(prompt: String, publish: Boolean): Seq[Setting[_]] =
      Defaults.coreDefaultSettings ++
      Seq(
          AdditionalSettings.projectPromptName := prompt
        , publishArtifact := publish
      ) ++
      ScaladocSettings.defaults ++
      PublishSettings.defaults ++
      ReleaseProcessSettings.defaults ++
      BuildSettings.defaults ++
      MavenSettings.defaults

    object module {
      def apply(name: String)
               (base: String,
                id: String = normalizeId(name),
                prompt: String = name,
                publish: Boolean = true,
                additionalSettings: Iterable[Setting[_]] = Seq(),
                aggregate: Iterable[ProjectReference] = Seq(),
                dependencies: Iterable[ClasspathDep[ProjectReference]] = Seq()): Project =
        Project(
          id           = id,
          base         = file(base),
          settings     = defaultSettings(prompt, publish) ++ additionalSettings,
          aggregate    = aggregate.toSeq,
          dependencies = dependencies.toSeq
        )
    }
  }
}


//Unnamed package

import org.scommon.sbt.settings._
