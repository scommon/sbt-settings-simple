
import sbt._
import Keys._


//Unnamed package

import org.scommon.sbt.settings._

object BuildSettings {

  val defaults: Seq[sbt.Def.Setting[_]] = Seq(
      sbt.Keys.shellPrompt   := org.scommon.sbt.settings.promptSettings.value.prompt(
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
    , sbt.Keys.organization  := org.scommon.sbt.settings.primarySettings.value.organization
  )

}

