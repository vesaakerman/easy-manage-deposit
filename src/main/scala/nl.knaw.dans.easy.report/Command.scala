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
  //val app2 = new EasyDepositReportApp2(configuration)

  /* ------------- List of subdirectories in easy-ingest-flow-inbox -------------------- */
  /*def getListOfSubDirectories(directoryName: String): Array[String] = {
    (new File(directoryName))
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName)
  }
  val dirs = getListOfSubDirectories("./data/easy-ingest-flow-inbox")*/
  //val dirs = getListOfSubDirectories("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox")
  /* ------------------------------------------------------------------------------------ */

  //val in = new FileReader("./data/easy-ingest-flow-inbox/ ")

  //for (i <- dirs) {
  //  val in = new FileReader("./data/easy-ingest-flow-inbox/" + i + "/deposit.properties")
  //}

  val result: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(full @ commandLine.fullCmd) =>
      app.createFullReport()
      Try {"full report"}

    case Some(summary @ commandLine.summaryCmd) =>
       app.createSummaryReport()
       Try {"summary report"}

    case _ => throw new IllegalArgumentException(s"Unknown command: ${commandLine.subcommand}")
      Try { "Unknown command" }

  }


  //val result2: Try[FeedBackMessage] = commandLine.subcommand match {
      //case Some(full @ commandLine.fullCmd) =>
      // app2.createFullReport()
      // Try {"full report"}

      //case Some(summary @ commandLine.summaryCmd) =>
      //app2.createSummaryReport()
      //Try {"summary report"}

   // case _ => throw new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")
   //   Try { "Unknown command" }

  //}
  def readTextFile(filename: String): Try[List[String]] = {
    Try{Source.fromFile(filename).getLines.toList}
  }

  def readCsvFile(filename2: String): Try[Unit] = {
    Try{Source.fromFile(filename2).getLines.map(_.split(","))}
  }

  val filename = "./mendeleyReport2_Summary.txt"
  readTextFile(filename) match {
    case Success(lines) => lines.foreach(println)
    case Failure(f) => println(f)
  }

  /*val filename2 = "./mendeleyReport2.csv"
  readCsvFile(filename2) match {
    case Success(lines) => ??????????????
    case Failure(f) => println(f)
  }*/

  //Console.println(result)
  //Console.println(app.createFullReport())

  result.map(msg => Console.err.println(s"OK: $msg")).doIfFailure {
    case t => Console.err.println(s"ERROR: ${ t.getMessage }")
  }

  //result2.map(msg => Console.err.println(s"OK: $msg")).doIfFailure {
  //  case t => Console.err.println(s"ERROR: ${ t.getMessage }")
  // }

}
