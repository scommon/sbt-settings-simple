
package org.scommon.sbt.settings {
  trait PrimarySettings extends Settings {
    def name            : String
    def companyName     : String
    def organization    : String
    def homepage        : String
    def vcsSpecification: String
  }
}


//Unnamed package

import org.scommon.sbt.settings._
