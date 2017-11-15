/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.report

import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand }

import scala.language.reflectiveCalls

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))
  printedName = "easy-deposit-report"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""Creates report about the deposits in the deposit area."""
  val synopsis: String =
    s"""
       |  $printedName (full <depositor>)
       |  $printedName (summary <depositor>)
       |  $printedName  """.stripMargin

  version(s"$printedName v${ configuration.version }")
  banner(
    s"""
       |  $description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |
       |""".stripMargin)

  val fullCmd3: Subcommand = new Subcommand("full") {

    val depos: ScallopOption[List[String]] = trailArg[List[String]]("depositor")

    footer(SUBCOMMAND_SEPARATOR)

  }
  addSubcommand(fullCmd3)


  val summaryCmd3: Subcommand = new Subcommand("summary") {

    val depos: ScallopOption[List[String]] = trailArg[List[String]]("depositor")

    footer(SUBCOMMAND_SEPARATOR)

  }
  addSubcommand(summaryCmd3)

  verify()

  footer("")

}

