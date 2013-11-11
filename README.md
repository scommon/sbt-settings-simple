# SBT-Settings-Simple

This plugin provides an easier to use and configure interface into SBT which allows you to largely 
ignore having to learn about SBT tasks and settings and to instead focus on delivering your work.

It's goal is to simply *lower the barrier of entry for new SBT adopters.*

You probably do not want to use this if you have heavy customizations of your build process although 
it is flexible enough that you may get further than you realize with it while still being able to 
reduce a significant amount of boilerplate and keeping your build code focused on what's unique 
about your project.

### Tell me more
Common tasks like signing artifacts and setting up your build for publishing to maven repositories 
like Maven Central (via Sonatype) or a Nexus instance are taken care of. Basically it takes 
configurations that look like (and this is a rather simple one):

```scala
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

name := "sbt-settings-simple"

organization := "org.scommon"

scalaVersion := "2.10.3"

scalacOptions := Seq("-deprecation", "-unchecked")

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
      <id>John Doe</id>
      <name>John Doe</name>
      <email>john@hiscompany.com</email>
      <url>http://www.johnswebsite.com/</url>
      <organization>His Company</organization>
      <organizationUrl>http://www.hiscompany.com/</organizationUrl>
      <roles>
        <role>Architect</role>
      </roles>
    </developer>
  </developers>

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

releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies              //
  , runTest                                //
  , inquireVersions                        //
  , setReleaseVersion                      //
  , commitReleaseVersion                   //performs the initial git checks
  , tagRelease                             //
  , publishArtifacts.copy(action =         //uses publish-signed instead of publish if configured.
      publishSignedAction
    )
  , setNextVersion                         //
  , commitNextVersion                      //
  , pushChanges                            //also checks that an upstream branch is properly configured
)
```

And turns it into:

```scala
import SimpleSettings._

primarySettings := primary(
    name             = "sbt-settings-simple"
  , companyName      = "scommon"
  , organization     = "org.scommon"
  , homepage         = "https://github.com/scommon/sbt-settings-simple"
  , vcsSpecification = "git@github.com:scommon/sbt-settings-simple.git"
)

mavenSettings := maven(
  license(
      name  = "The Apache Software License, Version 2.0"
    , url   = "http://www.apache.org/licenses/LICENSE-2.0.txt"
  ),
  developer(
      id              = "John Doe"
    , name            = "John Doe"
    , email           = "john@hiscompany.com"
    , url             = "http://www.johnswebsite.com/"
    , organization    = "His Company"
    , organizationUri = "http://www.hiscompany.com/"
    , roles           = Seq("Architect")
  )
)

publishSettings := publishing(
    releaseCredentialsID  = "sonatype-nexus-staging"
  , releaseRepository     = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  , snapshotCredentialsID = "sonatype-nexus-snapshots"
  , snapshotRepository    = "https://oss.sonatype.org/content/repositories/snapshots"
)
```

### But why?
 * No need to know when to use `<<=` vs. `++=` vs. `+=` vs. `:=`
 * No need to know that the SBT release plugin (as of v0.8) does not sign artifacts during the publish task requiring you to customize the action to take
 * No need to know how to look for credentials on the system without specifying them explicitly in your SBT configuration using `credentials += ...`
 * Automatically looks in your maven settings (`~/.m2/settings.xml`) for credentials
 * Knows how to form a proper maven pom that will be accepted by Sonatype for publishing to Maven Central
 * Can change the publish location based on snapshots vs. releases
 * Can change credentials used based on snapshots vs. releases
 * Can specify if a project should be published or not
 * Provides a special prompt that (by default) displays the name, current project, current branch, and most recent commit
 * Provides easier customization of the release process
 * Automatically discovers the publishing destination's realm
 * Allows the customization of the behavior of producing pom output, release steps, serializing developer and license information, the prompt, the version control system for the prompt, loading credentials, loading the PGP passphrase, determining the next version after a release, and more.

## Requirements
 * sbt >= 0.13.0
 * scala >= 2.10.0

