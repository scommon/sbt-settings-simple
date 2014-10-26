
//Relaxed semantic version parser that also allows optional qualifiers (e.g. "-SNAPSHOT").
//
//Modified from sbt-release plugin:
//  https://github.com/sbt/sbt-release/blob/master/src/main/scala/Version.scala

import scala.util.control.Exception._

object SemanticVersion {
  sealed trait Bump {
    def bump: SemanticVersion => SemanticVersion
  }

  object Bump {
    object Major extends Bump { def bump = _.bumpMajor }
    object Minor extends Bump { def bump = _.bumpMinor }
    object Bugfix extends Bump { def bump = _.bumpBugfix }
    object Next extends Bump { def bump = _.bump }

    val default = Next
  }

  val SemanticVersionR = """(\d+)\.(\d+)\.(\d+)(\-[.\w]+)?(\-.+)?""".r

  def apply(s: String): Option[SemanticVersion] = {
    allCatch opt {
      val SemanticVersionR(maj, min, mic, qual1, qual2) = s
      val optQual1 = Option(qual1)
      val optQual2 = Option(qual2)
      val prerelease = if (optQual2.isDefined) optQual1 else None
      val qual = optQual2.orElse(optQual1)
      SemanticVersion(maj.toInt, Option(min).map(_.toInt), Option(mic).map(_.toInt), prerelease, qual.filterNot(_.isEmpty))
    }
  }

  def versionFormatError =
    sys.error(s"Unrecognized version. Please ensure it is compatible with the semantic versioning specification (http://semver.org/) and this pattern: ${SemanticVersionR.pattern.toString}")
}

case class SemanticVersion(major: Int, minor: Option[Int], bugfix: Option[Int], prerelease: Option[String], qualifier: Option[String]) {
  def bump = {
    val maybeBumpedBugfix = bugfix.map(m => copy(bugfix = Some(m + 1)))
    val maybeBumpedMinor = minor.map(m => copy(minor = Some(m + 1)))
    lazy val bumpedMajor = copy(major = major + 1)

    maybeBumpedBugfix.orElse(maybeBumpedMinor).getOrElse(bumpedMajor)
  }

  def bumpMajor = copy(major = major + 1, minor = minor.map(_ => 0), bugfix = bugfix.map(_ => 0))
  def bumpMinor = copy(minor = minor.map(_ + 1), bugfix = bugfix.map(_ => 0))
  def bumpBugfix = copy(bugfix = bugfix.map(_ + 1))

  def bump(bumpType: SemanticVersion.Bump): SemanticVersion = bumpType.bump(this)

  def withoutQualifier = copy(qualifier = None)
  def asSnapshot = copy(qualifier = Some("-SNAPSHOT"))

  def string = "" + major + get(minor) + get(bugfix) + prerelease.getOrElse("") + qualifier.getOrElse("")

  private def get(part: Option[Int]) = part.map("." + _).getOrElse("")
}