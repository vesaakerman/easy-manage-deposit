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
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.rogach.scallop.ScallopOption

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

class EasyManageDepositApp(configuration: Configuration) extends DebugEnhancedLogging with Curation {

  private val sword2DepositsDir = Paths.get(configuration.properties.getString("easy-sword2"))
  private val ingestFlowInbox = Paths.get(configuration.properties.getString("easy-ingest-flow-inbox"))
  private val fedoraCredentials = new FedoraCredentials(
    new URL(configuration.properties.getString("fedora.url")),
    configuration.properties.getString("fedora.user"),
    configuration.properties.getString("fedora.password"))
  val fedora = new Fedora(new FedoraClient(fedoraCredentials))
  val landingPageBaseUrl = new URI(configuration.properties.getString("landing-pages.base-url"))

  private implicit val dansDoiPrefixes: List[String] = configuration.properties.getList("dans-doi.prefixes")
    .asScala.toList
    .map(prefix => prefix.asInstanceOf[String])

  private def collectDataFromDepositsDir(depositsDir: Path, filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], location: String): Deposits = {
    depositsDir.list(collectDataFromDepositsDir(filterOnDepositor, filterOnAge, location))
  }

  def deleteDepositFromDepositsDir(depositsDir: Path, deleteParams: DeleteParameters): DeletedDeposits = {
    depositsDir.list(deleteDepositsFromDepositsDir(deleteParams)).filter(d => d.nonEmpty).map(d => d.get)
  }

  private def collectDataFromDepositsDir(filterOnDepositor: Option[DepositorId], filterOnAge: Option[Age], location: String)(depositPaths: List[Path]): Deposits = {
    trace(filterOnDepositor)
    getDepositManagers(depositPaths)
      .withFilter(_.isValidDeposit)
      .withFilter(_.hasDepositor(filterOnDepositor))
      .withFilter(_.isOlderThan(filterOnAge))
      .map(_.getDepositInformation(location))
      .collect { case Success(d: DepositInformation) => d }
  }

  private def getDepositManagers(depositPaths: List[Path]): List[DepositManager] = {
    depositPaths.collect { case file if Files.isDirectory(file) => new DepositManager(file) }
  }

  def deleteDepositsFromDepositsDir(deleteParams: DeleteParameters)(depositPaths: List[Path]): List[Option[DeletedDepositInformation]] = {
    getDepositManagers(depositPaths)
      .map { depositManager =>
        // The result of the Try will be discarded, only logged as other deposits need to be deleted nonetheless
        depositManager.deleteDepositFromDir(deleteParams)
          .doIfFailure {
            case e: Exception => logger.error(s"[${ depositManager.getDepositId }] Error while deleting deposit: ${ e.getMessage }", e)
          }
          .collect { case d: DeletedDepositInformation => d }
          .toOption
      }
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

  def cleanDepositor(depositor: Option[DepositorId], age: Int, state: String, onlyData: Boolean, doUpdate: Boolean, newStateLabel: ScallopOption[String], newStateDescription: ScallopOption[String], output: Boolean): Try[String] = Try {
    val toBeDeletedState = State.toState(state).getOrElse(throw new IllegalArgumentException(s"state: $state is an unrecognized state")) // assigning unknown or null to the state when given an invalid state argument is dangerous while deleting
    newStateLabel.foreach { stateLabel => State.toState(stateLabel).getOrElse(throw new IllegalArgumentException(s"state: $stateLabel is an unrecognized state")) }
    val deleteParams = DeleteParameters(depositor, age, toBeDeletedState, onlyData, doUpdate, newStateLabel.getOrElse(""), newStateDescription.getOrElse(""), output)
    val sword2DeletedDeposits = deleteDepositFromDepositsDir(sword2DepositsDir, deleteParams)
    val ingestFlowDeletedDeposits = deleteDepositFromDepositsDir(ingestFlowInbox, deleteParams)
    if (output || !doUpdate)
      ReportGenerator.outputDeletedDeposits(sword2DeletedDeposits ++ ingestFlowDeletedDeposits)(Console.out)
    "Execution of clean: success "
  }

  def syncFedoraState(easyDatasetId: DatasetId): Try[FeedBackMessage] = {
    for {
      _ <- validateUserCanReadAllDepositsInIngestFlowBox()
      manager <- findDepositManagerForDatasetId(easyDatasetId)
      curationMessage <- curate(manager)
    } yield curationMessage
  }

  private def validateUserCanReadAllDepositsInIngestFlowBox(): Try[Unit] = {
    val deposits = Files.newDirectoryStream(ingestFlowInbox).asScala.toList
    getDepositManagers(deposits)
      .map(_.validateUserCanReadTheDepositDirectoryAndTheDepositProperties())
      .collectFirst { case f @ Failure(_: Exception) => f }
      .getOrElse(Success(()))
  }

  private def findDepositManagerForDatasetId(easyDatasetId: DatasetId): Try[DepositManager] = Try {
    ingestFlowInbox
      .list(_.collect { case deposit if Files.isDirectory(deposit) => new DepositManager(deposit) })
      .collectFirst { case manager if manager.getFedoraIdentifier.contains(easyDatasetId) => manager }
      .getOrElse(throw new IllegalArgumentException(s"No deposit found for datatsetId $easyDatasetId"))
  }
}
