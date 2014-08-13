# SBT-Settings-Simple

This plugin provides an easier to use and configure interface into SBT which allows you to largely 
ignore having to learn about SBT tasks and settings and to instead focus on delivering your work.

Its goal is to simply *lower the barrier of entry for new SBT adopters.*

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

scalaVersion := "2.10.4"

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

apiMappings ++= {
  def findManagedDependency(organization: String, name: String): Option[File] = {
    (for {
      entry <- (fullClasspath in (Compile)).value
      module <- entry.get(moduleID.key) if module.organization == organization && module.name.startsWith(name)
    } yield entry.data).headOption
  }
  val links: Seq[Option[(File, URL)]] = Seq(
    findManagedDependency("com.typesafe", "config").map(d => d -> url("http://typesafehub.github.io/config/latest/api/")),
    findManagedDependency("com.typesafe.akka", "akka-actor").map(d => d -> url(s"http://doc.akka.io/api/akka/2.3.3/")),
    findManagedDependency("org.scalacheck", "scalacheck").map(d => d -> url(s"http://rickynils.github.io/scalacheck/api-1.10.1/"))
  )
  links.collect { case Some(d) => d }.toMap
}

scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits")

autoAPIMappings := true

apiURL := Some(url("http://scommon.github.io/sbt-settings-simple/api/"))

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
    checkSnapshotDependencies       //
  , runTest                         //
  , inquireVersions                 //
  , setReleaseVersion               //
  , commitReleaseVersion            //performs the initial git checks
  , tagRelease                      //
  , publishArtifacts.copy(action =  //uses publish-signed instead of publish if configured.
      publishSignedAction
    )
  , setNextVersion                  //
  , commitNextVersion               //
  , pushChanges                     //also checks that an upstream branch is properly configured
)
```

And turns it into:

```scala
import SimpleSettings._

primarySettings in Global := primary(
    name             = "sbt-settings-simple"
  , companyName      = "scommon"
  , organization     = "org.scommon"
  , homepage         = "https://github.com/scommon/sbt-settings-simple"
  , vcsSpecification = "git@github.com:scommon/sbt-settings-simple.git"
)

compilerSettings in Global := compiling(
    scalaVersion  = "2.11.2"
  , scalacOptions = Seq("-deprecation", "-unchecked")
)

scaladocSettings in Global := scaladocs(
    options            = Seq("-groups", "-implicits")
  , useAutoApiMappings = true
  , baseApiUri         = "http://scommon.github.io/sbt-settings-simple/api/"
  , apiMappings        = Map(
      "com.typesafe"        % "config"      -> "http://typesafehub.github.io/config/latest/api/"
    , "com.typesafe.akka"   % "akka"        -> {spec: ArtifactSpec => s"http://doc.akka.io/api/akka/${spec.revision}/"}
    , "org.scalacheck"     %% "scalacheck"  -> "http://scalacheck.org/files/scalacheck_2.11-1.11.4-api/"
  )
)

