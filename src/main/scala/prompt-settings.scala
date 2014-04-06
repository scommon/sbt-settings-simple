
import sbt._

package org.scommon.sbt.settings {
  trait PromptSettings extends Settings {
    def prompt        : TerminalPrompt
    def versionControl: VersionControl
    def format        : String
  }
}


//Unnamed package

import org.scommon.sbt.settings._

trait TerminalPrompt {
  def apply(settings: CoreSettings): (sbt.State => String)
}

object PromptSettings {
  object Default extends TerminalPrompt {
    override def toString = s"<default prompt settings>"

    def apply(settings: CoreSettings): (sbt.State => String) = (state: sbt.State) => {
      val extracted = sbt.Project.extract(state)

      val sbt_project_name =
        extracted.currentRef.project

      val project_prompt_name =
        extracted
          .getOpt(AdditionalSettings.projectPromptName)
          .getOrElse("")

      val branch = try settings.prompt.versionControl.branchName()
                   catch { case _: Throwable => println(s"Unable to determine the repository branch: ${settings.prompt.versionControl.help}"); VersionControl.Default.branchName() }
      val commit = try settings.prompt.versionControl.shortenedCurrentCommit()
                   catch { case _: Throwable => println(s"Unable to determine the repository commit: ${settings.prompt.versionControl.help}"); VersionControl.Default.shortenedCurrentCommit() }

      val project_name =
        if ("" == project_prompt_name)
          sbt_project_name
        else
          project_prompt_name

      val curr_branch =
        if ("" == branch)
          "master"
        else
          branch

      val curr_commit =
        if ("" == commit)
          "unknown"
        else
          commit

      //"%s:%s:%s@%s> "
      //s"${settings.primary.name}:$project_name:$curr_branch@$curr_commit> "

      settings.prompt.format.format(settings.primary.name, project_name, curr_branch, curr_commit)
    }
  }
}
