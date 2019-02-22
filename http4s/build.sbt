//: ----------------------------------------------------------------------------
//: Copyright (C) 2017 Verizon.  All Rights Reserved.
//:
//:   Licensed under the Apache License, Version 2.0 (the "License");
//:   you may not use this file except in compliance with the License.
//:   You may obtain a copy of the License at
//:
//:       http://www.apache.org/licenses/LICENSE-2.0
//:
//:   Unless required by applicable law or agreed to in writing, software
//:   distributed under the License is distributed on an "AS IS" BASIS,
//:   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//:   See the License for the specific language governing permissions and
//:   limitations under the License.
//:
//: ----------------------------------------------------------------------------
import verizon.build._

enablePlugins(MetadataPlugin, ScalaTestPlugin)

libraryDependencies ++= Seq(
  dependencies.simulacrum.core,
  "io.argonaut"                %% "argonaut"                           % V.argonaut,
  "io.argonaut"                %% "argonaut-cats"                      % V.argonaut,
  "org.http4s"                 %% "http4s-argonaut"                    % V.http4s,
  "org.http4s"                 %% "http4s-blaze-client"                % V.http4s,
  "io.verizon.knobs"           %% "core"                               % V.knobs,
  "io.verizon.journal"         %% "core"                               % V.journal,
  "com.whisk"                  %% "docker-testkit-scalatest"           % V.dockerit % "test",
  "com.whisk"                  %% "docker-testkit-impl-spotify"        % V.dockerit % "test"
)

addCompilerPlugin(dependencies.kindprojector.plugin)

scalaModuleInfo := scalaModuleInfo.value map { _.withOverrideScalaVersion(true) }

addCompilerPlugin(dependencies.macroparadise.plugin)

initialCommands in console := """
import platypus._
import platypus.http4s._
"""

scalacOptions ++= List("-Ypartial-unification", "-Ywarn-value-discard")

scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

scalaTestVersion := "3.0.5"

scalaCheckVersion := "1.13.5"