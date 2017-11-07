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

import java.io.{BufferedReader, File, FileInputStream, FileWriter, InputStream, InputStreamReader, _}
import java.lang.Object
import java.nio._
import java.nio.file.{attribute, _}
import java.text.SimpleDateFormat
import java.util
import java.util.Calendar

import com.sun.jmx.snmp.Timestamp
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration.{DefaultIOFactory, IOFactory, PropertiesReader}
import org.apache.commons.configuration.{PropertiesConfiguration, PropertyConverter}
import org.apache.commons.csv.{CSVFormat, CSVPrinter, _}
import resource.managed

import scala.collection.JavaConverters._
import scala.io.Source
import scala.tools.nsc.io.Directory
import scala.tools.util
import scala.util.Try
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.FileReader
import java.io.IOException
import scala.collection.Map



class EasyDepositReportApp(configuration: Configuration) extends DebugEnhancedLogging{

  val csvFormat: CSVFormat = CSVFormat.RFC4180.withHeader("DEPOSITOR", "DEPOSIT_ID", "DEPOSIT_STATE", "DOI", "DEPOSIT_CREATION_TIMESTAMP", "DEPOSIT_UPDATE_TIMESTAMP", "DESCRIPTION", "NBR OF CONTINUED DEPOSITS").withDelimiter(',')
  val out: Appendable = new StringBuffer()
  val printer : CSVPrinter = csvFormat.print(out)

  logger.info("Headers: $csvFormat.format()")


  def getListOfSubDirectories(directoryName: String): Array[String] = {
     new File(directoryName)
          .listFiles
          .filter(_.isDirectory)
          .map(_.getName)
  }

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def getPath (dir: String): Path = {
    Paths.get(configuration.properties.getString(dir))
  }

  //private val IngestFlowInbox = getPath("easy-ingest-flow-inbox")
  //private val sword2 = getPath("easy-sword2")

  //private val dirs = getListOfSubDirectories(IngestFlowInbox.toString)
  //private val dirsSword = getListOfSubDirectories(sword2.toString)


  var nbrOfContinued: Int = 0
  var pathname: String = "default"
  var pathname2: String = "default"
  var dirPath : Path = null
  var dirList : List[String] = null
  var fileList : List[File] = null
  var k: Int = 1
  var file2: File =  null
  var DepositCounterMendeley: Int = 0
  var TotalSpaceMendeley:Long = 0
  var nbrDraft: Int= 0
  var spaceDraft:Long = 0
  var nbrInvalid: Int = 0
  var spaceInvalid:Long = 0
  var nbrFinalizing: Int = 0
  var spaceFinalizing:Long = 0
  var nbrSubmitted: Int = 0
  var spaceSubmitted:Long = 0
  var nbrArchived: Int = 0
  var spaceArchived:Long = 0
  var nbrRejected: Int = 0
  var spaceRejected:Long = 0
  var nbrFailed: Int = 0
  var spaceFailed:Long = 0
  //var nbrDepositorsMap:Map[String, Int] = scala.collection.mutable.Map("default" -> 0)
  var nbrDepositorsMap:Map[String, Int]= Map("default" -> 0)
  var map: Map[Int,Int]= Map(0 -> 0)
  for ((k,v) <- map) printf("key: %d, value: %d\n", k, v)
  var nbrDraftsMap:Map[String, Int] = Map("default" -> 0)
  var nbrSubmittedMap:Map[String, Int] = Map("default" -> 0)
  var nbrArchivedMap:Map[String, Int] = Map("default" -> 0)
  var nbrFinalizingMap:Map[String, Int] = Map("default" -> 0)
  var nbrRejectedMap:Map[String, Int] = Map("default" -> 0)
  var nbrInvalidMap:Map[String, Int] = Map("default" -> 0)
  var nbrFailedMap:Map[String, Int] = Map("default" -> 0)

