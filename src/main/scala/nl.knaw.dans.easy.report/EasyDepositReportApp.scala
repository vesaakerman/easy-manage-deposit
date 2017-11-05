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



  def function1(dir: String): String = {
    dirPath = getPath(dir)
    dirList = getListOfSubDirectories(getPath(dir).toString).toList
    dirList.foreach { i =>
      pathname = dirPath.toString + "/" + i + "/deposit.properties"
      pathname2 = dirPath.toString + "/" + i
      fileList = getListOfFiles(pathname2)
      nbrOfContinued = 0
      fileList.foreach {j =>
        if(j.getName.contains("zip")){
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

      if(config.getString("depositor.userId").equals("mendeleydata")){
        DepositCounterMendeley = DepositCounterMendeley + 1
        TotalSpaceMendeley = TotalSpaceMendeley + file2.length()
      }
      if(config.getString("state.label").equals("DRAFT")) {
        nbrDraft = nbrDraft + 1
        spaceDraft = spaceDraft + file2.length()
      }
      if(config.getString("state.label").equals("INVALID")) {
        nbrInvalid = nbrInvalid + 1
        spaceInvalid = spaceInvalid + file2.length()
      }
      if(config.getString("state.label").equals("FINALIZING")) {
        nbrFinalizing= nbrFinalizing + 1
        spaceFinalizing = spaceFinalizing + file2.length()
      }
      if(config.getString("state.label").equals("SUBMITTED")) {
        nbrSubmitted = nbrSubmitted + 1
        spaceSubmitted = spaceSubmitted + file2.length()
      }
      if(config.getString("state.label").equals("ARCHIVED")) {
        nbrArchived = nbrArchived + 1
        spaceArchived = spaceArchived + file2.length()
      }
      if(config.getString("state.label").equals("REJECTED")) {
        nbrRejected = nbrRejected + 1
        spaceRejected = spaceRejected + file2.length()
      }
      if(config.getString("state.label").equals("FAILED")) {
        nbrFailed = nbrFailed + 1
        spaceFailed = spaceFailed + file2.length()
      }

    }
    printer.flush()
    printer.close()
    out.toString
  }




  def createSummaryReport(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-ingest-flow-inbox")
      function1("easy-sword2")
      System.out.print("summary:\n")
      System.out.print("Depositor: mendeley\n")
      System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
      System.out.print("\n")
      System.out.print("Number of deposits: " + DepositCounterMendeley)
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley / 1073741824 + "GB")
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley + "bytes")
      System.out.print("\n")
      System.out.print("\n")
      System.out.print("Per state :\n")
      System.out.print("DRAFT : " + nbrDraft + "(" + spaceDraft / 1048576 + "M)" + "(" + spaceDraft + "bytes)")
      System.out.print("\n")
      System.out.print("INVALID : " + nbrInvalid + "(" + spaceInvalid / 1048576 + "M)" + "(" + spaceInvalid + "bytes)")
      System.out.print("\n")
      System.out.print("FINALIZING : " + nbrFinalizing + "(" + spaceFinalizing / 1048576 + "M)" + "(" + spaceFinalizing + "bytes)")
      System.out.print("\n")
      System.out.print("SUBMITTED : " + nbrSubmitted + "(" + spaceSubmitted / 1048576 + "M)" + "(" + spaceSubmitted + "bytes)")
      System.out.print("\n")
      System.out.print("ARCHIVED : " + nbrArchived + "(" + spaceArchived / 1048576 + "M)" + "(" + spaceArchived + "bytes)")
      System.out.print("\n")
      System.out.print("REJECTED : " + nbrRejected + "(" + spaceRejected / 1048576 + "M)" + "(" + spaceRejected + "bytes)")
      System.out.print("\n")
      System.out.print("FAILED : " + nbrFailed + "(" + spaceFailed / 1048576 + "M)" + "(" + spaceFailed + "bytes)")
      System.out.print("\n")
      Try {"summary report"}
  }

  def createSummaryReportSword2(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-sword2")
      System.out.print("summary:\n")
      System.out.print("Depositor: mendeley\n")
      System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
      System.out.print("\n")
      System.out.print("Number of deposits: " + DepositCounterMendeley)
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley / 1073741824 + "GB")
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley + "bytes")
      System.out.print("\n")
      System.out.print("\n")
      System.out.print("Per state :\n")
      System.out.print("DRAFT : " + nbrDraft + "(" + spaceDraft / 1048576 + "M)" + "(" + spaceDraft + "bytes)")
      System.out.print("\n")
      System.out.print("INVALID : " + nbrInvalid + "(" + spaceInvalid / 1048576 + "M)" + "(" + spaceInvalid + "bytes)")
      System.out.print("\n")
      System.out.print("FINALIZING : " + nbrFinalizing + "(" + spaceFinalizing / 1048576 + "M)" + "(" + spaceFinalizing + "bytes)")
      System.out.print("\n")
      System.out.print("SUBMITTED : " + nbrSubmitted + "(" + spaceSubmitted / 1048576 + "M)" + "(" + spaceSubmitted + "bytes)")
      System.out.print("\n")
      System.out.print("ARCHIVED : " + nbrArchived + "(" + spaceArchived / 1048576 + "M)" + "(" + spaceArchived + "bytes)")
      System.out.print("\n")
      System.out.print("REJECTED : " + nbrRejected + "(" + spaceRejected / 1048576 + "M)" + "(" + spaceRejected + "bytes)")
      System.out.print("\n")
      System.out.print("FAILED : " + nbrFailed + "(" + spaceFailed / 1048576 + "M)" + "(" + spaceFailed + "bytes)")
      System.out.print("\n")
      Try {"summary report easy-sword2"}
  }

  def createSummaryReportEasyIngestFlowInbox(depositor: Option[String] = None): Try[String] = {
      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)//SHOULD THIS BE THE CURRENT TIMESTAMP???????
      function1("easy-ingest-flow-inbox")
      System.out.print("summary:\n")
      System.out.print("Depositor: mendeley\n")
      System.out.print("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
      System.out.print("\n")
      System.out.print("Number of deposits: " + DepositCounterMendeley)
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley / 1073741824 + "GB")
      System.out.print("\n")
      System.out.print("Total space : " + TotalSpaceMendeley + "bytes")
      System.out.print("\n")
      System.out.print("\n")
      System.out.print("Per state :\n")
      System.out.print("DRAFT : " + nbrDraft + "(" + spaceDraft / 1048576 + "M)" + "(" + spaceDraft + "bytes)")
      System.out.print("\n")
      System.out.print("INVALID : " + nbrInvalid + "(" + spaceInvalid / 1048576 + "M)" + "(" + spaceInvalid + "bytes)")
      System.out.print("\n")
      System.out.print("FINALIZING : " + nbrFinalizing + "(" + spaceFinalizing / 1048576 + "M)" + "(" + spaceFinalizing + "bytes)")
      System.out.print("\n")
      System.out.print("SUBMITTED : " + nbrSubmitted + "(" + spaceSubmitted / 1048576 + "M)" + "(" + spaceSubmitted + "bytes)")
      System.out.print("\n")
      System.out.print("ARCHIVED : " + nbrArchived + "(" + spaceArchived / 1048576 + "M)" + "(" + spaceArchived + "bytes)")
      System.out.print("\n")
      System.out.print("REJECTED : " + nbrRejected + "(" + spaceRejected / 1048576 + "M)" + "(" + spaceRejected + "bytes)")
      System.out.print("\n")
      System.out.print("FAILED : " + nbrFailed + "(" + spaceFailed / 1048576 + "M)" + "(" + spaceFailed + "bytes)")
      System.out.print("\n")
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


