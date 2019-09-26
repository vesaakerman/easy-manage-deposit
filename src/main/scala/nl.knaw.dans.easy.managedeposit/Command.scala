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
package nl.knaw.dans.easy.managedeposit

import java.nio.file.Paths

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.rogach.scallop.ScallopOption

import scala.annotation.tailrec
import scala.io.StdIn
import scala.language.reflectiveCalls
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration)
  val app = new EasyManageDepositApp(configuration)

  private def checkCleanArguments(doUpdate: Boolean, dataOnly: Boolean, newStateLabel: ScallopOption[String], newStateDescription: ScallopOption[String]): Boolean = {
    var result = true
      if (newStateLabel.isSupplied || newStateDescription.isSupplied) {
        if (!dataOnly) {
          println("--newStateLabel and --newStateDescription can be given only when also --data-only is selected")
          result = false
        }
        if (newStateLabel.isSupplied && !newStateDescription.isSupplied) {
          println("When --newStateLabel is given, also --newStateDescription has to be given")
          result = false
        }
        else if (!newStateLabel.isSupplied && newStateDescription.isSupplied) {
          println("When --newStateDescription is given, also --newStateLabel has to be given")
          result = false
        }
      }
    if (!doUpdate)
      println("--do-update was not selected, and therefore the actual deleting of data does not take place")
    result
  }

  @tailrec
  private def cleanInteraction(): Boolean = {
    StdIn.readLine("This action will delete data from the deposit area. OK? (y/n): ") match {
      case "y" => true
      case "n" => false
      case _ =>
        println("Please enter a valid char : y or n")
        cleanInteraction()
    }
  }

  val result: Try[FeedBackMessage] = commandLine.subcommands match {
    case commandLine.reportCmd :: (full @ commandLine.reportCmd.fullCmd) :: Nil =>
      app.createFullReport(full.depositor.toOption, full.age.toOption)
    case commandLine.reportCmd :: (summary @ commandLine.reportCmd.summaryCmd) :: Nil =>
      app.summary(summary.depositor.toOption, summary.age.toOption)
    case commandLine.reportCmd :: (error @ commandLine.reportCmd.errorCmd) :: Nil =>
      app.createErrorReport(error.depositor.toOption, error.age.toOption)
    case (clean @ commandLine.cleanCmd) :: Nil =>
      Console.out.println(s"${ if(clean.doUpdate()) "Deleting" else "To be deleted" } ${ if(clean.dataOnly()) "data from " else "" }deposits with state ${clean.state()}${ if(clean.newStateLabel.isSupplied) ", replacing with state "  else ""}${clean.newStateLabel.getOrElse("")} for ${clean.depositor.toOption.getOrElse("all users")}")
      if (clean.force() || cleanInteraction())
        app.cleanDepositor(clean.depositor.toOption, clean.keep(), clean.state(), clean.dataOnly(), clean.doUpdate(), clean.newStateLabel, clean.newStateDescription, clean.output())
      else
        Try { "Clean operation aborted by user" }
    case (syncFedora @ commandLine.`syncFedoraState`) :: Nil =>
      app.syncFedoraState(syncFedora.easyDatasetId())
    case _ => Failure(new IllegalArgumentException("Enter a valid subcommand"))
  }

  result.doIfSuccess(msg => Console.err.println(s"OK: $msg"))
    .doIfFailure { case t =>
      Console.err.println(s"ERROR: ${ t.getClass.getSimpleName }: ${ t.getMessage }")
      logger.error("A fatal exception occurred", t)
      System.exit(1)
    }
}

