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
  val csvPrinter = new CSVPrinter(new FileWriter("./data/mendeleyReport2" + ".csv"), csvFormat.withDelimiter(','))

  val out: Appendable = new StringBuffer()
  val printer : CSVPrinter = csvFormat.print(out)

  logger.info(csvFormat.format())

  val summaryOutput = new File("./data/mendeleyReport2_Summary" + ".txt")
  val fileWriter = new FileWriter(summaryOutput)

  def getListOfSubDirectories(directoryName: String): Array[String] = {
    (new File(directoryName))
          .listFiles
          .filter(_.isDirectory)
          .map(_.getName)
  }
  val dirs = getListOfSubDirectories("./data/easy-ingest-flow-inbox")
  val dirsSword = getListOfSubDirectories("./data/easy-sword2")


  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  /* --------- Last modified time of the directory 'easy-ingest-flow-inbox' and 'easy-sword2'------- */
  /* -------------------------THIS IS NOT NECESSARY FOR THE REPORTS--------------------------------- */
  //val file = new File("./data/easy-ingest-flow-inbox")
  //val fileSword = new File("./data/easy-sword2")
  //val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  //println(sdf.format(file.lastModified()))
  //println(sdf.format(fileSword.lastModified()))
  /* ----------------------------------------------------------------------------------------------- */

  /* ------------ Size of the directory 'easy-ingest-flow-inbox & easy-sword2' --------------------- */
  /* -------------------------THIS IS NOT NECESSARY FOR THE REPORTS--------------------------------- */
  //println(file.length())
  //println(fileSword.length())
  /* ----------------------------------------------------------------------------------------------- */


  var DepositCounterMendeley = 0

  var TotalSpaceMendeley:Long = 0

  var nbrDraft = 0

  var spaceDraft:Long = 0

  var nbrInvalid = 0

  var spaceInvalid:Long = 0

  var nbrFinalizing = 0

  var spaceFinalizing:Long = 0

  var nbrSubmitted = 0

  var spaceSubmitted:Long = 0

  var nbrArchived = 0

  var spaceArchived:Long = 0

  var nbrRejected = 0

  var spaceRejected:Long = 0

  var nbrFailed = 0

  var spaceFailed:Long = 0

  var pthname = "default"

  var pthname2 = "default"


  /*----------------------------------For each subdirectory i in the directory easy-ingest-flow---------------------------------- */
  dirs.foreach {i =>
  //for (i <- dirs) {
       //println(i)
       //val watcher: WatchService = FileSystems.getDefault.newWatchService

       pthname = "./data/easy-ingest-flow-inbox/" + i + "/deposit.properties"
       pthname2 = "./data/easy-ingest-flow-inbox/"+ i

       var nbrOfContinuedDeposits = 0

       val dirs2 = getListOfFiles(pthname2)

       dirs2.foreach {j =>
           if(j.getName.contains("zip")){
              nbrOfContinuedDeposits = nbrOfContinuedDeposits + 1
           }
       }

       val config = new PropertiesConfiguration(pthname)
       var file2 = new File(pthname2)

       /*-------Depositor------------------------*/
       logger.info(config.getString("depositor.userId"))
       /*-----------------------------------------*/

       /*-------Deposit_ID------------------------*/
       logger.info(config.getString("bag-store.bag-id"))
       /*-----------------------------------------*/

       /*-------Deposit State---------------------*/
       logger.info(config.getString("state.label"))
       /*-----------------------------------------*/

       /*--------------DOI------------------------*/
       logger.info(config.getString("identifier.doi"))
       /*-----------------------------------------*/

       /*-----------Description-------------------*/
       logger.info(config.getString("state.description"))
       /*-----------------------------------------*/

       /*--------DEPOSIT_UPDATE_TIMESTAMP---------*/
       /*-------??????????????????????????--------*/
       var sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
       logger.info(sdf.format(file2.lastModified()))
       /*-----------------------------------------*/


       csvPrinter.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinuedDeposits.toString)
       printer.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinuedDeposits.toString)


       /*--Update the nbr and space of deposits by the depositor 'mendeleydata'--*/
       if(config.getString("depositor.userId").equals("mendeleydata")){
          DepositCounterMendeley = DepositCounterMendeley + 1
          TotalSpaceMendeley = TotalSpaceMendeley + file2.length()
       }
       /*------------------------------------------------------------------------*/

       /*--------  Update the nbr and space of deposits per state----------------*/

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
        /*------------------------------------------------------------------------*/
  }

  /*----------------------------------For each subdirectory i in the directory easy-sword2---------------------------------- */

  dirsSword.foreach { i =>
  //for (i <- dirsSword) {
    logger.info(i)
    //val watcher: WatchService = FileSystems.getDefault.newWatchService

    pthname = "./data/easy-sword2/" + i + "/deposit.properties"
    pthname2 = "./data/easy-sword2/" + i

    var nbrOfContinuedDeposits = 0

    val dirs2 = getListOfFiles(pthname2)

    dirs2.foreach { j =>
    //for(j <- dirs2){
      if(j.getName.contains("zip")){
        nbrOfContinuedDeposits = nbrOfContinuedDeposits + 1
      }
    }

    val config = new PropertiesConfiguration(pthname)

    var file2 = new File(pthname2)

    /*-------Depositor------------------------*/
    logger.info(config.getString("depositor.userId"))
    /*-----------------------------------------*/

    /*-------Deposit_ID------------------------*/
    logger.info(config.getString("bag-store.bag-id"))
    /*-----------------------------------------*/

    /*-------Deposit State---------------------*/
    logger.info(config.getString("state.label"))
    /*-----------------------------------------*/

    /*--------------DOI------------------------*/
    logger.info(config.getString("identifier.doi"))
    /*-----------------------------------------*/

    /*-----------Description-------------------*/
    logger.info(config.getString("state.description"))
    /*-----------------------------------------*/

    /*--------DEPOSIT_UPDATE_TIMESTAMP---------*/
    /*-------??????????????????????????--------*/
    var sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
    logger.info(sdf.format(file2.lastModified()))
    /*-----------------------------------------*/


    csvPrinter.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinuedDeposits.toString)
    printer.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file2.lastModified()), config.getString("state.description"), nbrOfContinuedDeposits.toString)


    /*--Update the nbr and space of deposits by the depositor 'mendeleydata'--*/
    if(config.getString("depositor.userId").equals("mendeleydata")){
      DepositCounterMendeley = DepositCounterMendeley + 1
      TotalSpaceMendeley = TotalSpaceMendeley + file2.length()
    }
    /*------------------------------------------------------------------------*/

    /*--------  Update the nbr and space of deposits per state----------------*/

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
    /*------------------------------------------------------------------------*/
  }





      fileWriter.write("summary:\n")

      fileWriter.write("Depositor: mendeley \n")

      val currentTimestamp = new Timestamp(Calendar.getInstance.getTime.getTime)

      fileWriter.write("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????
      fileWriter.write("\n")

      fileWriter.write("Number of deposits: " + DepositCounterMendeley)
      fileWriter.write("\n")

      fileWriter.write("Total space : " + TotalSpaceMendeley / 1073741824 + "GB")
      fileWriter.write("\n")

      fileWriter.write("Total space : " + TotalSpaceMendeley + "bytes")
      fileWriter.write("\n")
      fileWriter.write("\n")

      fileWriter.write("Per state : ")
      fileWriter.write("\n")

      fileWriter.write("DRAFT : " + nbrDraft + "(" + spaceDraft / 1048576 + "M)" + "(" + spaceDraft + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("INVALID : " + nbrInvalid + "(" + spaceInvalid / 1048576 + "M)" + "(" + spaceInvalid + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("FINALIZING : " + nbrFinalizing + "(" + spaceFinalizing / 1048576 + "M)" + "(" + spaceFinalizing + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("SUBMITTED : " + nbrSubmitted + "(" + spaceSubmitted / 1048576 + "M)" + "(" + spaceSubmitted + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("ARCHIVED : " + nbrArchived + "(" + spaceArchived / 1048576 + "M)" + "(" + spaceArchived + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("REJECTED : " + nbrRejected + "(" + spaceRejected / 1048576 + "M)" + "(" + spaceRejected + "bytes)")
      fileWriter.write("\n")

      fileWriter.write("FAILED : " + nbrFailed + "(" + spaceFailed / 1048576 + "M)" + "(" + spaceFailed + "bytes)")
      fileWriter.write("\n")


  def createSummaryReport(depositor: Option[String] = None): Try[String] = {

    println("summary:")

    println("Depositor: mendeley")

    println("Timestamp: " + currentTimestamp) //SHOULD THIS BE THE CURRENT TIMESTAMP???????

    println("Number of deposits: " + DepositCounterMendeley)

    println("Total space : " + TotalSpaceMendeley / 1073741824 + "GB")

    println("Total space : " + TotalSpaceMendeley + "bytes")
    println("\n")

    println("Per state : ")

    println("DRAFT : " + nbrDraft + "(" + spaceDraft / 1048576 + "M)" + "(" + spaceDraft + "bytes)")

    println("INVALID : " + nbrInvalid + "(" + spaceInvalid / 1048576 + "M)" + "(" + spaceInvalid + "bytes)")

    println("FINALIZING : " + nbrFinalizing + "(" + spaceFinalizing / 1048576 + "M)" + "(" + spaceFinalizing + "bytes)")

    println("SUBMITTED : " + nbrSubmitted + "(" + spaceSubmitted / 1048576 + "M)" + "(" + spaceSubmitted + "bytes)")

    println("ARCHIVED : " + nbrArchived + "(" + spaceArchived / 1048576 + "M)" + "(" + spaceArchived + "bytes)")

    println("REJECTED : " + nbrRejected + "(" + spaceRejected / 1048576 + "M)" + "(" + spaceRejected + "bytes)")

    println("FAILED : " + nbrFailed + "(" + spaceFailed / 1048576 + "M)" + "(" + spaceFailed + "bytes)")

    Try {"summary report"}
  }


  def createFullReport(depositor: Option[String] = None): Try[String] = {
      //val reader = new FileReader("./data/mendeleyReport2.csv")
      //val parser = new CSVParser(reader, CSVFormat.RFC4180)
      //val listt = parser.getRecords
      //println(listt)

      System.out.print(printer)
      System.out.print(out.toString)

      Try { "full report" }
  }

  csvPrinter.flush()

  logger.info(csvFormat.format())

  fileWriter.flush()


  /*def readTextFile(filename: String): Try[List[String]] = {
    Try{Source.fromFile(filename).getLines.toList}
  }


  val filename = "./mendeleyReport2_Summary" + ".txt"
  readTextFile(filename) match {
    case Success(lines) => lines.foreach(println)
    case Failure(f) => println(f)
  }*/


}


