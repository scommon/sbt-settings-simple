
package org.scommon.sbt.settings {

  import CoreSettingsPlugin.SimpleSettings.ArtifactSpec
  import scala.collection._

  trait ScaladocSettings extends Settings {
    def options           : Traversable[String]
    def useAutoApiMappings: Boolean
    def baseApiUri        : String
    def apiMappings       : ScaladocApiMappingMap

    def behavior          : ScaladocBehavior
  }

  case class ApiMapping(organization: String, name: String, revision: String = "") extends ArtifactSpec

  trait ScaladocBehavior {
    import sbt.impl._
    import sbt.ModuleID
    import Utils._

    import scala.language.implicitConversions

    implicit def groupArtifactId2ApiMappingSpecification(spec: GroupArtifactID): ApiMapping = {
      val tricky = spec % "0.0.0"
      ApiMapping(tricky.organization, tricky.name)
    }

    implicit def moduleId2ApiMappingSpecification(spec: ModuleID): ApiMapping =
      ApiMapping(spec.organization, spec.name, spec.revision)

    implicit def groupArtifactId2ApiMappingSpecificationTuple2[T](spec: (GroupArtifactID, T)): (ApiMapping, T) =
      (groupArtifactId2ApiMappingSpecification(spec._1), spec._2)

    implicit def moduleId2ApiMappingSpecificationTuple2[T](spec: (ModuleID, T)): (ApiMapping, T) =
      (moduleId2ApiMappingSpecification(spec._1), spec._2)

    implicit def groupArtifactMap2ApiMappingSpecification(spec: immutable.Map[GroupArtifactID,String]): Map[ArtifactSpec, String] =
      spec map groupArtifactId2ApiMappingSpecificationTuple2

    implicit def moduleIdMap2ApiMappingSpecification(spec: immutable.Map[ModuleID,String]): Map[ArtifactSpec, String] =
      spec map moduleId2ApiMappingSpecificationTuple2

    implicit def string2ScaladocApiMappingUri(uri: String): ScaladocApiMappingUri =
      _ => uri

    def generateApiMappings(fullClasspath: Seq[(ArtifactSpec, sbt.File)], settings: CoreSettings): immutable.Map[sbt.File, sbt.URL] = {
      val mappings: ScaladocApiMappingMap = settings.scaladocs.apiMappings

      val result =
        for {
          (mapping, fnMappingUri) <- mappings
          (module, file) <- fullClasspath
          if ( mapping.isRevisionSpecified && module.organization == mapping.organization && module.name.startsWith(mapping.name) && module.revision == mapping.revision) ||
             (!mapping.isRevisionSpecified && module.organization == mapping.organization && module.name.startsWith(mapping.name))
          uri <- optionUrl(fnMappingUri(module))
        } yield file -> uri

      result.toMap
    }
  }
}


//Unnamed package

import scala.collection._
import org.scommon.sbt.settings._

object ScaladocBehavior extends ScaladocBehavior

object ScaladocSettings {
  import CoreSettingsPlugin.SimpleSettings.ArtifactSpec
  import Utils._

  val useAutoApiMappings =
    sbt.Keys.autoAPIMappings :=
      org.scommon.sbt.settings.scaladocSettings.value.useAutoApiMappings

  val baseApiUri =
    sbt.Keys.apiURL := {
      val scaladocSettings = org.scommon.sbt.settings.scaladocSettings.value
      if ((scaladocSettings.baseApiUri ne null) && "" != scaladocSettings.baseApiUri)
        optionUrl(scaladocSettings.baseApiUri)
      else
        None
    }

  val apiMappings =
    sbt.Keys.apiMappings in sbt.Global ++= {
      val fullClasspath = (sbt.Keys.fullClasspath in sbt.Compile).value
      val fullClasspathMapped: Seq[Option[(ArtifactSpec, sbt.File)]] = fullClasspath map { entry: sbt.Attributed[sbt.File] =>
        entry.get(sbt.Keys.moduleID.key) map (moduleId => (ApiMapping(moduleId.organization, moduleId.name, moduleId.revision), entry.data))
      }
      val cleaned = fullClasspathMapped collect {
        case Some(entry) => entry
      }
      delegateGenerateApiMappings(cleaned, CoreSettings(
          org.scommon.sbt.settings.primarySettings.value
        , org.scommon.sbt.settings.promptSettings.value
        , org.scommon.sbt.settings.compilerSettings.value
        , org.scommon.sbt.settings.scaladocSettings.value
        , org.scommon.sbt.settings.mavenSettings.value
        , org.scommon.sbt.settings.publishSettings.value
        , org.scommon.sbt.settings.releaseProcessSettings.value
      ))
    }

  val options =
    sbt.Keys.scalacOptions in (sbt.Compile, sbt.Keys.doc) ++=
      org.scommon.sbt.settings.scaladocSettings.value.options.toSeq

  val defaults =
    Seq(
        options
      , useAutoApiMappings
      , baseApiUri
      , apiMappings
    )

  private[this] def delegateGenerateApiMappings(fullClasspath: Seq[(ArtifactSpec, sbt.File)], settings: CoreSettings): immutable.Map[sbt.File, sbt.URL] =
    settings.scaladocs.behavior.generateApiMappings(fullClasspath, settings)

}