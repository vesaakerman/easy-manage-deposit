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

import java.io.{File, FileReader}

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.csv._

import scala.io.Source
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration()
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = new EasyDepositReportApp(configuration)

  val result: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(fullS @ commandLine.fullCmd) =>
      app.createFullReportSword2()
      Try {"full report easy-sword2"} match {
        case Failure(_) => Try{"failure: Full report easy-sword2"}
        case Success(_) => Try{"success: Full report easy-sword2"}
      }
    case Some(fullI @ commandLine.fullCmd2) =>
      app.createFullReportEasyIngestFlowInbox()
      Try {"full report easy-ingest-flow-inbox"} match {
        case Failure(_) => Try{"failure: Full report easy-ingest-flow-inbox"}
        case Success(_) => Try{"success: Full report easy-ingest-flow-inbox"}
      }
    case Some(full @ commandLine.fullCmd3) =>
      app.createFullReport()
      Try {"full report"} match {
        case Failure(_) => Try{"failure: Full report easy-ingest-flow-inbox + easy-sword2"}
        case Success(_) => Try{"success: Full report easy-ingest-flow + easy-sword2"}
      }

    case Some(summaryS @ commandLine.summaryCmd) =>
      app.createSummaryReportSword2()
      Try {"summary report easy-sword2"} match {
        case Failure(_) => Try{"failure: Summary report easy-sword2"}
        case Success(_) => Try{"success: Summary report easy-sword2"}
      }
    case Some(summaryI @ commandLine.summaryCmd2) =>
      app.createSummaryReportEasyIngestFlowInbox()
      Try {"summary report easy-ingest-flow-inbox"} match {
        case Failure(_) => Try{"failure: Summary report easy-ingest-flow-inbox"}
        case Success(_) => Try{"success: Summary report easy-ingest-flow-inbox"}
      }
    case Some(summary @ commandLine.summaryCmd3) =>
       app.createSummaryReport()
       Try {"summary report"} match {
         case Failure(_) => Try {"failure: Summary report easy-ingest-flow-inbox + easy-sword2"}
         case Success(_) => Try {"success: Summary report easy-ingest-flow-inbox + easy-sword2"}
       }

    case _ => //throw new IllegalArgumentException(s"Unknown command: ${commandLine.subcommand}")
         //Try {commandLine.subcommand.toString} match {
         //  case Failure(_) => Try{"failure: ?"}
         //  case Success(_) => Try{"unknown command"}
         //}
         Try {""} match {
            case Failure(_) => Try{"failure: ?"}
            case Success(_) => Try{"unknown command"}
         }
  }



  //Console.println(result)
  //Console.println(app.createFullReport())

  result.map(msg => Console.err.println(s"OK: $msg")).doIfFailure {
   case t => Console.err.println(s"ERROR: ${ t.getMessage }")
  }



}
