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

import java.io.FileReader

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.Try
import org.apache.commons.csv._

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration()
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = new EasyDepositReportApp(configuration)

  val in = new FileReader("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties")

  val result: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(full @ commandLine.fullCmd) =>
      app.createFullReport()
      //Try { "command" }
      //Try {in.read().toString}
      //Try {in.read().toString}
      //Try {in.getEncoding}

    case Some(summary @ commandLine.summaryCmd) =>
      Try { "command" }
    case _ => throw new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")
      Try { "Unknown command" }

  }

  result.map(msg => Console.err.println(s"OK: $msg")).doIfFailure {
    case t => Console.err.println(s"ERROR: ${ t.getMessage }")
  }
}
