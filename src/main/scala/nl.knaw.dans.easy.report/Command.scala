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

import java.nio.file.Paths

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.Try


object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration)
  val app = new EasyDepositReportApp(configuration)
  val result: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(full @ commandLine.fullCmd) =>
      app.createFullReport(full.depositor.toOption)
    case Some(summary @ commandLine.summaryCmd) =>
      app.summary(summary.depositor.toOption)
    case _ => Try { s"Unknown command: ${ commandLine.subcommand }" }
  }

  result.doIfSuccess(msg => Console.err.println(s"OK: $msg"))
    .doIfFailure { case t =>
      Console.err.println(s"ERROR: ${ t.getClass.getSimpleName }: ${ t.getMessage }")
      logger.error("A fatal exception occurred", t)
      System.exit(1)
    }
}

