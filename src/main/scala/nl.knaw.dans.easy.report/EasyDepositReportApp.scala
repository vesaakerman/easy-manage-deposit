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

import java.io.{FileWriter, File, _}
import java.lang.Object
import java.nio._
import java.nio.file.{attribute, _}
import java.util

import org.apache.commons.configuration.PropertiesConfiguration.{DefaultIOFactory, IOFactory, PropertiesReader}
import org.apache.commons.configuration.{PropertiesConfiguration, PropertyConverter}
import org.apache.commons.csv.{CSVFormat, CSVPrinter, _}
import resource.managed

import scala.collection.JavaConverters._
import scala.io.Source
import scala.tools.nsc.io.Directory
import scala.util.Try

//import package com.mkyong.file
import java.text.SimpleDateFormat

import java.io.BufferedReader
import java.io.InputStreamReader




class EasyDepositReportApp(configuration: Configuration) {

  //val in = new FileReader("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties")

  //  def read(filePath:String):String = {
  //  Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8).asScala.mkString
  //}


  /*val linesList = Source.fromFile("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties").getLines.toList
      println(linesList)
  val linesArray = Source.fromFile("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties").getLines.toArray
      println(linesArray)
  //val fileContents = Source.fromFile(filename).getLines.mkString*/




  //val printer = CSVFormat.DEFAULT.withHeader("H1", "H2").print(createFullReport))

  /*val outputFile: String = "/Users/gulcinermis/git/service/easy/easy-deposit-report/mendeleyReport" + ".csv"
  val csvFileFormat: CSVFormat = CSVFormat.RFC4180.withHeader("Col1", "Col2").withDelimiter(',')
  val fileWriter = new FileWriter(outputFile)
  var csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat)
  csvFilePrinter.printRecord("Name", "Price")
  csvFilePrinter.println()
  csvFilePrinter.print("a")
  csvFilePrinter.flush()*/

  val csvFormat: CSVFormat = CSVFormat.RFC4180.withHeader("DEPOSITOR", "DEPOSIT_ID", "DEPOSIT_STATE", "DOI", "DEPOSIT_CREATION_TIMESTAMP", "DEPOSIT_UPDATE_TIMESTAMP", "DESCRIPTION").withDelimiter(',')
  val csvPrinter = new CSVPrinter(new FileWriter("/Users/gulcinermis/git/service/easy/easy-deposit-report/mendeleyReport2" + ".csv"), csvFormat.withDelimiter(','))
  //csvPrinter.printRecord("Name", "Price", "A", "B")

  /*class WhitespacePropertiesReader(val in: Nothing, val delimiter: Char) extends PropertiesConfiguration.PropertiesReader(in, delimiter) {
       override protected def parseProperty(line: String): Unit = { // split the line at the first '=' character
           val pos = line.indexOf('=')
           val key = line.substring(0, pos).trim
           val value = line.substring(pos + 1).trim
           //now store the key and the value of the property
           initPropertyName(key)
           initPropertyValue(value)
           //csvPrinter.printRecord(initPropertyName(key))
       }
  }*/

  def getListOfSubDirectories(directoryName: String): Array[String] = {
    (new File(directoryName))
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName)
  }

  val dirs = getListOfSubDirectories("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox")

  var pthname = "default"

  //def lastModified()

  val file = new File("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox")
  println(file.lastModified())
  val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  println(sdf.format(file.lastModified()))
  val process= Runtime.getRuntime.exec("run.sh /Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox" )
  val br = new BufferedReader(new InputStreamReader(process.getInputStream))
  //var data = "default"

  //var i = 0
   while(br.readLine()!=null) {
         //  data = br.readLine
         println(br.readLine())
         //i=i+1
   }


  for (i <- dirs) {
    println(i)
    //val watcher: WatchService = FileSystems.getDefault.newWatchService
    pthname = "/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/" + i + "/deposit.properties"
    val config = new PropertiesConfiguration(pthname)
    println(config.getString("depositor.userId"))
    println(config.getString("bag-store.bag-id"))
    println(config.getString("state.label"))
    println(config.getString("identifier.doi"))
    println(config.getString("state.description"))

    val file = new File(pthname)
    //println(file.lastModified())
    val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
    println(sdf.format(file.lastModified()))

    val process= Runtime.getRuntime.exec(pthname)
    val br = new BufferedReader(new InputStreamReader(process.getInputStream))

    while(br.readLine()!=null) {
      println(br.readLine())
    }

    

    csvPrinter.printRecord(config.getString("depositor.userId"), config.getString("bag-store.bag-id"), config.getString("state.label"), config.getString("identifier.doi"), "unknown", sdf.format(file.lastModified()), config.getString("state.description"))


  }
  csvPrinter.flush()


  /*
  var strng = "default"
  var valueDepositUserId = "default"
  var valueBagId = "default"
  var valueStateLabel = "default"
  var valueIdentifierDoi = "default"

  for (line <- Source.fromFile("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties").getLines) {
       println(line)
       if (line.contains("depositor.userId")) {
           //println(line)
           strng = line
           //println(strng.toString)
           valueDepositUserId = PropertyConverter.split(line, '=').get(1)
           println(valueDepositUserId)
       }
       if (line.contains("bag-store.bag-id")) {
           valueBagId = PropertyConverter.split(line, '=').get(1)
       }
       if (line.contains("state.label")) {
           valueStateLabel = PropertyConverter.split(line, '=').get(1)
       }
       if (line.contains("identifier.doi")) {
           valueIdentifierDoi = PropertyConverter.split(line, '=').get(1)
       }
  }

  csvPrinter.printRecord(valueDepositUserId, valueBagId, valueStateLabel, valueIdentifierDoi) */

  /*val data: util.List[String] = new util.ArrayList[String]
  data.add(str)
  csvPrinter.printRecord(data)
  csvPrinter.flush()*/





  //val str = strng

  //val props = new FileReader("/Users/gulcinermis/git/service/easy/easy-deposit-report/data/easy-ingest-flow-inbox/0168583c-0f4a-4691-a136-371f147aa14d/deposit.properties")
  //val ky = PropertyConverter.split("gulcin", 'l').get(0)
  //csvPrinter.printRecord(ky, "Price", "A", "B")




  def createFullReport(depositor: Option[String] = None): Try[String] = {
    Try { "full report" }
    //Try {in.read().toString}
    //Try {str}
    //csvFilePrinter.print(str)

  }
}


