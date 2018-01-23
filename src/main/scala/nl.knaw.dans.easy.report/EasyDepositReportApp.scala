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

import java.nio.file._
import java.text.SimpleDateFormat
import java.util.Calendar

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.FileUtils
import org.joda.time.{ DateTime, DateTimeZone }
import resource.managed

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.math.Ordering.{ Long => LongComparator }
import scala.util.Try
import scala.xml.{ NodeSeq, XML }

case class Deposit(depositId: DepositId,
                   doi: Option[String],
                   depositor: DepositorId,
                   state: String,
                   description: String,
                   creationTimestamp: String,
                   numberOfContinuedDeposits: Int,
                   storageSpace: Long,
                   lastModified: String)

class EasyDepositReportApp(configuration: Configuration) extends DebugEnhancedLogging {
  private val KB = 1024L
  private val MB = 1024L * KB
  private val GB = 1024L * MB
  private val TB = 1024L * GB

  private val sword2DepositsDir = Paths.get(configuration.properties.getString("easy-sword2"))
  private val ingestFlowInbox = Paths.get(configuration.properties.getString("easy-ingest-flow-inbox"))

  private def collectDataFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId]): Deposits = {
    depositsDir.list(collectDataFromDepositsDir(filterOnDepositor))
  }

  private def collectDataFromDepositsDir(filterOnDepositor: Option[DepositorId])(deposits: List[Path]): Deposits = {
    trace(filterOnDepositor)
    deposits.filter(Files.isDirectory(_))
      .flatMap { depositDirPath =>
        debug(s"Getting info from $depositDirPath")
        val depositId = depositDirPath.getFileName.toString
        val depositProperties = new PropertiesConfiguration(depositDirPath.resolve("deposit.properties").toFile)
        val depositorId = depositProperties.getString("depositor.userId")

        // forall returns true for the empty set, see https://en.wikipedia.org/wiki/Vacuous_truth
        if (filterOnDepositor.forall(depositorId ==)) Some {
          Deposit(
            depositId = depositId,
            doi = getDoi(depositProperties, depositDirPath),
            depositorId,
            state = depositProperties.getString("state.label"),
            description = depositProperties.getString("state.description"),
            creationTimestamp = Option(depositProperties.getString("creation.timestamp")).getOrElse("n/a"),
            depositDirPath.list(_.count(_.getFileName.toString.matches("""^.*\.zip\.\d+$"""))),
            storageSpace = FileUtils.sizeOfDirectory(depositDirPath.toFile),
            lastModified = getLastModifiedTimestamp(depositDirPath)
          )
        }
        else None
      }
  }

  private def getLastModifiedTimestamp(depositDirPath: Path): String = {
    managed(Files.list(depositDirPath)).acquireAndGet { files =>
      files.map[Long](Files.getLastModifiedTime(_).toInstant.toEpochMilli)
        .max(LongComparator)
        .map[String](millis => new DateTime(millis, DateTimeZone.UTC).toString(dateTimeFormatter))
        .orElse("n/a")
    }
  }

  private def getDoi(depositProperties: PropertiesConfiguration, depositDirPath: Path): Option[String] = {
    Option(depositProperties.getString("identifier.doi")).orElse {
      managed(Files.list(depositDirPath)).acquireAndGet { files =>
        files.iterator().asScala.toStream
          .collectFirst { case bagDir if Files.isDirectory(bagDir) => bagDir.resolve("metadata/dataset.xml") }
          .flatMap {
            case datasetXml if Files.exists(datasetXml) => Try {
              val docElement = XML.loadFile(datasetXml.toFile)
              findDoi(docElement \\ "dcmiMetadata" \\ "identifier")
            }.getOrElse(None)
            case _ => None
          }
      }
    }
  }

  private def findDoi(identifiers: NodeSeq): Option[String] = {
    identifiers.find { id =>
      id.attribute(XML_NAMESPACE_XSI, "type").exists {
        case Seq(n) =>
          n.text.split(':') match {
            case Array(pre, suffix) => id.getNamespace(pre) == XML_NAMESPACE_ID_TYPE && suffix == "DOI"
            case _ => false
          }
      }
    }.map(_.text)
  }

  private def outputSummary(deposits: Deposits, depositor: Option[DepositorId] = None): Unit = {
    val selectedDeposits = depositor.map(d => deposits.filter(_.depositor == d)).getOrElse(deposits)
    val draft = selectedDeposits.filter(_.state == "DRAFT").toList
    val invalid = selectedDeposits.filter(_.state == "INVALID").toList
    val finalizing = selectedDeposits.filter(_.state == "FINALIZING").toList
    val submitted = selectedDeposits.filter(_.state == "SUBMITTED").toList
    val archived = selectedDeposits.filter(_.state == "ARCHIVED").toList
    val rejected = selectedDeposits.filter(_.state == "REJECTED").toList
    val failed = selectedDeposits.filter(_.state == "FAILED").toList

    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    val currentTime = format.format(now)

    println("Grand totals:")
    println("-------------")
    println(s"Timestamp          : $currentTime")
    println(f"Number of deposits : ${ selectedDeposits.size }%10d")
    println(s"Total space        : ${ formatStorageSize(selectedDeposits.map(_.storageSpace).sum) }")
    println()
    println("Per state:")
    println("----------")
    println(formatCountAndSize(draft, "DRAFT"))
    println(formatCountAndSize(invalid, "INVALID"))
    println(formatCountAndSize(finalizing, "FINALIZING"))
    println(formatCountAndSize(submitted, "SUBMITTED"))
    println(formatCountAndSize(archived, "ARCHIVED"))
    println(formatCountAndSize(rejected, "REJECTED"))
    println(formatCountAndSize(failed, "FAILED"))
    println()
  }

  private def outputFullReport(deposits: Deposits): Unit = {
    val csvFormat: CSVFormat = CSVFormat.RFC4180
      .withHeader("DEPOSITOR", "DEPOSIT_ID", "DEPOSIT_STATE", "DOI", "DEPOSIT_CREATION_TIMESTAMP",
        "DEPOSIT_UPDATE_TIMESTAMP", "DESCRIPTION", "NBR_OF_CONTINUED_DEPOSITS", "STORAGE_IN_BYTES")
      .withDelimiter(',')
    val printer = csvFormat.print(Console.out)
    deposits.sortBy(_.creationTimestamp) foreach { deposit =>
      printer.printRecord(
        deposit.depositor,
        deposit.depositId,
        deposit.state,
        deposit.doi.getOrElse("n/a"),
        deposit.creationTimestamp,
        deposit.lastModified,
        deposit.description,
        deposit.numberOfContinuedDeposits.toString,
        deposit.storageSpace.toString)
    }
  }

  private def formatStorageSize(nBytes: Long): String = {
    def formatSize(unitSize: Long, unit: String): String = {
      f"${ nBytes / unitSize.toFloat }%8.1f $unit"
    }

    if (nBytes > 1.1 * TB) formatSize(TB, "T")
    else if (nBytes > 1.1 * GB) formatSize(GB, "G")
    else if (nBytes > 1.1 * MB) formatSize(MB, "M")
    else if (nBytes > 1.1 * KB) formatSize(KB, "K")
    else formatSize(1, "B")
  }

  private def formatCountAndSize(deposits: List[Deposit], filterOnState: String): String = {
    f"${ filterOnState }%-10s : ${ deposits.size }%5d (${ formatStorageSize(deposits.map(_.storageSpace).sum) })"
  }

  def summary(depositor: Option[DepositorId] = None): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor)
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor)
    outputSummary(sword2Deposits ++ ingestFlowDeposits, depositor)
    "End of summary report."
  }

  def createFullReport(depositor: Option[DepositorId]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor)
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor)
    outputFullReport(sword2Deposits ++ ingestFlowDeposits)
    "End of full report."
  }
}
