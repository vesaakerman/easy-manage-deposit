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

import java.io.File
import java.nio.file._
import java.text.SimpleDateFormat
import java.util.Calendar

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.csv.{ CSVFormat, CSVPrinter }

import scala.collection.Map
import scala.util.{ Failure, Success, Try }

class EasyDepositReportApp(configuration: Configuration) extends DebugEnhancedLogging {

  val csvFormat: CSVFormat = CSVFormat.RFC4180.withHeader("DEPOSITOR", "DEPOSIT_ID", "DEPOSIT_STATE", "DOI", "DEPOSIT_CREATION_TIMESTAMP", "DEPOSIT_UPDATE_TIMESTAMP", "DESCRIPTION", "NBR_OF_CONTINUED_DEPOSITS").withDelimiter(',')
  val out: Appendable = new StringBuffer()
  val printer: CSVPrinter = csvFormat.print(out)
  val out2: Appendable = new StringBuffer()

  def getListOfSubDirectories(directoryName: String): Array[String] = {
    new File(directoryName)
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName)
  }

  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    }
    else {
      List[File]()
    }
  }

  def getPath(dir: String): Path = {
    Paths.get(configuration.properties.getString(dir))
  }


  var nbrOfContinued: Int = 0
  var pathname: String = "default"
  var pathname2: String = "default"
  var dirPath: Path = _
  var dirList: List[String] = _
  var fileList: List[File] = _
  var file2: File = _

  var nbrDepositorsMap: Map[String, Int] = Map("default" -> 0)
  var nbrDraftsMap: Map[String, Int] = Map("default" -> 0)
  var nbrSubmittedMap: Map[String, Int] = Map("default" -> 0)
  var nbrArchivedMap: Map[String, Int] = Map("default" -> 0)
  var nbrFinalizingMap: Map[String, Int] = Map("default" -> 0)
  var nbrRejectedMap: Map[String, Int] = Map("default" -> 0)
  var nbrInvalidMap: Map[String, Int] = Map("default" -> 0)
  var nbrFailedMap: Map[String, Int] = Map("default" -> 0)

  var spaceDepositorsMap: Map[String, Long] = Map("default" -> 0)
  var spaceDraftsMap: Map[String, Long] = Map("default" -> 0)
  var spaceSubmittedMap: Map[String, Long] = Map("default" -> 0)
  var spaceArchivedMap: Map[String, Long] = Map("default" -> 0)
  var spaceFinalizingMap: Map[String, Long] = Map("default" -> 0)
  var spaceRejectedMap: Map[String, Long] = Map("default" -> 0)
  var spaceInvalidMap: Map[String, Long] = Map("default" -> 0)
  var spaceFailedMap: Map[String, Long] = Map("default" -> 0)


  def createReport(dir: String, depositorId: String): String = {
    val dep = depositorId
    dirPath = getPath(dir)
    dirList = getListOfSubDirectories(getPath(dir).toString).toList
    dirList.foreach { i =>
      pathname = dirPath.toString + "/" + i + "/deposit.properties"
      pathname2 = dirPath.toString + "/" + i
      fileList = getListOfFiles(pathname2)

      Try { getListOfFiles(pathname2) }
      match {
        case Failure(_) => println("Failure : getListOfFiles. Files in the directory with the path " + pathname2 + "cannot be listed. Check the pathname")
        case Success(_) => logger.info("Success : getListOfFiles")
      }

      val config = new PropertiesConfiguration(pathname)

      Try { config }
      match {
        case Failure(_) => println("Failure : config ")
        case Success(_) => logger.info("Success : config ")
      }

      file2 = new File(pathname2)
      nbrOfContinued = 0

      if (config.getString("depositor.userId").contentEquals(dep) || (dep == "optionIsEmpty")) {

        Try { (config.getString("depositor.userId").contentEquals(dep) || (dep == "optionIsEmpty")).toString.nonEmpty }
        match {
          case Failure(_) => println("Failure : DepositorId. Make sure including the DepositorId arg when you call createReport function ")
          case Success(_) => logger.info("Success : DepositorId: " + dep)
        }

        fileList.foreach { j =>
          if (j.getName.contains("zip")) {
            Try { j.getName.contains("zip") }
            match {
              case Failure(_) => println("Failure : j.getName.contains(). Function fails to create a value ")
              case Success(_) => logger.info("Success : j.getName.contains(). Function does not fail ")
            }
            nbrOfContinued = nbrOfContinued + 1
          }
        }

        val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

        Try { config.getString("depositor.userId") }
        match {
          case Failure(_) => println("Failure : config.getString(\"depositor.userId\") ")
          case Success(_) => logger.info("Success : config.getString(\"depositor.userId\") ")
        }
        Try { config.getString("bag-store.bag-id") }
        match {
          case Failure(_) => println("Failure : config.getString(\"bag-store.bag-id\") ")
          case Success(_) => logger.info("Success : config.getString(\"bag-store.bag-id\") ")
        }
        Try { config.getString("state.label") }
        match {
          case Failure(_) => println("Failure : config.getString(\"state.label\") ")
          case Success(_) => logger.info("Success : config.getString(\"state.label\") ")
        }
        Try { config.getString("identifier.doi") }
        match {
          case Failure(_) => println("Failure : config.getString(\"identifier.doi\") ")
          case Success(_) => logger.info("Success : config.getString(\"identifier.doi\") ")
        }
        Try { config.getString(sdf.format(file2.lastModified())) }
        match {
          case Failure(_) => println("Failure : sdf.format(file2.lastModified()) ")
          case Success(_) => logger.info("Success : sdf.format(file2.lastModified())", sdf.format(file2.lastModified()))
        }
        Try { config.getString("state.description") }
        match {
          case Failure(_) => println("Failure : config.getString(\"state.description\") ")
          case Success(_) => logger.info("Success : config.getString(\"state.description\") ")
        }

        printer.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinued.toString)

        Try {
          printer.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinued.toString)
        }
        match {
          case Failure(_) => println("Failure : printer.printRecord. stdout fails ")
          case Success(_) => logger.info("Success : printer.printRecord. stdout does not fail ")
        }

        if (nbrDepositorsMap.contains(config.getString("depositor.userId"))) {
          for ((k, v) <- nbrDepositorsMap) {
            if (k.contentEquals(config.getString("depositor.userId"))) {
              val new_v = (k, v)._2 + 1
              nbrDepositorsMap = nbrDepositorsMap - k
              nbrDepositorsMap = nbrDepositorsMap + (k -> new_v)
            }
          }
        }
        if (nbrDepositorsMap.contains(config.getString("depositor.userId")).equals(false)) {
          nbrDepositorsMap = nbrDepositorsMap + (config.getString("depositor.userId") -> 1)
        }
        if (nbrDepositorsMap.contains("default")) {
          nbrDepositorsMap = nbrDepositorsMap - "default"
        }

        if (spaceDepositorsMap.contains(config.getString("depositor.userId"))) {
          for ((k, v) <- spaceDepositorsMap) {
            if (k.contentEquals(config.getString("depositor.userId"))) {
              val new_v = (k, v)._2 + file2.length()
              spaceDepositorsMap = spaceDepositorsMap - k
              spaceDepositorsMap = spaceDepositorsMap + (k -> new_v)
            }
          }
        }
        if (spaceDepositorsMap.contains(config.getString("depositor.userId")).equals(false)) {
          spaceDepositorsMap = spaceDepositorsMap + (config.getString("depositor.userId") -> file2.length())
        }
        if (spaceDepositorsMap.contains("default")) {
          spaceDepositorsMap = spaceDepositorsMap - "default"
        }

        def countStateLabels(stateLabel: String, mapIns: Map[String, Int]): Map[String, Int] = {
          var map: Map[String, Int] = mapIns
          if (config.getString("state.label").equals(stateLabel)) {
            if (map.contains(config.getString("depositor.userId"))) {
              for ((k, v) <- map) {
                if (k.contentEquals(config.getString("depositor.userId"))) {
                  val new_v = (k, v)._2 + 1
                  map = map - k
                  map = map + (k -> new_v)
                }
              }
            }
            if (map.contains(config.getString("depositor.userId")).equals(false)) {
              map = map + (config.getString("depositor.userId") -> 1)
            }
            if (map.contains("default")) {
              map = map - "default"
            }
          }
          map
        }

        def spaceStateLabels(stateLabel: String, mapIns: Map[String, Long]): Map[String, Long] = {
          var map: Map[String, Long] = mapIns
          if (config.getString("state.label").equals(stateLabel)) {
            if (map.contains(config.getString("depositor.userId"))) {
              for ((k, v) <- map) {
                if (k.contentEquals(config.getString("depositor.userId"))) {
                  val new_v = (k, v)._2 + file2.length()
                  map = map - k
                  map = map + (k -> new_v)
                }
              }
            }
            if (map.contains(config.getString("depositor.userId")).equals(false)) {
              map = map + (config.getString("depositor.userId") -> file2.length())
            }
            if (map.contains("default")) {
              map = map - "default"
            }
          }
          map
        }

        nbrDraftsMap = countStateLabels("DRAFT", nbrDraftsMap)
        nbrInvalidMap = countStateLabels("INVALID", nbrInvalidMap)
        nbrFinalizingMap = countStateLabels("FINALIZING", nbrFinalizingMap)
        nbrSubmittedMap = countStateLabels("SUBMITTED", nbrSubmittedMap)
        nbrArchivedMap = countStateLabels("ARCHIVED", nbrArchivedMap)
        nbrRejectedMap = countStateLabels("REJECTED", nbrRejectedMap)
        nbrFailedMap = countStateLabels("FAILED", nbrFailedMap)

        spaceDraftsMap = spaceStateLabels("DRAFT", spaceDraftsMap)
        spaceInvalidMap = spaceStateLabels("INVALID", spaceInvalidMap)
        spaceFinalizingMap = spaceStateLabels("FINALIZING", spaceFinalizingMap)
        spaceSubmittedMap = spaceStateLabels("SUBMITTED", spaceSubmittedMap)
        spaceArchivedMap = spaceStateLabels("ARCHIVED", spaceArchivedMap)
        spaceRejectedMap = spaceStateLabels("REJECTED", spaceRejectedMap)
        spaceFailedMap = spaceStateLabels("FAILED", spaceFailedMap)

        Try { countStateLabels("DRAFT", nbrDraftsMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"DRAFT\", nbrDraftsMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"DRAFT\", nbrDraftsMap)")
        }
        Try { countStateLabels("INVALID", nbrInvalidMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"INVALID\", nbrInvalidMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"INVALID\", nbrInvalidMap)")
        }
        Try { countStateLabels("FINALIZING", nbrFinalizingMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"FINALIZING\", nbrFinalizingMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"FINALIZING\", nbrFinalizingMap)")
        }
        Try { countStateLabels("SUBMITTED", nbrSubmittedMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"SUBMITTED\", nbrSubmittedMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"SUBMITTED\", nbrSubmittedMap)")
        }
        Try { countStateLabels("ARCHIVED", nbrArchivedMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"ARCHIVED\", nbrArchivedMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"ARCHIVED\", nbrArchivedMap)")
        }
        Try { countStateLabels("REJECTED", nbrRejectedMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"REJECTED\", nbrRejectedMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"REJECTED\", nbrRejectedMap)")
        }
        Try { countStateLabels("FAILED", nbrFailedMap) }
        match {
          case Failure(_) => println("Failure : countStateLabels(\"FAILED\", nbrFailedMap)")
          case Success(_) => logger.info("Success : countStateLabels(\"FAILED\", nbrFailedMap)")
        }
        Try { spaceStateLabels("DRAFT", spaceDraftsMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"DRAFT\", spaceDraftsMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"DRAFT\", spaceDraftsMap)")
        }
        Try { spaceStateLabels("INVALID", spaceInvalidMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"INVALID\", spaceInvalidMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"INVALID\", spaceInvalidMap)")
        }
        Try { spaceStateLabels("FINALIZING", spaceFinalizingMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"FINALIZING\", spaceFinalizingMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"FINALIZING\", spaceFinalizingMap)")
        }
        Try { spaceStateLabels("SUBMITTED", spaceSubmittedMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"SUBMITTED\", spaceSubmittedMap)")
          case Success(_) => logger.info("Success : spacetStateLabels(\"SUBMITTED\", spaceSubmittedMap)")
        }
        Try { spaceStateLabels("ARCHIVED", spaceArchivedMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"ARCHIVED\", spaceArchivedMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"ARCHIVED\", spaceArchivedMap)")
        }
        Try { spaceStateLabels("REJECTED", spaceRejectedMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"REJECTED\", spaceRejectedMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"REJECTED\", spaceRejectedMap)")
        }
        Try { spaceStateLabels("FAILED", spaceFailedMap) }
        match {
          case Failure(_) => println("Failure : spaceStateLabels(\"FAILED\", spaceFailedMap)")
          case Success(_) => logger.info("Success : spaceStateLabels(\"FAILED\", spaceFailedMap)")
        }
      }
    }

    if (nbrDepositorsMap == Map("default" -> 0)) {
      println("There is no deposit for the depositorId " + dep +
        " or you might have entered a nonvalid depositorId")
    }

    printer.flush()
    printer.close()
    out.toString
  }

  def summarizeReport(mapinput: Map[String, Int]): Unit = {
    val map: Map[String, Int] = mapinput
    val now = Calendar.getInstance().getTime()
    val Format = new SimpleDateFormat("yyyy-MM-dd")
    val currentTime = Format.format(now)
    for ((k, v) <- map) {
      println("summary:")
      println("Depositor: " + k)
      println("Timestamp: " + currentTime)
      println("Number of deposits: " + v)
      println("Total space : " + spaceDepositorsMap.apply(k) / 1073741824 + " GB")
      println("Total space : " + spaceDepositorsMap.apply(k) + " bytes")
      println("")
      println("Per state :")

      if (nbrDraftsMap.contains(k)) {
        println("DRAFT:   " + "\t" + nbrDraftsMap.apply(k) + "(" + spaceDraftsMap.apply(k) / 1048576 + "M)" + "(" + spaceDraftsMap.apply(k) + "bytes)")
      }
      if (nbrDraftsMap.contains(k).equals(false)) {
        println("DRAFT: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrInvalidMap.contains(k)) {
        println("INVALID: " + "\t" + nbrInvalidMap.apply(k) + "(" + spaceInvalidMap.apply(k) / 1048576 + "M)" + "(" + spaceInvalidMap.apply(k) + "bytes)")
      }
      if (nbrInvalidMap.contains(k).equals(false)) {
        println("INVALID: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrFinalizingMap.contains(k)) {
        println("FINALIZING: " + "\t" + nbrFinalizingMap.apply(k) + "(" + spaceFinalizingMap.apply(k) / 1048576 + "M)" + "(" + spaceFinalizingMap.apply(k) + "bytes)")
      }
      if (nbrFinalizingMap.contains(k).equals(false)) {
        println("FINALIZING: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrSubmittedMap.contains(k)) {
        println("SUBMITTED: " + "\t" + nbrSubmittedMap.apply(k) + "(" + spaceSubmittedMap.apply(k) / 1048576 + "M)" + "(" + spaceSubmittedMap.apply(k) + "bytes)")
      }
      if (nbrSubmittedMap.contains(k).equals(false)) {
        println("SUBMITTED: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrArchivedMap.contains(k)) {
        println("ARCHIVED: " + "\t" + nbrArchivedMap.apply(k) + "(" + spaceArchivedMap.apply(k) / 1048576 + "M)" + "(" + spaceArchivedMap.apply(k) + "bytes)")
      }
      if (nbrArchivedMap.contains(k).equals(false)) {
        println("ARCHIVED: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrRejectedMap.contains(k)) {
        println("REJECTED: " + "\t" + nbrRejectedMap.apply(k) + "(" + spaceRejectedMap.apply(k) / 1048576 + "M)" + "(" + spaceRejectedMap.apply(k) + "bytes)")
      }
      if (nbrRejectedMap.contains(k).equals(false)) {
        println("REJECTED: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      if (nbrFailedMap.contains(k)) {
        println("FAILED: " + "\t" + nbrFailedMap.apply(k) + "(" + spaceFailedMap.apply(k) / 1048576 + "M)" + "(" + spaceFailedMap.apply(k) + "bytes)")
      }
      if (nbrFailedMap.contains(k).equals(false)) {
        println("FAILED: " + "\t" + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
      }
      println()
    }
  }

  def createSummaryReport(depositor: String): Try[String] = {
    val depos = depositor
    createReport("easy-ingest-flow-inbox", depos)
    createReport("easy-sword2", depos)
    summarizeReport(nbrDepositorsMap)
    Try {
      "summary report" + depos
    }
  }

  def createFullReport(depositor: String): Try[String] = {
    val depos = depositor
    createReport("easy-ingest-flow-inbox", depos)
    println(createReport("easy-sword2", depos))
    Try {
      "full report " + depos
    }
  }


  def tryCreateFullReport(dirName: String, depositor: String): Unit = {
    val depos = depositor
    val name = dirName
    Try { createReport(dirName, depos) }
    match {
      case Failure(_) => println("FAILURE: createReport(" + name + "," + depos + ") failed. \n " +
        " \t Detected failures are reported below : ")
        tryGetPath(dirName: String)
      case Success(_) => println("SUCCESS: createReport(" + name + "," + depos + ") does not fail")
    }
  }

  def tryGetPath(dirName: String): Unit = {
    val name = dirName
    Try { getPath(name.toString) }
    match {
      case Failure(_) => println(
        " \n " +
          " \t FAILURE: getPath function fails to return a value: \n " +
          " \t The path for easy-ingest-flow-inbox cannot be found : \n " +
          " \t Check configuration properties -> \n " +
          " \t ./home/cfg/applicaton.properties, \n " +
          " \t ./src/main/assembly/dist/cfg/application.properties, \n" +
          " \t ./test/resources/debug-config/application.properties "
      )
      case Success(_) =>
        println(
          " \n " +
            " \t SUCCESS: getPath function returns a value: \n" +
            " \t The path for easy-ingest-flow-inbox is defined as: \n" +
            " \t" + getPath(name).toString
        )
        tryGetNonEmptyPath(name)
        tryGetListOfSubdirectories(name)

    }
  }

  def tryGetNonEmptyPath(dirName: String): Unit = {
    val name = dirName
    Try { getPath(name.toString).toString.nonEmpty }
    match {
      case Failure(_) => println(
        " \n " +
          " \t \t FAILURE: getPath function returns an empty value: \n " +
          " \t \t The path for easy-ingest-flow-inbox could not be defined : \n " +
          " \t \t Check configuration properties -> \n " +
          " \t \t ./home/cfg/applicaton.properties, \n " +
          " \t \t ./src/main/assembly/dist/cfg/application.properties, \n" +
          " \t \t ./test/resources/debug-config/application.properties "
      )
      case Success(_) => println(
        " \n " +
          " \t \t SUCCESS: getPath function returns a nonempty value: \n" +
          " \t \t The path for easy-ingest-flow-inbox is defined as: \n" +
          " \t \t " + getPath(name).toString
      )
    }
  }

  def tryGetListOfSubdirectories(dirName: String): Unit = {
    val name = dirName
    Try { getListOfSubDirectories(getPath(name).toString).toList }
    match {
      case Failure(_) => println(
        " \n " +
          "\t \t FAILURE: getListOfSubDirectories function fails: \n " +
          "\t \t Subdirectories could not be listed: \n " +
          "\t \t The path of the main directory " + "'" + name.toString + "'\n" + "  " +
          "\t \t might be incorrect: \n" +
          "\t \t Verify the path of the main directory " + "'" + name.toString + "'" + " \n " + "" +
          " \t \t from configuration properties "
      )
      case Success(_) => println(
        " \n " +
          "\t \t SUCCESS: getListOfSubDirectories does not fail: \n " +
          " \t \t Subdirectories of  " + "'" + name.toString + "'" + " were listed \n "
      )
    }
  }


}


