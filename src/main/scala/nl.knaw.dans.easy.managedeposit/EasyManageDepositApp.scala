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
package nl.knaw.dans.easy.managedeposit

import java.nio.file.{ Files, Path, Paths }

import nl.knaw.dans.easy.managedeposit.State._
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.BooleanUtils
import org.joda.time.{ DateTime, DateTimeZone, Duration }
import resource.managed

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.math.Ordering.{ Long => LongComparator }
import scala.util.{ Success, Try }
import scala.xml.{ NodeSeq, XML }

class EasyManageDepositApp(configuration: Configuration) extends DebugEnhancedLogging {

  private val sword2DepositsDir = Paths.get(configuration.properties.getString("easy-sword2"))
  private val ingestFlowInbox = Paths.get(configuration.properties.getString("easy-ingest-flow-inbox"))
  private val metadataDirName = "metadata"
  private val depositPropertiesFileName = "deposit.properties"
  private val dataSetFileName = "dataset.xml"
  val notAvailable = "n/a"

  private def collectDataFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age]): Deposits = {
    depositsDir.list(collectDataFromDepositsDir(filterOnDepositor, filterOnAge))
  }

  def deleteDepositFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId], age: Int, state: String, onlyData: Boolean): Try[Unit] = {
    val toBeDeletedState = State.toState(state).getOrElse(throw new IllegalArgumentException(s"state: $state is an unrecognized state")) // assigning unknown or null to the state when given an invalid state argument is dangerous while deleting
    depositsDir.list(deleteDepositsFromDepositsDir(filterOnDepositor, age, toBeDeletedState, onlyData))
  }

  def retryStalledDeposit(depositsDir: Path, filterOnDepositor: Option[DepositorId]): Unit = {
    depositsDir.list(retryStalledDeposit(filterOnDepositor))
  }

  private def collectDataFromDepositsDir(filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age])(deposits: List[Path]): Deposits = {
    trace(filterOnDepositor)
    deposits.filter(Files.isDirectory(_))
      .flatMap { depositDirPath =>
        val depositManager = new DepositManager(depositDirPath)
        val deposit: Try[Option[Deposit]] = for {
          _ <- depositManager.validateThatDepositDirectoryIsReadable()
          // if the properties file does not exist continue the process with an empty properties file
          // if the file does exist but the user cannot read it, this will return an exception
          _ <- depositManager.validateUserRightsForPropertiesFile()
          deposit <- getDeposit(filterOnDepositor, filterOnAge, depositManager)
        } yield deposit
        deposit.unsafeGetOrThrow
      }
  }

  private def getDeposit(filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], depositManager: DepositManager): Try[Option[Deposit]] = {
    val depositorId = depositManager.getDepositorId.getOrElse(notAvailable)
    lazy val lastModified: Option[DateTime] = getLastModifiedTimestamp(depositManager.depositDirPath)
    // forall returns true for the empty set, see https://en.wikipedia.org/wiki/Vacuous_truth
    val hasDepositor = filterOnDepositor.forall(depositorId ==)
    lazy val shouldReport = filterOnAge.forall(age => lastModified.forall(mod => Duration.millis(DateTime.now(mod.getZone).getMillis - mod.getMillis).getStandardDays <= age))

    if (hasDepositor && shouldReport) Success(
      Some {
        Deposit(
          depositId = depositManager.getDepositId.getOrElse(notAvailable),
          dansDoiIdentifier = getDoi(depositManager.getDoiIdentifier, depositManager.depositDirPath).getOrElse(notAvailable),
          dansDoiRegistered = depositManager.getDoiRegistered.map(BooleanUtils.toBoolean),
          fedoraIdentifier = depositManager.getFedoraIdentifier.getOrElse(notAvailable),
          depositor = depositorId,
          state = depositManager.getStateLabel,
          description = depositManager.getStateDescription.getOrElse(notAvailable),
          creationTimestamp = depositManager.getCreationTime.getOrElse(notAvailable).toString,
          numberOfContinuedDeposits = depositManager.getNumberOfContinuedDeposits,
          storageSpace = FileUtils.sizeOfDirectory(depositManager.depositDirPath.toFile),
          lastModified = lastModified.map(_.toString(dateTimeFormatter)).getOrElse(notAvailable),
        )
      })
    else Success(None)
  }

  def deleteDepositsFromDepositsDir(filterOnDepositor: Option[DepositorId], age: Int, state: State, onlyData: Boolean)(list: List[Path]): Try[Unit] = Try {
    list.filter(Files.isDirectory(_))
      .foreach { depositDirPath =>
        val depositManager = new DepositManager(depositDirPath)
        // The result of the Try will be discarded, only logged as other deposits need to be deleted nonetheless
        depositManager.deleteDepositFromDir(filterOnDepositor, age, state, onlyData)
          .doIfFailure { case e: Exception => logger.error(s"[${ depositManager.getDepositId }] Error while deleting deposit: ${ e.getMessage }", e) }
      }
  }

  def retryStalledDeposit(filterOnDepositor: Option[DepositorId])(list: List[Path]): Unit = {
    list.filter(Files.isDirectory(_))
      .foreach { depositDirPath =>
        val depositPropertiesFilePath = depositDirPath.resolve(depositPropertiesFileName)
        if (!Files.isReadable(depositDirPath)) {
          logErrorAndThrowNotReadableException(depositDirPath)
        }
        else if (!Files.isReadable(depositPropertiesFilePath)) {
          logErrorAndThrowNotReadableException(depositPropertiesFilePath)
        }
        val depositManager = new DepositManager(depositDirPath)
        val depositorId = depositManager.getDepositorId.getOrElse(notAvailable)
        val depositState = depositManager.getStateLabel

        // forall returns true for the empty set, see https://en.wikipedia.org/wiki/Vacuous_truth
        if (filterOnDepositor.forall(depositorId ==)) {
          if (depositState == STALLED) {
            logger.info(s"RESET to SUBMITTED for $depositorId on $depositDirPath")
            depositManager.setState(SUBMITTED)
            depositManager.saveProperties()
          }
        }
      }
  }

  private def getLastModifiedTimestamp(depositDirPath: Path): Option[DateTime] = {
    if (!Files.isReadable(depositDirPath)) {
      logErrorAndThrowNotReadableException(depositDirPath)
    }
    managed(Files.list(depositDirPath)).acquireAndGet { files =>
      files.forEach(file => if (!Files.isReadable(file.toRealPath())) {
        val pathOfAZipFileOrPropFileInDepositDirPath = file.toRealPath()
        logErrorAndThrowNotReadableException(pathOfAZipFileOrPropFileInDepositDirPath)
      }
      )
    }
    managed(Files.list(depositDirPath)).acquireAndGet { files =>
      files.map[Long](Files.getLastModifiedTime(_).toInstant.toEpochMilli)
        .max(LongComparator)
        .map[DateTime](millis => new DateTime(millis, DateTimeZone.UTC))
        .toOption
    }
  }

  private def getDoi(doiIdentifier: Option[String], depositDirPath: Path): Option[String] = {
    if (!Files.isReadable(depositDirPath)) {
      logErrorAndThrowNotReadableException(depositDirPath)
    }
    managed(Files.list(depositDirPath)).acquireAndGet { files =>
      files.forEach(file => if (!Files.isReadable(file.toRealPath())) {
        val pathOfAZipFileOrPropFileInDepositDirPath = file.toRealPath()
        logErrorAndThrowNotReadableException(pathOfAZipFileOrPropFileInDepositDirPath)
      }
      )
    }

    managed(Files.list(depositDirPath)).acquireAndGet { files =>
      files.iterator().asScala.toStream
        .collectFirst { case bagDir if Files.isDirectory(bagDir) =>
          if (!Files.isReadable(bagDir)) {
            logErrorAndThrowNotReadableException(bagDir)
          }
          if (Files.isReadable(bagDir)) {
            val datasetXml = bagDir.resolve(metadataDirName).resolve(dataSetFileName)
            if (Files.exists(datasetXml) && !Files.isReadable(datasetXml)) {
              logErrorAndThrowNotReadableException(datasetXml)
            }
          }
        }
    }
    doiIdentifier.orElse {
      managed(Files.list(depositDirPath)).acquireAndGet { files =>
        files.iterator().asScala.toStream
          .collectFirst { case bagDir if Files.isDirectory(bagDir) => bagDir.resolve(metadataDirName).resolve(dataSetFileName) }
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

  private def logErrorAndThrowNotReadableException(filePath: Path) = {
    logger.error(s"cannot read $filePath")
    throw NotReadableException(filePath)
  }

  def summary(depositor: Option[DepositorId], age: Option[Age]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age)
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age)
    ReportGenerator.outputSummary(sword2Deposits ++ ingestFlowDeposits, depositor)(Console.out)
    "End of summary report."
  }

  def createFullReport(depositor: Option[DepositorId], age: Option[Age]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age)
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age)
    ReportGenerator.outputFullReport(sword2Deposits ++ ingestFlowDeposits)(Console.out)
    "End of full report."
  }

  def createErrorReport(depositor: Option[DepositorId], age: Option[Age]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age)
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age)
    ReportGenerator.outputErrorReport(sword2Deposits ++ ingestFlowDeposits)(Console.out)
    "End of error report."
  }

  def retryDepositor(depositor: Option[DepositorId]): Try[String] = Try {
    retryStalledDeposit(ingestFlowInbox, depositor)
    "STALLED states were replaced by SUBMITTED states."
  }

  def cleanDepositor(depositor: Option[DepositorId], age: Int, state: String, onlyData: Boolean): Try[String] = {
    for {
      _ <- deleteDepositFromDepositsDir(sword2DepositsDir, depositor, age, state, onlyData)
      _ <- deleteDepositFromDepositsDir(ingestFlowInbox, depositor, age, state, onlyData)
    } yield "Execution of clean: success "
  }
}
