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
  printedName = "easy-manage-deposit"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""Creates report about the deposits in the deposit area."""
  val synopsis: String =
    s"""
       |  $printedName report full [<depositor>]
       |  $printedName report summary [<depositor>]
     """.stripMargin

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

  val reportCmd = new Subcommand("report") {

    val fullCmd = new Subcommand("full") {
      val depositor: ScallopOption[DepositorId] = trailArg("depositor", required = false)
      footer(SUBCOMMAND_SEPARATOR)
    }
    addSubcommand(fullCmd)

    val summaryCmd = new Subcommand("summary") {
      val depositor: ScallopOption[DepositorId] = trailArg("depositor", required = false)
      footer(SUBCOMMAND_SEPARATOR)
    }
    addSubcommand(summaryCmd)
  }
  addSubcommand(reportCmd)

  footer("")
  verify()
}

