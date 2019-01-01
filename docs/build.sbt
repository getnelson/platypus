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

enablePlugins(DocsPlugin)

libraryDependencies += dependencies.simulacrum.core

addCompilerPlugin(dependencies.macroparadise.plugin)

addCompilerPlugin(dependencies.kindprojector.plugin)

scalacOptions += "-Ypartial-unification"

githubOrg := "getnelson"

githubRepoName := "platypus"

baseURL in Hugo := {
  if (isTravisBuild.value) new URI(s"https://getnelson.io/")
  else new URI(s"http://127.0.0.1:${previewFixedPort.value.getOrElse(1313)}/")
}

import com.typesafe.sbt.SbtGit.GitKeys.{gitBranch, gitRemoteRepo}

gitRemoteRepo := "git@github.com:getnelson/platypus.git"

includeFilter in Hugo := ("*.html" | "*.ico" | "*.jpg" | "*.svg" | "*.png" | "*.js" | "*.css" | "*.gif" | "CNAME")

minimumHugoVersion in Hugo := "0.48"

excludeFilter in Hugo := HiddenFileFilter
