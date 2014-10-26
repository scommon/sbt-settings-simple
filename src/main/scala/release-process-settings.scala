
import sbt._

import Keys._

import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import Utilities._


package org.scommon.sbt.settings {
  trait ReleaseProcessSettings extends Settings {
    def steps   : Traversable[sbtrelease.ReleaseStep]

    def behavior: ReleaseProcessBehavior
  }

  trait ReleaseProcessBehavior extends Behavior {
    import ReleaseProcessSettings._

    def shouldSignArtifacts(version: String, settings: CoreSettings): Boolean =
      settings.publish.signArtifacts

    def releaseVersion(version: String, settings: CoreSettings): String = {
      SemanticVersion(version)
        .map(_.withoutQualifier.string)
        .getOrElse(SemanticVersion.versionFormatError)
    }

    def nextVersion(version: String, settings: CoreSettings): String = {
      SemanticVersion(version)
        .map(_.bumpBugfix.asSnapshot.string)
        .getOrElse(SemanticVersion.versionFormatError)
    }

    def loadPGPPassphrase(version: String, settings: CoreSettings): String = {
      import Utils._

      //Look for it in a file.
      //Otherwise look for it in an environment variable.
      //Finally, just return an empty string.
      val sbt_credentials = Path.userHome / ".sbt"  / ".pgp"
      val from_sbt = if (sbt_credentials.canRead) readFileFirstNonEmptyLine(sbt_credentials) else None
      val from_env = sys.env.get("PGP_PASSPHRASE")

      val result = from_sbt.getOrElse(from_env.getOrElse(""))

      result
    }

    def customReleaseProcess(version: String, settings: CoreSettings): Traversable[sbtrelease.ReleaseStep] =
      settings.releaseProcess.steps
  }
}


//Unnamed package

import org.scommon.sbt.settings._

case object ReleaseProcessBehavior extends ReleaseProcessBehavior

object ReleaseProcessSettings {
  val pgpPassphrase =
    //Do not prompt for PGP pass phrase
    com.typesafe.sbt.SbtPgp.PgpKeys.pgpPassphrase :=
      Some(delegateLoadPGPPassphrase(
          sbt.Keys.version.value
        , CoreSettings(
              org.scommon.sbt.settings.primarySettings.value
            , org.scommon.sbt.settings.promptSettings.value
            , org.scommon.sbt.settings.compilerSettings.value
            , org.scommon.sbt.settings.scaladocSettings.value
            , org.scommon.sbt.settings.mavenSettings.value
            , org.scommon.sbt.settings.publishSettings.value
            , org.scommon.sbt.settings.releaseProcessSettings.value
          )
      ).toCharArray)

  val stepCheckSnapshotDependencies =
    sbtrelease.ReleaseStateTransformations.checkSnapshotDependencies

  val stepRunTest =
    sbtrelease.ReleaseStateTransformations.runTest

  val stepInquireVersions =
    sbtrelease.ReleaseStateTransformations.inquireVersions

  val stepSetReleaseVersion =
    sbtrelease.ReleaseStateTransformations.setReleaseVersion

  val stepCommitReleaseVersion =
    sbtrelease.ReleaseStateTransformations.commitReleaseVersion

  val stepTagRelease =
    sbtrelease.ReleaseStateTransformations.tagRelease

  val stepPublishArtifacts =
    //Determine if publish-signed should be used instead of publish.
    sbtrelease.ReleaseStateTransformations.publishArtifacts.copy(action =
      runPublishAction()
    )

  val stepSetNextVersion =
    sbtrelease.ReleaseStateTransformations.setNextVersion

  val stepCommitNextVersion =
    sbtrelease.ReleaseStateTransformations.commitNextVersion

  val stepPushChanges =
    sbtrelease.ReleaseStateTransformations.pushChanges

  val releaseProcess =
    //Customize the steps of the release process.
    sbtrelease.ReleasePlugin.ReleaseKeys.releaseProcess :=
      delegateCustomReleaseProcess(
          sbt.Keys.version.value
        , CoreSettings(
              org.scommon.sbt.settings.primarySettings.value
            , org.scommon.sbt.settings.promptSettings.value
            , org.scommon.sbt.settings.compilerSettings.value
            , org.scommon.sbt.settings.scaladocSettings.value
            , org.scommon.sbt.settings.mavenSettings.value
            , org.scommon.sbt.settings.publishSettings.value
            , org.scommon.sbt.settings.releaseProcessSettings.value
          )
      )

