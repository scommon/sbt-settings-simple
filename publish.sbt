
val realm                = "Sonatype Nexus Repository Manager"


val snapshot_credentials = "sonatype-nexus-staging"
val snapshot_repository  = "https://oss.sonatype.org/content/repositories/snapshots"

val release_credentials  = "sonatype-nexus-snapshots"
val release_repository   = "https://oss.sonatype.org/service/local/staging/deploy/maven2"

val sign_artifacts       = true


publishMavenStyle := true

homepage := Some(url("https://github.com/scommon/"))

pomIncludeRepository := { _ => false }

pomExtra :=
  <scm>
    <url>git@github.com:scommon/sbt-plugin-simple.git</url>
    <connection>scm:git:git@github.com:scommon/sbt-plugin-simple.git</connection>
  </scm>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>David Hoyt</id>
      <name>David Hoyt</name>
      <email>dhoyt@hoytsoft.org</email>
      <url>http://www.hoytsoft.org/</url>
      <organization>HoytSoft</organization>
      <organizationUrl>http://www.hoytsoft.org/</organizationUrl>
      <roles>
        <role>Architect</role>
      </roles>
    </developer>
  </developers>
  //<parent>
  //  <groupId>org.sonatype.oss</groupId>
  //  <artifactId>oss-parent</artifactId>
  //  <version>7</version>
  //</parent>

publishTo <<= (version) { version: String =>
  val (name, url) =
    if (version.trim.endsWith("SNAPSHOT"))
      ("snapshots", snapshot_repository)
    else
      ("releases",  release_repository)
  Some(Resolver.url(name, new URL(url))(Patterns(true, Resolver.mavenStyleBasePattern)))
}

credentials ++= {
  val ver = version.value
  //
  val (destination, credentials_id) =
    if (ver.trim.endsWith("SNAPSHOT"))
      (snapshot_repository, snapshot_credentials)
    else
      (release_repository, release_credentials)
  //
  val host =
    new URI(destination).getHost
  //
  def loadMavenCredentials(file: java.io.File): Seq[Credentials] = {
    for {
      s <- xml.XML.loadFile(file) \ "servers" \ "server"
      id = (s \ "id").text
      if id == credentials_id
      username = (s \ "username").text
      password = (s \ "password").text
    } yield Credentials(realm, host, username, password)
  }
  //
  val sbt_credentials = Path.userHome / ".sbt"  / ".credentials"
  val ivy_credentials = Path.userHome / ".ivy2" / ".credentials"
  val mvn_credentials = Path.userHome / ".m2"   / "settings.xml"
  //
  val sbt = if (sbt_credentials.canRead) Seq(Credentials(sbt_credentials)) else Seq()
  val ivy = if (ivy_credentials.canRead) Seq(Credentials(ivy_credentials)) else Seq()
  val mvn = if (mvn_credentials.canRead) loadMavenCredentials(mvn_credentials) else Seq()
  //
  sbt ++ ivy ++ mvn
}

lazy val publishSignedAction = { st: State =>
  val extracted = Project.extract(st)
  val ref = extracted.get(thisProjectRef)
  if (sign_artifacts) {
    extracted.runAggregated(com.typesafe.sbt.pgp.PgpKeys.publishSigned in Global in ref, st)
  } else {
    extracted.runAggregated(publish in Global in ref, st)
  }
}

sbtrelease.ReleasePlugin.ReleaseKeys.releaseProcess := Seq[sbtrelease.ReleaseStep](
    sbtrelease.ReleaseStateTransformations.checkSnapshotDependencies              //
  , sbtrelease.ReleaseStateTransformations.runTest                                //
  , sbtrelease.ReleaseStateTransformations.inquireVersions                        //
  , sbtrelease.ReleaseStateTransformations.setReleaseVersion                      //
  , sbtrelease.ReleaseStateTransformations.commitReleaseVersion                   //performs the initial git checks
  , sbtrelease.ReleaseStateTransformations.tagRelease                             //
  , sbtrelease.ReleaseStateTransformations.publishArtifacts.copy(action =         //uses publish-signed instead of publish if configured.
      publishSignedAction
    )
  , sbtrelease.ReleaseStateTransformations.setNextVersion                         //
  , sbtrelease.ReleaseStateTransformations.commitNextVersion                      //
  , sbtrelease.ReleaseStateTransformations.pushChanges                            //also checks that an upstream branch is properly configured
)