  var spaceDepositorsMap:Map[String, Long]= Map("default" -> 0)
  var spaceDraftsMap:Map[String, Long] = Map("default" -> 0)
  var spaceSubmittedMap:Map[String, Long] = Map("default" -> 0)
  var spaceArchivedMap:Map[String, Long] = Map("default" -> 0)
  var spaceFinalizingMap:Map[String, Long] = Map("default" -> 0)
  var spaceRejectedMap:Map[String, Long] = Map("default" -> 0)
  var spaceInvalidMap:Map[String, Long] = Map("default" -> 0)
  var spaceFailedMap:Map[String, Long] = Map("default" -> 0)


    def function1(dir: String): String = {
      dirPath = getPath(dir)
      dirList = getListOfSubDirectories(getPath(dir).toString).toList
      dirList.foreach { i =>
        pathname = dirPath.toString + "/" + i + "/deposit.properties"
        pathname2 = dirPath.toString + "/" + i
        fileList = getListOfFiles(pathname2)
        nbrOfContinued = 0
        fileList.foreach { j =>
          if (j.getName.contains("zip")) {
            nbrOfContinued = nbrOfContinued + 1
          }
        }
        val config = new PropertiesConfiguration(pathname)
        file2 = new File(pathname2)

        /*--------DEPOSIT_UPDATE_TIMESTAMP---------*/
        /*-------??????????????????????????--------*/
        var sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        logger.info(sdf.format(file2.lastModified()))
        /*-----------------------------------------*/

        printer.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinued.toString)

        if (nbrDepositorsMap.contains(config.getString("depositor.userId"))) {
          for ((k, v) <- nbrDepositorsMap) {
            if (k.contentEquals(config.getString("depositor.userId"))) {
              var new_v = (k, v)._2 + 1
              nbrDepositorsMap = nbrDepositorsMap - k
              nbrDepositorsMap = nbrDepositorsMap + (k -> new_v)
            }
          }
        }
        if (nbrDepositorsMap.contains(config.getString("depositor.userId")).equals(false)) {
           nbrDepositorsMap = nbrDepositorsMap + (config.getString("depositor.userId") -> 1)
        }
        if (nbrDepositorsMap.contains("default")) {
           nbrDepositorsMap = nbrDepositorsMap - ("default")
        }

        //println(nbrDepositorsMap)

        if (spaceDepositorsMap.contains(config.getString("depositor.userId"))) {
          for ((k, v) <- spaceDepositorsMap) {
            if (k.contentEquals(config.getString("depositor.userId"))) {
              var new_v = (k, v)._2 + file2.length()
              spaceDepositorsMap = spaceDepositorsMap - k
              spaceDepositorsMap = spaceDepositorsMap + (k -> new_v)
            }
          }
        }
        if (spaceDepositorsMap.contains(config.getString("depositor.userId")).equals(false)) {
          spaceDepositorsMap = spaceDepositorsMap + (config.getString("depositor.userId") -> file2.length())
        }
        if (spaceDepositorsMap.contains("default")) {
          spaceDepositorsMap = spaceDepositorsMap - ("default")
        }

        //println(spaceDepositorsMap)


        def CountStateLabels(stateLabel: String, mapIns : Map[String, Int]): Map[String, Int] = {
          var map: Map[String, Int] = mapIns
          if (config.getString("state.label").equals(stateLabel)) {
            //var nbrDraftsMap: Map[String, Int] = Map(config.getString("depositor.userId") -> 1)
            if (map.contains(config.getString("depositor.userId"))) {
              for ((k, v) <- map) {
                if (k.contentEquals(config.getString("depositor.userId"))) {
                  var new_v = (k, v)._2 + 1
                  map = map - k
                  map = map + (k -> new_v)
                }
              }
            }
            if (map.contains(config.getString("depositor.userId")).equals(false)) {
                map = map + (config.getString("depositor.userId") -> 1)
            }
            if (map.contains("default")) {
                map = map - ("default")
            }
          }
          map
        }

        def SpaceStateLabels(stateLabel: String, mapIns : Map[String, Long]): Map[String, Long] = {
          var map: Map[String, Long] = mapIns
          if (config.getString("state.label").equals(stateLabel)) {
            //var nbrDraftsMap: Map[String, Int] = Map(config.getString("depositor.userId") -> 1)
            if (map.contains(config.getString("depositor.userId"))) {
              for ((k, v) <- map) {
                if (k.contentEquals(config.getString("depositor.userId"))) {
                  var new_v = (k, v)._2 + file2.length()
                  map = map - k
                  map = map + (k -> new_v)
                }
              }
            }
            if (map.contains(config.getString("depositor.userId")).equals(false)) {
              map = map + (config.getString("depositor.userId") -> file2.length())
            }
            if (map.contains("default")) {
              map = map - ("default")
            }
          }
          map
        }



        nbrDraftsMap = CountStateLabels("DRAFT", nbrDraftsMap)
        nbrInvalidMap = CountStateLabels("INVALID", nbrInvalidMap)
        nbrFinalizingMap = CountStateLabels("FINALIZING", nbrFinalizingMap)
        nbrSubmittedMap = CountStateLabels("SUBMITTED", nbrSubmittedMap)
        nbrArchivedMap = CountStateLabels("ARCHIVED", nbrArchivedMap)
        nbrRejectedMap = CountStateLabels("REJECTED", nbrRejectedMap)
        nbrFailedMap = CountStateLabels("FAILED", nbrFailedMap)

        spaceDraftsMap = SpaceStateLabels("DRAFT", spaceDraftsMap)
        spaceInvalidMap = SpaceStateLabels("INVALID", spaceInvalidMap)
        spaceFinalizingMap = SpaceStateLabels("FINALIZING", spaceFinalizingMap)
        spaceSubmittedMap = SpaceStateLabels("SUBMITTED", spaceSubmittedMap)
        spaceArchivedMap = SpaceStateLabels("ARCHIVED", spaceArchivedMap)
        spaceRejectedMap = SpaceStateLabels("REJECTED", spaceRejectedMap)
        spaceFailedMap = SpaceStateLabels("FAILED", spaceFailedMap)

      }