  val releaseVersion =
    //Customize the next version string to bump the revision number.
    sbtrelease.ReleasePlugin.ReleaseKeys.releaseVersion := { ver =>
      delegateReleaseVersion(ver,
        CoreSettings(
            org.scommon.sbt.settings.primarySettings.value
          , org.scommon.sbt.settings.promptSettings.value
          , org.scommon.sbt.settings.compilerSettings.value
          , org.scommon.sbt.settings.scaladocSettings.value
          , org.scommon.sbt.settings.mavenSettings.value
          , org.scommon.sbt.settings.publishSettings.value
          , org.scommon.sbt.settings.releaseProcessSettings.value
        )
      )
    }

  val nextVersion =
    //Customize the next version string to bump the revision number.
    sbtrelease.ReleasePlugin.ReleaseKeys.nextVersion := { ver =>
      delegateNextVersion(ver,
        CoreSettings(
            org.scommon.sbt.settings.primarySettings.value
          , org.scommon.sbt.settings.promptSettings.value
          , org.scommon.sbt.settings.compilerSettings.value
          , org.scommon.sbt.settings.scaladocSettings.value
          , org.scommon.sbt.settings.mavenSettings.value
          , org.scommon.sbt.settings.publishSettings.value
          , org.scommon.sbt.settings.releaseProcessSettings.value
        )
      )
    }

  val defaultReleaseSteps =
    Seq(
        stepCheckSnapshotDependencies              //
      , stepRunTest                                //
      , stepInquireVersions                        //
      , stepSetReleaseVersion                      //
      , stepCommitReleaseVersion                   //performs the initial git checks
      , stepTagRelease                             //
      , stepPublishArtifacts                       //Customized to determine if artifacts should be signed
      , stepSetNextVersion                         //
      , stepCommitNextVersion                      //
      , stepPushChanges                            //also checks that an upstream branch is properly configured
    )

  val defaults =
    pgpPassphrase +: sbtrelease.ReleasePlugin.releaseSettings :+ releaseProcess :+ releaseVersion :+ nextVersion


  private[this] def runPublishAction() = { st: sbt.State =>
    val extracted = st.extract

    val project_ref =
      extracted.get(sbt.Keys.thisProjectRef)
    val version =
      extracted.get(sbt.Keys.version)
    val settings =
      CoreSettings(
          extracted.get(org.scommon.sbt.settings.primarySettings)
        , extracted.get(org.scommon.sbt.settings.promptSettings)
        , extracted.get(org.scommon.sbt.settings.compilerSettings)
        , extracted.get(org.scommon.sbt.settings.scaladocSettings)
        , extracted.get(org.scommon.sbt.settings.mavenSettings)
        , extracted.get(org.scommon.sbt.settings.publishSettings)
        , extracted.get(org.scommon.sbt.settings.releaseProcessSettings)
      )

    if (delegateShouldSignArtifacts(version, settings)) {
      extracted.runAggregated(com.typesafe.sbt.pgp.PgpKeys.publishSigned in Global in project_ref, st)
    } else {
      extracted.runAggregated(sbt.Keys.publish in Global in project_ref, st)
    }
  }

  private[this] def delegateShouldSignArtifacts(version: String, settings: CoreSettings): Boolean =
    settings.releaseProcess.behavior.shouldSignArtifacts(version, settings)

  private[this] def delegateReleaseVersion(version: String, settings: CoreSettings): String =
    settings.releaseProcess.behavior.releaseVersion(version, settings)

  private[this] def delegateNextVersion(version: String, settings: CoreSettings): String =
    settings.releaseProcess.behavior.nextVersion(version, settings)

  private[this] def delegateLoadPGPPassphrase(version: String, settings: CoreSettings): String =
    settings.releaseProcess.behavior.loadPGPPassphrase(version, settings)

  private[this] def delegateCustomReleaseProcess(version: String, settings: CoreSettings): Seq[sbtrelease.ReleaseStep] =
    settings.releaseProcess.behavior.customReleaseProcess(version, settings).toSeq
}
