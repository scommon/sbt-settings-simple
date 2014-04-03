import sbt._
import scala.collection._

package org.scommon.sbt.settings {

import scala._
import scala.xml._
import scala.collection.Seq
import scala.collection.Traversable

object CoreSettingsPlugin extends sbt.Plugin {
    override val projectSettings = Seq(
      primarySettings in Global <<= (primarySettings in Global) ?? SimpleSettings.primary(
          name             = "<Name Unspecified>"
        , companyName      = "<Company Unspecified>"
        , organization     = "organization.unknown"
        , homepage         = "<Homepage Unspecified>"
        , vcsSpecification = "git@github.com:organization/project.git"
      ),
      promptSettings in Global <<= (promptSettings in Global) ?? SimpleSettings.prompt(
      ),
      mavenSettings in Global <<= (mavenSettings in Global) ?? SimpleSettings.maven(
      ),
      publishSettings in Global <<= (publishSettings in Global) ?? SimpleSettings.publishing(
          releaseRepository     = "<Release Repository Unspecified>"
        , snapshotRepository    = "<Snapshot Repository Unspecified>"
        , releaseCredentialsID  = ""
        , snapshotCredentialsID = ""
      ),
      releaseProcessSettings in Global <<= (releaseProcessSettings in Global) ?? SimpleSettings.releaseProcess(
      )
    )

    override val buildSettings = Seq(
      compilerSettings in Global <<= (compilerSettings in Global) ?? SimpleSettings.compiling(
      )
    )

    object SimpleSettings {
      case object PUBLISH_ARTIFACT extends PublishArtifactSpecification
      case object DO_NOT_PUBLISH_ARTIFACT extends PublishArtifactSpecification

      val defaultPrompt = PromptSettings.Default
      val git           = VersionControl.Git
      val root          = ProjectTemplates.root
      val module        = ProjectTemplates.module

      val primarySettings        = settingKey[PrimarySettings]("Standard primary settings")
      val promptSettings         = settingKey[PromptSettings]("Standard prompt settings")
      val compilerSettings       = settingKey[CompilerSettings]("Standard compiler settings")
      val mavenSettings          = settingKey[MavenSettings]("Standard maven settings")
      val publishSettings        = settingKey[PublishSettings]("Standard publish settings")
      val releaseProcessSettings = settingKey[ReleaseProcessSettings]("Standard release process settings")

      sealed case class primary(
          name            : String
        , companyName     : String
        , organization    : String
        , homepage        : String
        , vcsSpecification: String
      ) extends PrimarySettings

      sealed case class prompt(
          prompt        : TerminalPrompt = defaultPrompt
        , versionControl: VersionControl = git
        , format        : String         = DEFAULT_PROMPT_FORMAT
      ) extends PromptSettings

      sealed case class compiling(
          scalaVersion : String              = DEFAULT_SCALA_VERSION
        , scalacOptions: Traversable[String] = DEFAULT_SCALAC_OPTIONS
        , javacOptions : Traversable[String] = DEFAULT_JAVAC_OPTIONS
      ) extends CompilerSettings

      sealed case class maven(
          licenses  : Traversable[MavenSettingsLicense]   = Seq()
        , developers: Traversable[MavenSettingsDeveloper] = Seq()
        , behavior  : MavenBehavior                       = MavenBehavior
      ) extends MavenSettings

      object maven {
        def apply(items: MavenSettingsMagnet*): MavenSettings =
          apply(MavenBehavior, items:_*)

        def apply(behavior: MavenBehavior, items: MavenSettingsMagnet*): MavenSettings = {
          val licenses   = mutable.LinkedHashSet[MavenSettingsLicense]()
          val developers = mutable.LinkedHashSet[MavenSettingsDeveloper]()

          for(item <- items) item match {
            case license: MavenSettingsLicense =>
              licenses += license
            case developer: MavenSettingsDeveloper =>
              developers += developer
          }

          maven(licenses, developers, behavior)
        }
      }

      object mavenBehavior {
        def apply(
            preAppendToPom : (MavenBehavior, String, CoreSettings)                         => NodeSeq = { (_, x, y) =>
              MavenBehavior.preAppendToPom(x, y)
            }
          , appendToPom    : (MavenBehavior, String, CoreSettings)                         => NodeSeq = { (_, x, y) =>
              MavenBehavior.appendToPom(x, y)
            }
          , postAppendToPom: (MavenBehavior, String, CoreSettings)                         => NodeSeq = { (_, x, y) =>
              MavenBehavior.postAppendToPom(x, y)
            }
          , appendDeveloper: (MavenBehavior, MavenSettingsDeveloper, String, CoreSettings) => NodeSeq = { (_, x, y, z) =>
              MavenBehavior.appendDeveloper(x, y, z)
            }
        ): MavenBehavior = {
          val fnPreAppendToPom  = preAppendToPom
          val fnAppendToPom     = appendToPom
          val fnPostAppendToPom = postAppendToPom
          val fnAppendDeveloper = appendDeveloper

          new MavenBehavior {
            override def preAppendToPom(version: String, settings: CoreSettings): NodeSeq =
              fnPreAppendToPom(MavenBehavior, version, settings)

            override def appendToPom(version: String, settings: CoreSettings): NodeSeq =
              fnAppendToPom(MavenBehavior, version, settings)

            override def postAppendToPom(version: String, settings: CoreSettings): NodeSeq =
              fnPostAppendToPom(MavenBehavior, version, settings)

            override def appendDeveloper(developer: MavenSettingsDeveloper, version: String, settings: CoreSettings): NodeSeq =
              fnAppendDeveloper(MavenBehavior, developer, version, settings)
          }
        }
      }

      sealed case class publishing(
          releaseRepository    : String
        , snapshotRepository   : String
        , releaseCredentialsID : String
        , snapshotCredentialsID: String
        , signArtifacts        : Boolean         = false
        , autoDiscoverRealm    : Boolean         = true
        , realm                : String          = "Sonatype Nexus Repository Manager"
        , publishRoot          : Boolean         = true
        , behavior             : PublishBehavior = PublishBehavior
      ) extends PublishSettings

      sealed case class license(
          name: String
        , url : String
      ) extends MavenSettingsLicense

      sealed case class developer(
          id             : String
        , name           : String
        , email          : String
        , url            : String = ""
        , organization   : String = ""
        , organizationUri: String = ""
        , roles          : Traversable[String] = Seq()
      ) extends MavenSettingsDeveloper

      object publishingBehavior {
        def apply(
            publishTo        : (PublishBehavior, String, CoreSettings) => Resolver         = { (_, x, y) =>
              PublishBehavior.publishTo(x, y)
            }
          , autoDiscoverRealm: (PublishBehavior, String, CoreSettings) => String           = { (_, x, y) =>
              PublishBehavior.autoDiscoverRealm(x, y)
            }
          , loadCredentials  : (PublishBehavior, String, CoreSettings) => Seq[Credentials] = { (_, x, y) =>
              PublishBehavior.loadCredentials(x, y)
            }
        ): PublishBehavior = {
          val fnPublishTo         = publishTo
          val fnAutoDiscoverRealm = autoDiscoverRealm
          val fnLoadCredentials   = loadCredentials

          new PublishBehavior {
            override def publishTo(version: String, settings: CoreSettings): Resolver =
              fnPublishTo(PublishBehavior, version, settings)

            override def autoDiscoverRealm(destination: String, settings: CoreSettings): String =
              fnAutoDiscoverRealm(PublishBehavior, destination, settings)

            override def loadCredentials(version: String, settings: CoreSettings): Seq[Credentials] =
              fnLoadCredentials(PublishBehavior, version, settings)
          }
        }
      }

      sealed case class releaseProcess(
          steps   : Traversable[sbtrelease.ReleaseStep] = DEFAULT_RELEASE_STEPS
        , behavior: ReleaseProcessBehavior              = ReleaseProcessBehavior
      ) extends ReleaseProcessSettings

      object releaseProcessBehavior {
        def apply(
            shouldSignArtifacts : (ReleaseProcessBehavior, String,             CoreSettings) => Boolean                             = { (_, x, y) =>
              ReleaseProcessBehavior.shouldSignArtifacts(x, y)
            }
          , nextVersion         : (ReleaseProcessBehavior, sbtrelease.Version, CoreSettings) => String                              = { (_, x, y) =>
              ReleaseProcessBehavior.nextVersion(x, y)
            }
          , loadPGPPassphrase   : (ReleaseProcessBehavior, String,             CoreSettings) => String                              = { (_, x, y) =>
              ReleaseProcessBehavior.loadPGPPassphrase(x, y)
            }
          , customReleaseProcess: (ReleaseProcessBehavior, String,             CoreSettings) => Traversable[sbtrelease.ReleaseStep] = { (_, x, y) =>
              ReleaseProcessBehavior.customReleaseProcess(x, y)
            }
        ): ReleaseProcessBehavior = {
          val fnShouldSignArtifacts = shouldSignArtifacts
          val fnNextVersion = nextVersion
          val fnLoadPGPPassphrase = loadPGPPassphrase
          val fnCustomReleaseProcess = customReleaseProcess

          new ReleaseProcessBehavior {
            override def shouldSignArtifacts(version: String, settings: CoreSettings): Boolean =
              fnShouldSignArtifacts(ReleaseProcessBehavior, version, settings)

            override def nextVersion(version: sbtrelease.Version, settings: CoreSettings): String =
              fnNextVersion(ReleaseProcessBehavior, version, settings)

            override def loadPGPPassphrase(version: String, settings: CoreSettings): String =
              fnLoadPGPPassphrase(ReleaseProcessBehavior, version, settings)

            override def customReleaseProcess(version: String, settings: CoreSettings): Traversable[sbtrelease.ReleaseStep] =
              fnCustomReleaseProcess(ReleaseProcessBehavior, version, settings)
          }
        }
      }

      object ReleaseSteps {
        def stepCheckSnapshotDependencies =
          ReleaseProcessSettings.stepCheckSnapshotDependencies

        def stepRunTest =
          ReleaseProcessSettings.stepRunTest

        def stepInquireVersions =
          ReleaseProcessSettings.stepInquireVersions

        def stepSetReleaseVersion =
          ReleaseProcessSettings.stepSetReleaseVersion

        def stepCommitReleaseVersion =
          ReleaseProcessSettings.stepCommitReleaseVersion

        def stepTagRelease =
          ReleaseProcessSettings.stepTagRelease

        def stepPublishArtifacts =
          ReleaseProcessSettings.stepPublishArtifacts

        def stepSetNextVersion =
          ReleaseProcessSettings.stepSetNextVersion

        def stepCommitNextVersion =
          ReleaseProcessSettings.stepCommitNextVersion

        def stepPushChanges =
          ReleaseProcessSettings.stepPushChanges
      }
    }
  }
}

