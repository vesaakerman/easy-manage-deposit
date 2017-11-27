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

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.{ Failure, Success, Try }


object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration()
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = new EasyDepositReportApp(configuration)


  val result: Try[FeedBackMessage] = commandLine.subcommand match {

    case Some(full @ commandLine.fullCmd) =>

      val depositorId: Option[List[String]] = commandLine.fullCmd.trailArg[List[String]]("depositor").toOption
      var matchArg: String = "optionIsEmpty"

      //app.tryGetPath("easy-ingest-flow-inbox")
      //app.tryGetPath("easy-sword2")

      if (depositorId.isDefined) {
        app.createFullReport(depositorId.get.head)
        matchArg = depositorId.get.head.toString
      }
      if (depositorId.isEmpty) {
        app.createFullReport(matchArg.toString)
      }

      app.tryCreateFullReport("easy-ingest-flow-inbox", matchArg)
      app.tryCreateFullReport("easy-sword2", matchArg)


      Try { "full report " + matchArg } match {
        case Failure(_) => Try { "failure: Full report easy-ingest-flow-inbox + easy-sword2 " + matchArg }
        case Success(_) => Try { "success: Full report easy-ingest-flow + easy-sword2 " + matchArg }
      }


    case Some(summary @ commandLine.summaryCmd) =>

      val depositorId: Option[List[String]] = commandLine.summaryCmd.trailArg[List[String]]("depositor").toOption
      var matchArg: String = "optionIsEmpty"

      if (depositorId.isDefined) {
        app.createSummaryReport(depositorId.get.head)
        matchArg = depositorId.get.head.toString
      }
      if (depositorId.isEmpty) {
        app.createSummaryReport(matchArg.toString)
      }

      Try { "summary report " + matchArg } match {
        case Failure(_) => Try { "failure: Summary report easy-ingest-flow-inbox + easy-sword2 " + matchArg }
        case Success(_) => Try { "success: Summary report easy-ingest-flow + easy-sword2 " + matchArg }
      }

    case _ =>
      Try { "" } match {
        case Failure(_) => Try { "failure: ?" }
        case Success(_) => Try { "unknown command" }
      }


  }

  result.map(msg => Console.err.println(s"OK: $msg")).doIfFailure {
    case t => Console.err.println(s"ERROR: ${ t.getMessage }")
  }


}
