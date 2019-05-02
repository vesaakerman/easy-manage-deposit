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

import java.net.{ URI, URL }
import java.nio.file.{ Files, Path, Paths }

import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraCredentials }
import nl.knaw.dans.easy.managedeposit.Command.FeedBackMessage
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

class EasyManageDepositApp(configuration: Configuration) extends DebugEnhancedLogging with Curation {

  private val sword2DepositsDir = Paths.get(configuration.properties.getString("easy-sword2"))
  private val ingestFlowInbox = Paths.get(configuration.properties.getString("easy-ingest-flow-inbox"))
  private val metadataDirName = "metadata"
  private val dataSetFileName = "dataset.xml"
  private val fedoraCredentials = new FedoraCredentials(
    new URL(configuration.properties.getString("fedora.url")),
    configuration.properties.getString("fedora.user"),
    configuration.properties.getString("fedora.password"))
  val fedora = new Fedora(new FedoraClient(fedoraCredentials))
  val landingPageBaseUrl = new URI(configuration.properties.getString("landing-pages.base-url"))

  private implicit val dansDoiPrefixes: List[String] = configuration.properties.getList("dans-doi.prefixes")
    .asScala.toList
    .map(prefix => prefix.asInstanceOf[String])

  private def collectDataFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], source: String): Deposits = {
    depositsDir.list(collectDataFromDepositsDir(filterOnDepositor, filterOnAge, source))
  }

  def deleteDepositFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId], age: Int, state: String, onlyData: Boolean): Try[Unit] = {
    val toBeDeletedState = State.toState(state).getOrElse(throw new IllegalArgumentException(s"state: $state is an unrecognized state")) // assigning unknown or null to the state when given an invalid state argument is dangerous while deleting
    depositsDir.list(deleteDepositsFromDepositsDir(filterOnDepositor, age, toBeDeletedState, onlyData))
  }

  private def collectDataFromDepositsDir(filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], source: String)(deposits: List[Path]): Deposits = {
    trace(filterOnDepositor)
    deposits.filter(Files.isDirectory(_))
      .flatMap { depositDirPath =>
        val depositManager = new DepositManager(depositDirPath)
        val deposit: Try[Option[Deposit]] = for {
          _ <- depositManager.validateThatDepositDirectoryIsReadable()
          // if the properties file does not exist continue the process with an empty properties file
          // if the file does exist but the user cannot read it, this will return an exception
          _ <- depositManager.validateUserRightsForPropertiesFile()
          deposit <- getDeposit(filterOnDepositor, filterOnAge, depositManager, source)
        } yield deposit
        deposit.unsafeGetOrThrow
      }
  }

  private def getDeposit(filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], depositManager: DepositManager, source: String): Try[Option[Deposit]] = {
    val depositorId = depositManager.getDepositorId.getOrElse(notAvailable)
    lazy val lastModified: Option[DateTime] = getLastModifiedTimestamp(depositManager.depositDirPath)
    // forall returns true for the empty set, see https://en.wikipedia.org/wiki/Vacuous_truth
    val hasDepositor = filterOnDepositor.forall(depositorId ==)
    lazy val shouldReport = filterOnAge.forall(age => lastModified.forall(mod => Duration.millis(DateTime.now(mod.getZone).getMillis - mod.getMillis).getStandardDays <= age))

    if (hasDepositor && shouldReport) Try {
      Some {
        Deposit(
          depositId = depositManager.getDepositId.getOrElse(notAvailable),
          doiIdentifier = getDoi(depositManager.getDoiIdentifier, depositManager.depositDirPath).getOrElse(notAvailable),
          dansDoiRegistered = depositManager.getDansDoiRegistered.map(BooleanUtils.toBoolean),
          fedoraIdentifier = depositManager.getFedoraIdentifier.getOrElse(notAvailable),
          depositor = depositorId,
          state = depositManager.getStateLabel,
          description = depositManager.getStateDescription.getOrElse(notAvailable),
          creationTimestamp = depositManager.getCreationTime.getOrElse(notAvailable).toString,
          numberOfContinuedDeposits = depositManager.getNumberOfContinuedDeposits,
          storageSpace = FileUtils.sizeOfDirectory(depositManager.depositDirPath.toFile),
          lastModified = lastModified.map(_.toString(dateTimeFormatter)).getOrElse(notAvailable),
          source = source,
        )
      }
    }
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
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age, "SWORD2")
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age, "INGEST_FLOW")
    ReportGenerator.outputSummary(sword2Deposits ++ ingestFlowDeposits, depositor)(Console.out)
    "End of summary report."
  }

  def createFullReport(depositor: Option[DepositorId], age: Option[Age]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age, "SWORD2")
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age, "INGEST_FLOW")
    ReportGenerator.outputFullReport(sword2Deposits ++ ingestFlowDeposits)(Console.out)
    "End of full report."
  }

  def createErrorReport(depositor: Option[DepositorId], age: Option[Age]): Try[String] = Try {
    val sword2Deposits = collectDataFromDepositsDir(sword2DepositsDir, depositor, age, "SWORD2")
    val ingestFlowDeposits = collectDataFromDepositsDir(ingestFlowInbox, depositor, age, "INGEST_FLOW")
    ReportGenerator.outputErrorReport(sword2Deposits ++ ingestFlowDeposits)(Console.out)
    "End of error report."
  }

  def cleanDepositor(depositor: Option[DepositorId], age: Int, state: String, onlyData: Boolean): Try[String] = {
    for {
      _ <- deleteDepositFromDepositsDir(sword2DepositsDir, depositor, age, state, onlyData)
      _ <- deleteDepositFromDepositsDir(ingestFlowInbox, depositor, age, state, onlyData)
    } yield "Execution of clean: success "
  }

  def adminCurate(easyDatasetId: DatasetId): Try[FeedBackMessage] = {
    for {
      manager <- findDepositManagerForDatasetId(easyDatasetId)
      curationMessage <- curate(manager)
    } yield curationMessage
  }

  private def findDepositManagerForDatasetId(easyDatasetId: DatasetId): Try[DepositManager] = Try {
    ingestFlowInbox
      .list(_.collect { case deposit if Files.isDirectory(deposit) => new DepositManager(deposit) })
      .collectFirst { case manager if manager.getDatasetId.contains(easyDatasetId) => manager }
      .getOrElse(throw new IllegalArgumentException(s"No deposit found for datatsetId $easyDatasetId"))
  }
}
