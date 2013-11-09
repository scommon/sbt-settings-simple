

//Unnamed package

import org.scommon.sbt.settings._

trait CoreSettings {
  def primary       : PrimarySettings
  def prompt        : PromptSettings
  def compiler      : CompilerSettings
  def maven         : MavenSettings
  def publish       : PublishSettings
  def releaseProcess: ReleaseProcessSettings
}

object CoreSettings {
  def apply(
      primarySettings       : PrimarySettings
    , promptSettings        : PromptSettings
    , compilerSettings      : CompilerSettings
    , mavenSettings         : MavenSettings
    , publishSettings       : PublishSettings
    , releaseProcessSettings: ReleaseProcessSettings
  ): CoreSettings = new CoreSettings {
    def primary        = primarySettings
    def prompt         = promptSettings
    def compiler       = compilerSettings
    def maven          = mavenSettings
    def publish        = publishSettings
    def releaseProcess = releaseProcessSettings
  }
}