mavenSettings in Global := maven(
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

publishSettings in Global := publishing(
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
 * Supports cross compiling out of the box

## Deficiencies
 * Creating your own project template is confusing
 * Need to provide easier access to the SBT project settings so it's easier to extend and compose

## Requirements
 * SBT >= 0.13.5

## Dependencies
The following are automatically added as dependencies of this plugin and as such, are not to be explicitly added to `./project/build.sbt` (if they appear, you can safely remove them -- it's assumed you want to reduce boilerplate and that you want the safety of using this plugin which deals with the idiosyncrasies of the [PGP][1] and [release][2] plugins already):
 * <a href="https://github.com/sbt/sbt-pgp" target="_blank">`"com.typesafe.sbt" % "sbt-pgp" % "0.8.2"`</a>
 * <a href="https://github.com/sbt/sbt-release" target="_blank">`"com.github.gseitz" % "sbt-release" % "0.8.3"`</a>

## Usage
### Adding the plugin dependency

Add the following line to `./project/build.sbt`:

`addSbtPlugin("org.scommon" % "sbt-settings-simple" % "0.0.4")`

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

lazy val my_project = module("my_project")(".")
```

All settings for publishing and releasing are automatically configured for you.

#### Multi-module project
So what if you have multiple modules that should be built and released together? Your configuration would resemble the following:

```scala
lazy val root          = module("all")(
    base = "."
  , publish = false                             //Do not try and publish the root project since it's
                                                //typically just a container for all of its modules.

  , aggregate = Seq(
        core                                    //These are aggregates.
      , io                                      //When you want to build everything by default, specify the
      , logging                                 //list of projects here ("module" is just bringing in
      , security                                //defaults for standard SBT projects).
    )
)

lazy val core          = module("core")("core") //Module returns a standard project
                                                //reference which can be used like
                                                //any normal SBT project ref.

lazy val io            = module("io")(          //This is an alternative way of specifying
    base = "io"                                 //dependencies and other information such
  , dependencies = Seq(                         //as id, aggregates, and settings.
      core % "compile"
    )
)

lazy val logging       = Project(               //You can easily integrate with existing project
    id = "core"                                 //definitions using the simpleSettings() method
  , base = file("core")                         //which provides a Seq[Setting[_]] instance.
  , settings = simpleSettings(prompt = "core", publish = true)
  , dependencies = Seq(core % "compile")
)

lazy val security      = module("security")("security")
  .dependsOn(core    % "compile")
  .dependsOn(io      % "compile")
  .dependsOn(logging % "compile")
```

You can see that for now you're not well-insulated from SBT's project and dependency configuration. That may be addressed later.

Modules can be in any directory off the root directory, but normally they're in a subdirectory directly off the root. So if I have a module named `core` it should be in:

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
 * `compilerSettings       := compiling(...)/crossCompiling(...)`
 * `scaladocSettings       := scaladocs(...)`
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

### Cross Compiling
Adding support to target multiple Scala versions is easy. If you have a similar section in your build:

```scala
compilerSettings in Global := compiling(
    scalaVersion  = "2.11.2"
  , scalacOptions = Seq("-deprecation", "-unchecked")
)
```

change it to:

```scala
compilerSettings in Global := crossCompiling(
  default = "2.11.2",
  crossScala(
      scalaVersion  = "2.10.4"
    , scalacOptions = Seq("-deprecation", "-unchecked")
  ),
  crossScala(
      scalaVersion = "2.11.2"
    , scalacOptions = Seq("-deprecation", "-unchecked")
  )
)
```

The `default` version is optional. If it is omitted, the first instance of `crossScala(...)` will be used as the 
default. The default will be what is used during normal, non-cross-compiled work.

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

#### scaladocs

##### `generateApiMappings` <br />
   Examines a project's dependencies (both managed and unmanaged) and returns a mapping from a jar
   to a URL that scaladoc uses to reference external APIs. The default implementation looks for
   dependencies with the provided group ID and that start with the name of the provided artifact
   ID. The version number is typically ignored but if provided will be used.
   <br />

Example:

```scala
scaladocSettings := scaladocs(
  ...
  behavior = scaladocBehavior(
    generateApiMappings = { (default, classpath, settings) =>
      //Every artifact will use the same uber scaladoc API URL.
      classpath map {
        case (spec, jar) => jar -> url("http://example.org/uber/api/")
      }.toMap
    }
  )
  ...
)
```

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
   load user names and associated passwords by first looking at the PUBLISH_USER and PUBLISH_PASSWORD
   environment variables and then from `~/.sbt/.credentials`, `~/.ivy2/.credentials`,
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
//The following import is important.
//
//It provides aliases for the release plugin's release steps
//as well as an overridden one ("stepPublishArtifacts") that
//takes into account settings specifying if you want to
//sign artifacts or not.

import SimpleSettings.ReleaseSteps._

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
Copyright (c) 2013-2014 David Hoyt

Published under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

[1]: https://github.com/sbt/sbt-pgp
[2]: https://github.com/sbt/sbt-release