      printer.flush()
      printer.close()
      out.toString
    }







  def createSummaryReport(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-ingest-flow-inbox")
      function1("easy-sword2")
    for ((k,v) <- nbrDepositorsMap ) {
      System.out.print("summary:\n")
      System.out.print("Depositor: " + k)
      System.out.print("\n")
      System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
      System.out.print("\n")
      System.out.print("Number of deposits: " + v)
      System.out.print("\n")
      System.out.print("Total space : " + spaceDepositorsMap.apply(k) / 1073741824 + " GB")
      System.out.print("\n")
      System.out.print("Total space : " + spaceDepositorsMap.apply(k) + " bytes")
      System.out.print("\n")
      System.out.print("\n")
      System.out.print("Per state :\n")

      if(nbrDraftsMap.contains(k)) {
        System.out.print("DRAFT : " + nbrDraftsMap.apply(k) + "(" + spaceDraftsMap.apply(k)/1048576 + "M)" + "(" + spaceDraftsMap.apply(k) + "bytes)")
        System.out.print("\n")
      }
      if(nbrDraftsMap.contains(k).equals(false)) {
        System.out.print("DRAFT : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrInvalidMap.contains(k)) {
        System.out.print("INVALID : " + nbrInvalidMap.apply(k) + "(" + spaceInvalidMap.apply(k)/1048576 + "M)" + "(" + spaceInvalidMap.apply(k) + "bytes)")
        System.out.print("\n")
      }
      if(nbrInvalidMap.contains(k).equals(false)) {
        System.out.print("INVALID : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrFinalizingMap.contains(k)) {
        System.out.print("FINALIZING : " + nbrFinalizingMap.apply(k) + "(" + spaceFinalizingMap.apply(k)/1048576 + "M)" + "(" + spaceFinalizingMap.apply(k) + "bytes)")
        System.out.print("\n")
      }else{
        System.out.print("FINALIZING : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrSubmittedMap.contains(k)) {
        System.out.print("SUBMITTED : " + nbrSubmittedMap.apply(k) + "(" + spaceSubmittedMap.apply(k)/1048576 + "M)" + "(" + spaceSubmittedMap.apply(k) + "bytes)")
        System.out.print("\n")
      }else{
        System.out.print("SUBMITTED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrArchivedMap.contains(k)) {
        System.out.print("ARCHIVED : " + nbrArchivedMap.apply(k) + "(" + spaceArchivedMap.apply(k)/1048576 + "M)" + "(" + spaceArchivedMap.apply(k) + "bytes)")
        System.out.print("\n")
      }else{
        System.out.print("ARCHIVED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrRejectedMap.contains(k)) {
        System.out.print("REJECTED : " + nbrRejectedMap.apply(k) + "(" + spaceRejectedMap.apply(k)/1048576 + "M)" + "(" + spaceRejectedMap.apply(k) + "bytes)")
        System.out.print("\n")
      }else{
        System.out.print("REJECTED: " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      if(nbrFailedMap.contains(k)) {
        System.out.print("FAILED : " + nbrFailedMap.apply(k) + "(" + spaceFailedMap.apply(k)/1048576 + "M)" + "(" + spaceFailedMap.apply(k) + "bytes)")
        System.out.print("\n")
      }else{
        System.out.print("FAILED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
        System.out.print("\n")
      }
      System.out.print("\n")

    }
      Try {"summary report"}
  }

  def createSummaryReportSword2(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-sword2")
      for ((k,v) <- nbrDepositorsMap ) {
        System.out.print("summary:\n")
        System.out.print("Depositor: " + k)
        System.out.print("\n")
        System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
        System.out.print("\n")
        System.out.print("Number of deposits: " + v)
        System.out.print("\n")
        System.out.print("Total space : " + spaceDepositorsMap.apply(k) / 1073741824 + " GB")
        System.out.print("\n")
        System.out.print("Total space : " + spaceDepositorsMap.apply(k) + " bytes")
        System.out.print("\n")
        System.out.print("\n")
        System.out.print("Per state :\n")

        if(nbrDraftsMap.contains(k)) {
           System.out.print("DRAFT : " + nbrDraftsMap.apply(k) + "(" + spaceDraftsMap.apply(k)/1048576 + "M)" + "(" + spaceDraftsMap.apply(k) + "bytes)")
            System.out.print("\n")
        }
        if(nbrDraftsMap.contains(k).equals(false)) {
          System.out.print("DRAFT : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrInvalidMap.contains(k)) {
          System.out.print("INVALID : " + nbrInvalidMap.apply(k) + "(" + spaceInvalidMap.apply(k)/1048576 + "M)" + "(" + spaceInvalidMap.apply(k) + "bytes)")
          System.out.print("\n")
        }
        if(nbrInvalidMap.contains(k).equals(false)) {
          System.out.print("INVALID : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrFinalizingMap.contains(k)) {
          System.out.print("FINALIZING : " + nbrFinalizingMap.apply(k) + "(" + spaceFinalizingMap.apply(k)/1048576 + "M)" + "(" + spaceFinalizingMap.apply(k) + "bytes)")
          System.out.print("\n")
        }else{
          System.out.print("FINALIZING : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrSubmittedMap.contains(k)) {
          System.out.print("SUBMITTED : " + nbrSubmittedMap.apply(k) + "(" + spaceSubmittedMap.apply(k)/1048576 + "M)" + "(" + spaceSubmittedMap.apply(k) + "bytes)")
          System.out.print("\n")
        }else{
          System.out.print("SUBMITTED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrArchivedMap.contains(k)) {
          System.out.print("ARCHIVED : " + nbrArchivedMap.apply(k) + "(" + spaceArchivedMap.apply(k)/1048576 + "M)" + "(" + spaceArchivedMap.apply(k) + "bytes)")
          System.out.print("\n")
        }else{
          System.out.print("ARCHIVED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrRejectedMap.contains(k)) {
          System.out.print("REJECTED : " + nbrRejectedMap.apply(k) + "(" + spaceRejectedMap.apply(k)/1048576 + "M)" + "(" + spaceRejectedMap.apply(k) + "bytes)")
          System.out.print("\n")
        }else{
          System.out.print("REJECTED: " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        if(nbrFailedMap.contains(k)) {
          System.out.print("FAILED : " + nbrFailedMap.apply(k) + "(" + spaceFailedMap.apply(k)/1048576 + "M)" + "(" + spaceFailedMap.apply(k) + "bytes)")
          System.out.print("\n")
        }else{
          System.out.print("FAILED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
          System.out.print("\n")
        }
        System.out.print("\n")
      }
      Try {"summary report easy-sword2"}
  }

  def createSummaryReportEasyIngestFlowInbox(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-ingest-flow-inbox")
      for ((k,v) <- nbrDepositorsMap ) {
         System.out.print("summary:\n")
         System.out.print("Depositor: " + k)
         System.out.print("\n")
         System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
         System.out.print("\n")
         System.out.print("Number of deposits: " + v)
         System.out.print("\n")
         System.out.print("Total space : " + spaceDepositorsMap.apply(k) / 1073741824 + " GB")
         System.out.print("\n")
         System.out.print("Total space : " + spaceDepositorsMap.apply(k) + " bytes")
         System.out.print("\n")
         System.out.print("\n")
         System.out.print("Per state :\n")

         if(nbrDraftsMap.contains(k)) {
           System.out.print("DRAFT : " + nbrDraftsMap.apply(k) + "(" + spaceDraftsMap.apply(k)/1048576 + "M)" + "(" + spaceDraftsMap.apply(k) + "bytes)")
           System.out.print("\n")
         }
         if(nbrDraftsMap.contains(k).equals(false)) {
           System.out.print("DRAFT : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrInvalidMap.contains(k)) {
           System.out.print("INVALID : " + nbrInvalidMap.apply(k) + "(" + spaceInvalidMap.apply(k)/1048576 + "M)" + "(" + spaceInvalidMap.apply(k) + "bytes)")
           System.out.print("\n")
         }
         if(nbrInvalidMap.contains(k).equals(false)) {
           System.out.print("INVALID : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrFinalizingMap.contains(k)) {
           System.out.print("FINALIZING : " + nbrFinalizingMap.apply(k) + "(" + spaceFinalizingMap.apply(k)/1048576 + "M)" + "(" + spaceFinalizingMap.apply(k) + "bytes)")
           System.out.print("\n")
         }else{
           System.out.print("FINALIZING : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrSubmittedMap.contains(k)) {
           System.out.print("SUBMITTED : " + nbrSubmittedMap.apply(k) + "(" + spaceSubmittedMap.apply(k)/1048576 + "M)" + "(" + spaceSubmittedMap.apply(k) + "bytes)")
           System.out.print("\n")
         }else{
           System.out.print("SUBMITTED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrArchivedMap.contains(k)) {
           System.out.print("ARCHIVED : " + nbrArchivedMap.apply(k) + "(" + spaceArchivedMap.apply(k)/1048576 + "M)" + "(" + spaceArchivedMap.apply(k) + "bytes)")
           System.out.print("\n")
         }else{
           System.out.print("ARCHIVED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrRejectedMap.contains(k)) {
           System.out.print("REJECTED : " + nbrRejectedMap.apply(k) + "(" + spaceRejectedMap.apply(k)/1048576 + "M)" + "(" + spaceRejectedMap.apply(k) + "bytes)")
           System.out.print("\n")
         }else{
           System.out.print("REJECTED: " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         if(nbrFailedMap.contains(k)) {
           System.out.print("FAILED : " + nbrFailedMap.apply(k) + "(" + spaceFailedMap.apply(k)/1048576 + "M)" + "(" + spaceFailedMap.apply(k) + "bytes)")
           System.out.print("\n")
         }else{
           System.out.print("FAILED : " + 0 + "(" + 0 + "M)" + "(" + 0 + "bytes)")
           System.out.print("\n")
         }
         System.out.print("\n")
      }
      Try {"summary report easy-ingest-flow-inbox"}
  }

  def createFullReport(depositor: Option[String] = None ): Try[String] = {
      System.out.print(function1("easy-ingest-flow-inbox"))
      System.out.print(function1("easy-sword2"))
      Try { "full report" }
  }

  def createFullReportSword2(depositor: Option[String] = None ): Try[String] = {
      System.out.print(function1("easy-sword2"))

    Try { "full report easy-sword2" }
  }

  def createFullReportEasyIngestFlowInbox(depositor: Option[String] = None ): Try[String] = {
      System.out.print(function1("easy-ingest-flow-inbox"))
      Try { "full report easy-ingest-flow-inbox" }
  }



}