## Dependencies
The following are automatically added as dependencies of this plugin and as such, do not to be explicitly added to `./project/build.sbt` (if they appear, you can safely remove them -- it's assumed you want to reduce boilerplate and that you want the safety of using this plugin which deals with the idiosyncrasies of the [PGP][1] and [release][2] plugins already):
 * <a href="https://github.com/sbt/sbt-pgp" target="_blank">`"com.typesafe.sbt" % "sbt-pgp" % "0.8.1"`</a>
 * <a href="https://github.com/sbt/sbt-release" target="_blank">`"com.github.gseitz" % "sbt-release" % "0.8"`</a>

## Usage
### Adding the plugin dependency

Add the following line to `./project/build.sbt`:

`addSbtPlugin("org.scommon" % "sbt-settings-simple" % "0.0.1")`

**Please omit any references to the [PGP][1] and [release][2] plugins.**


### Project Templates

#### Single project
In your build.sbt where you would normally define a single-project configuration like:

```scala
lazy val my_project = Project(id = "my_project", base = file(".")) settings(
  ...
)
```

You can instead do:

```scala
import SimpleSettings._

lazy val my_project = root("my_project")
```

The base is assumed to be "." (in most projects it is). All other settings for publishing and releasing are automatically configured for you.

#### Multi-module project
So what if you have multiple modules that should be built and released together? Your configuration would resemble the following:

```scala
lazy val root = root(
    DO_NOT_PUBLISH_ARTIFACT //Do not try and publish the root project since it's 
                            //typically just a container for all of its modules.
                            //
  , core                    //These are aggregates.
  , io                      //When you want to build everything by default, specify the 
  , logging                 //list of projects here ("root" and "module" are just 
  , security                //bringing in defaults for standard SBT projects)
)

lazy val core          = module("core")

lazy val io            = module("io")
  .dependsOn(core    % "compile")

lazy val logging       = module("logging")
  .dependsOn(core    % "compile")

lazy val security      = module("security")
  .dependsOn(core    % "compile")
  .dependsOn(io      % "compile")
  .dependsOn(logging % "compile")
```

You can see that for now you're not well-insulated from SBT's project and dependency configuration. That may be addressed later.

Modules are assumed to be in a directory off the root directory with the same name as the provided ID. So if I have a module named `core` it should be in:

```
<root>
  +- core/
  |  +- build.sbt       (contains dependencies)
  |  `- src/
  +- projects.sbt       (contains project specification)
  `- settings.sbt       (contains simple settings)
```


### Tasks and settings
This plugin adds no tasks other than those provided by the [PGP][1] and [release][2] plugins themselves. It does however provide the following `settingKey`s that you can reference directly in your `.sbt` file after specifying `import SimpleSettings._`:
 * `primarySettings        := primary(...)`
 * `compilerSettings       := compiling(...)`
 * `mavenSettings          := maven(...)`
 * `publishSettings        := publishing(...)`
 * `releaseProcessSettings := releaseProcess(...)`
 * `promptSettings         := prompt(...)`

### Publishing
Publishing is no different than if you were to configure it manually yourself. Simply execute the 
normal SBT `publish` or `publish-local` task and you're good to go.

### Releasing
Releasing follows the normal [release plugin][2] process. There's no need to customize for the publishing of artifacts to sign them (since in v0.8 of the [release plugin][2] this is no longer done by default), this is taken care of for you. To enable this, specify:

```scala
publishSettings := publishing(
  signArtifacts = true
  ...
)
```

Artifacts are only signed during the release process. During snapshot publishing artifacts are ***not*** signed.

## Customizing

### Prompt
The prompt by default is configured for use with Git and out-of-the-box only supports Git. Customizing 
this for an alternative version control system will be defined later. But customizing the format of 
the prompt itself is very easy. By default it does the following:

```scala
"%s:%s:%s@%s> ".format(
  primaryName,        //As defined in primarySettings.name
  projectDisplayName, //As defined in the project template:
                      //    lazy val my_project = root("<DISPLAY NAME>")
  gitBranchName,      //Retrieved by running: git rev-parse --abbrev-ref HEAD
  gitShortenedCommit, //Retrieved by running: git rev-parse --short HEAD
)
```

This can be changed by specifying `promptSettings.format`. For example:

```scala
promptSettings := prompt(
    format = "name: %s, project: %s, branch: %s, commit:%s> "
)
```

### Behaviors
You can alter the default behavior if it does not suit your needs yet retain the rest of the functionality that this plugin provides. Each setting (except for `primarySettings`, `compilerSettings`, and `promptSettings`) provides a `behavior` parameter that allows you to completely override all the behaviors for that setting or only select ones. They're specified in the form of callbacks and are 
usually provided with an instance of a `default` implementation, the current `version` as a `String`, and an instance of `CoreSettings` that provides access to all settings defined through this plugin.

#### maven

##### `preAppendToPom` <br />
   Inserts additional XML into a maven-style POM during publishing before `appendToPom` is called.
   <br />

##### `appendToPom` <br />
   Overrides the normal logic that would serialize the SCM information and developers. *It is not 
   suggested that you customize this unless you have the need.*
   <br />

##### `postAppendToPom` <br />
   Inserts additional XML into a maven-style POM during publishing after `appendToPom` is called. 
   ***This is usually the best place to insert your own customizations*** (e.g. adding a parent POM).
   <br />

##### `appendDeveloper` <br />
   Transforms an instance of `MavenSettingsDeveloper` into XML. *It is not suggested that you customize
   this unless you have the need.*
   <br />

Example:

```scala
import scala.xml._

mavenSettings := maven(
  ...
  behavior = mavenBehavior(
   preAppendToPom = { (default, version, settings) =>
     NodeSeq.Empty
   },
   postAppendToPom = { (default, version, settings) =>
     <parent>
       <groupId>org.sonatype.oss</groupId>
       <artifactId>oss-parent</artifactId>
       <version>7</version>
     </parent>
   },
   appendDeveloper = { (default, developer, version, settings) =>
     //Delegate to the default developer serialization which transforms  
     //an instance of MavenSettingsDeveloper into XML.
     //Begs the question -- why did you define this at all??!
     default.appendDeveloper(developer, version, settings)
   }
  )
  ...
```

#### publish

##### `publishTo` <br />
   Examines the `version` `String` and provides the destination to publish an artifact to. Normally 
   this means publishing to the defined `publishSettings.snapshotRepository` for snapshots (which is 
   defined as a `version` `String` which ends with `"SNAPSHOT"`) or to 
   `publishSettings.releaseRepository` for releases (which is defined as anything that is *not* a 
   snapshot).
   <br />

##### `autoDiscoverRealm` <br />
   Sometimes publishing to a maven repository can be difficult when using Ivy. It may attempt to 
   use the wrong realm when doing BASIC authentication. By default this performs an HTTP POST to 
   the repository in an attempt to automatically discover the realm for later authentication 
   when publishing an artifact.
   <br />

##### `loadCredentials` <br />
   This looks for credentials to use when publishing an artifact. By default it will attempt to 
   load user names and associated passwords from `~/.sbt/.credentials`, `~/.ivy2/.credentials`, 
   and `~/.m2/settings.xml` with preference for duplicates in that same order.
   <br />

Example:

```scala
publishSettings := publishing(
  ...
  behavior = publishingBehavior(
    publishTo = { (default, version, settings) =>
      //Always publish here:
      "snapshots" at "http://my-company-nexus/content/repositories/snapshots"
    }
  )
  ...
)
```

#### releaseProcess

##### `shouldSignArtifacts` <br />
   Allows you to change the logic for determining if artifacts should be signed or not. By default 
   this is governed by the `publishSettings.signArtifacts` setting.
   <br />

##### `nextVersion` <br />
   Sets the next version to be used after a release. By default this bumps the bugfix number and 
   makes it a snapshot. It would take `"0.0.2"` and make the next version be `"0.0.3-SNAPSHOT"` 
   assuming it's currently releasing 0.0.2.
   <br />

##### `loadPGPPassphrase` <br />
   This loads the PGP passphrase that's needed when signing an artifact. By default it looks for 
   the PGP passphrase as a single line in `~/.sbt/.pgp`. If that does not exist it looks for an
   environment variable named `"PGP_PASSPHRASE"` and if it is not defined it finally opts for an
   empty string (`""`).
   <br />
   
##### `customReleaseProcess` <br />
   Produces a sequence of `sbtrelease.ReleaseStep` instances that specify the steps to perform during
   the release process. By default it simply delegates to `releaseProcessSettings.steps`. *It is not 
   suggested that you customize this unless you have the need.* Instead you should simply specify 
   `releaseProcessSettings.steps`.
   <br />

Example:

```scala
releaseProcessSettings := releaseProcess(
  steps = Seq(
      stepCheckSnapshotDependencies //
    , stepRunTest                   //
    , stepInquireVersions           //
    , stepSetReleaseVersion         //
    , stepCommitReleaseVersion      //Performs the initial git checks
    , stepTagRelease                //
    , stepPublishArtifacts          //Already customized to determine if artifacts
                                    //should be signed. Please see the following
                                    //for details:
                                    //  https://github.com/sbt/sbt-release/issues/49
    , stepSetNextVersion            //
    , stepCommitNextVersion         //
    , stepPushChanges               //Checks that an upstream branch is properly configured
  ),
  behavior = releaseProcessBehavior(
    shouldSignArtifacts = { (default, version, settings) =>
      //Always sign an artifact!
      //Why not just specify 'publishSettings := publishing(signArtifacts = true)' ??!
      true
    }
  )
)
```

## License
Copyright (c) 2013 David Hoyt

Published under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

[1]: https://github.com/sbt/sbt-pgp
[2]: https://github.com/sbt/sbt-release