package org.scommon.sbt {
  package object settings {
    import CoreSettingsPlugin._

    val DEFAULT_SCALA_VERSION : String =
      "2.10.4"

    val DEFAULT_SCALAC_OPTIONS: Traversable[String] =
      Seq("-deprecation", "-unchecked", "-feature", "-Xelide-below", "900")

    val DEFAULT_JAVAC_OPTIONS : Traversable[String] =
      Seq("-Xlint:unchecked")

    val DEFAULT_PROMPT_FORMAT : String =
      "%s:%s:%s@%s> "

    def DEFAULT_RELEASE_STEPS : Traversable[sbtrelease.ReleaseStep] =
      ReleaseProcessSettings.defaultReleaseSteps

    val defaultPrompt = PromptSettings.Default
    val git           = VersionControl.Git
    val root          = ProjectTemplates.root
    val module        = ProjectTemplates.module

    val primarySettings        = SimpleSettings.primarySettings
    val promptSettings         = SimpleSettings.promptSettings
    val compilerSettings       = SimpleSettings.compilerSettings
    val mavenSettings          = SimpleSettings.mavenSettings
    val publishSettings        = SimpleSettings.publishSettings
    val releaseProcessSettings = SimpleSettings.releaseProcessSettings

    object AdditionalSettings {
      //Try to keep this out of the way for most users. Should only be used in conjunction with
      //project templates.
      val projectPromptName = settingKey[String]("Display name for a project")
    }

    sealed trait PublishArtifactSpecification
  }
}


//Unnamed package

import org.scommon.sbt.settings._
