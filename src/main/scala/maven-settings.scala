
import sbt._
import Keys._

import scala.xml._

import java.net.URL

package org.scommon.sbt.settings {
  trait MavenSettings extends Settings {
    def licenses  : Traversable[MavenSettingsLicense]
    def developers: Traversable[MavenSettingsDeveloper]

    def behavior  : MavenBehavior
  }

  trait MavenBehavior extends Behavior {
    def preAppendToPom(version: String, settings: CoreSettings): NodeSeq =
      NodeSeq.Empty

    def appendToPom(version: String, settings: CoreSettings): NodeSeq =
      <scm>
        <url>{settings.primary.vcsSpecification}</url>
        <connection>scm:git:{settings.primary.vcsSpecification}</connection>
      </scm>
      <developers>
        {for (developer <- settings.maven.developers) yield settings.maven.behavior.appendDeveloper(developer, version, settings)}
      </developers>
      //<parent>
      //  <groupId>org.sonatype.oss</groupId>
      //  <artifactId>oss-parent</artifactId>
      //  <version>7</version>
      //</parent>

    def postAppendToPom(version: String, settings: CoreSettings): NodeSeq =
      NodeSeq.Empty

    def appendDeveloper(developer: MavenSettingsDeveloper, version: String, settings: CoreSettings): NodeSeq =
      developer.toXml(settings)
  }

  trait MavenSettingsMagnet

  trait MavenSettingsLicense extends MavenSettingsMagnet {
    def name: String
    def url : String
  }

  trait MavenSettingsDeveloper extends MavenSettingsMagnet {
    def id             : String
    def name           : String
    def email          : String
    def url            : String
    def organization   : String
    def organizationUri: String
    def roles          : Traversable[String]

    def toXml(settings: CoreSettings): NodeSeq =
      <developer>
        <id>{id}</id>
        <name>{name}</name>
        <email>{email}</email>
        <url>{url}</url>
        <organization>{organization}</organization>
        <organizationUrl>{organizationUri}</organizationUrl>
        <roles>
          {for (role <- roles) yield <role>{role}</role>}
        </roles>
      </developer>
  }

  object MavenSettingsLicense {
    import scala.language.implicitConversions

    implicit def license2Tuple2(license: MavenSettingsLicense): (String, URL) =
      license.name -> url(license.url)

    implicit def seqLicense2SeqTuple2(licenses: Seq[MavenSettingsLicense]): Seq[(String, URL)] =
      for(license <- licenses) yield license2Tuple2(license)
  }
}


//Unnamed package

import org.scommon.sbt.settings._
import org.scommon.sbt.settings.Utils._

case object MavenBehavior extends MavenBehavior

object MavenSettings {
  import MavenSettingsLicense._

  val licenses =
    sbt.Keys.licenses :=
      org.scommon.sbt.settings.mavenSettings.value.licenses.toSeq

  val homepage =
    sbt.Keys.homepage :=
      optionUrl(org.scommon.sbt.settings.primarySettings.value.homepage)

  val pomIncludeRepository =
    sbt.Keys.pomIncludeRepository := { _ =>
      false
    }

  val pomExtra =
    sbt.Keys.pomExtra :=
      delegateAppendToPom(
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

  val defaults = Seq(
      licenses
    , homepage
    , pomIncludeRepository
    , pomExtra
  )

  private[this] def delegateAppendToPom(version: String, settings: CoreSettings): NodeSeq =
    settings.maven.behavior.preAppendToPom(version, settings)  ++
    settings.maven.behavior.appendToPom(version, settings)     ++
    settings.maven.behavior.postAppendToPom(version, settings)

}

