
sbtPlugin := true

name := "sbt-settings-simple"

organization := "org.scommon"




scalaVersion := "2.10.4"



scalacOptions := Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-target:jvm-1.7")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

releaseSettings

//crossBuildingSettings

