
sbtPlugin := true

name := "sbt-settings-simple"

organization := "org.scommon"




scalaVersion := "2.10.3"



scalacOptions := Seq("-deprecation", "-unchecked")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")


//crossBuildingSettings

