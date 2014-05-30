
import sbt._
import Keys._

package org.scommon.sbt.settings {
  trait PublishSettings extends Settings {
    def publishRoot          : Boolean
    def realm                : String
    def releaseCredentialsID : String
    def releaseRepository    : String
    def snapshotCredentialsID: String
    def snapshotRepository   : String
    def autoDiscoverRealm    : Boolean
    def signArtifacts        : Boolean

    def behavior             : PublishBehavior
  }

  trait PublishBehavior extends Behavior {
    def publishTo(version: String, settings: CoreSettings): Resolver = {
      //http://www.scala-sbt.org/release/docs/Detailed-Topics/Publishing
      if (version.trim.endsWith("SNAPSHOT"))
        "snapshots" at settings.publish.snapshotRepository
      else
        "releases"  at settings.publish.releaseRepository
    }

    def autoDiscoverRealm(destination: String, settings: CoreSettings): String = {
      //Make a POST to the destination in order to extract the WWW-Authenticate header
      //and discover the realm to use.
      //
      //Example of what the header might look like:
      //  WWW-Authenticate: BASIC realm="Sonatype Nexus Repository Manager"
      POST(destination) map { dest =>
        val response = dest.headers.getOrElse("WWW-Authenticate", s"""BASIC realm="${settings.publish.realm}" """)

        //Parse the return value to extract the realm.
        val pattern = """(?<=realm=").+(?=")""".r
        val realm = pattern findFirstIn response

        //If unable to extract the realm, attempt to use the default.
        realm getOrElse settings.publish.realm
      } getOrElse settings.publish.realm
    }

    protected def hostFor(destination: String, settings: CoreSettings): Option[String] = {
      try Some(new URI(destination).getHost)
      catch {
        case _: Throwable => None
      }
    }

    def loadCredentials(version: String, settings: CoreSettings): Seq[Credentials] = {
      val (destination, credentials_id) =
        if (version.trim.endsWith("SNAPSHOT"))
          (settings.publish.snapshotRepository, settings.publish.snapshotCredentialsID)
        else
          (settings.publish.releaseRepository, settings.publish.releaseCredentialsID)

      val realm =
        if (settings.publish.autoDiscoverRealm)
          autoDiscoverRealm(destination, settings)
        else
          settings.publish.realm

      (for {
        host <- hostFor(destination, settings)
      } yield {

        def loadMavenCredentials(file: java.io.File): Seq[Credentials] = {
          for {
            s <- xml.XML.loadFile(file) \ "servers" \ "server"
            id = (s \ "id").text
            if id == credentials_id
            username = (s \ "username").text
            password = (s \ "password").text
          } yield Credentials(realm, host, username, password)
        }

        val sbt_credentials = Path.userHome / ".sbt" / ".credentials"
        val ivy_credentials = Path.userHome / ".ivy2" / ".credentials"
        val mvn_credentials = Path.userHome / ".m2" / "settings.xml"

        //Attempt to gather credentials from the environment if possible.
        val env = Seq(sys.env.get("PUBLISH_USER"), sys.env.get("PUBLISH_PASSWORD")).flatten match {
          case Seq(username, password) =>
            Seq(Credentials(realm, host, username, password))
          case _ =>
            Seq()
        }

        //Attempt to read in all the credentials.
        val sbt = if (sbt_credentials.canRead) Seq(Credentials(sbt_credentials)) else Seq()
        val ivy = if (ivy_credentials.canRead) Seq(Credentials(ivy_credentials)) else Seq()
        val mvn = if (mvn_credentials.canRead) loadMavenCredentials(mvn_credentials) else Seq()

        env ++ sbt ++ ivy ++ mvn
      }) getOrElse Seq()
    }
  }
}


//Unnamed package

import org.scommon.sbt.settings._

case object PublishBehavior extends PublishBehavior

object PublishSettings {
  val credentials =
    sbt.Keys.credentials ++=
      delegateLoadCredentials(
          sbt.Keys.version.value
        , CoreSettings(
              org.scommon.sbt.settings.primarySettings.value
            , org.scommon.sbt.settings.promptSettings.value
            , org.scommon.sbt.settings.compilerSettings.value
            , org.scommon.sbt.settings.mavenSettings.value
            , org.scommon.sbt.settings.publishSettings.value
            , org.scommon.sbt.settings.releaseProcessSettings.value
          )
      )

  val publishArtifact =
    sbt.Keys.publishArtifact in Test :=
      false

  val publishTo =
    sbt.Keys.publishTo :=
      Some(delegatePublishTo(
          sbt.Keys.version.value
        , CoreSettings(
            org.scommon.sbt.settings.primarySettings.value
          , org.scommon.sbt.settings.promptSettings.value
          , org.scommon.sbt.settings.compilerSettings.value
          , org.scommon.sbt.settings.mavenSettings.value
          , org.scommon.sbt.settings.publishSettings.value
          , org.scommon.sbt.settings.releaseProcessSettings.value
        )
      ))

  val publishMavenStyle =
    sbt.Keys.publishMavenStyle :=
      true

  val defaults =
    Seq(
        credentials
      , publishArtifact
      , publishTo
      , publishMavenStyle
    )

  private[this] def delegatePublishTo(version: String, settings: CoreSettings): Resolver =
    settings.publish.behavior.publishTo(version, settings)

  private[this] def delegateLoadCredentials(version: String, settings: CoreSettings): Seq[Credentials] =
    settings.publish.behavior.loadCredentials(version, settings)
}

