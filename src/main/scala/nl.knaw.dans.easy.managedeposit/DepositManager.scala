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

import java.nio.file.{ Files, Path }

import nl.knaw.dans.easy.managedeposit.State.{ State, UNKNOWN }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.joda.time.{ DateTime, DateTimeZone, Duration }

import scala.language.postfixOps
import scala.util.{ Success, Try }

class DepositManager(val depositDirPath: Path) extends DebugEnhancedLogging {
  private val depositPropertiesFileName: String = "deposit.properties"
  private val depositIdKey = "bag-store.bag-id"
  private val stateLabelKey = "state.label"
  private lazy val depositProperties: Option[PropertiesConfiguration] = findDepositProperties

  def getNumberOfContinuedDeposits: Int = {
    if (Files.exists(depositDirPath)) {
      depositDirPath.list(_.count(_.getFileName.toString.matches("""^.*\.zip\.\d+$""")))
    }
    else 0
  }

  def getDoiIdentifier: Option[String] = {
    getProperty("identifier.doi")
  }

  def getDoiRegistered: Option[String] = {
    getProperty("identifier.doi.registered")
  }

  def getFedoraIdentifier: Option[String] = {
    getProperty("identifier.fedora")
  }

  def getDepositId: Option[String] = {
    getProperty(depositIdKey).orElse(Option(depositDirPath.getFileName).map(_.toString))
  }

  def getStateDescription: Option[String] = {
    getProperty("state.description")
  }

  def getDepositorId: Option[String] = {
    getProperty("depositor.userId")
  }

  def getCreationTime: Option[DateTime] = {
    getProperty("creation.timestamp").map(timeString => new DateTime(timeString))
  }

  /**
   * getStateLabel returns the state of the deposit based on its deposit.properties file
   * if the state.label entry is absent or has a unrecognised value State.UNKNOWN is returned
   *
   * @return State enumeration
   */
  def getStateLabel: State = {
    getProperty(stateLabelKey).flatMap(State.toState).getOrElse(UNKNOWN)
  }

  def setState(stateLabel: State): Unit = {
    setProperty(stateLabelKey, stateLabel.toString)
  }

  def setProperty(key: String, value: String): Unit = {
    depositProperties.foreach(_.setProperty(key, value))
  }

  def saveProperties(): Unit = {
    depositProperties.foreach(_.save(depositPropertiesFileName))
  }

  def validateThatFileIsReadable(path: Path): Try[Unit] = Try {
    if (!Files.isReadable(path))
      throw NotReadableException(path)
  }

  def depositAgeIsLargerThanRequiredAge(age: Int): Boolean = {
    val creationTime = getCreationTime
    if (creationTime.isEmpty) logger.warn(s"deposit: $getDepositId does not have a creation time") // a doIfEmpty method would be nice
    creationTime.fold(false)(start => new Duration(start, end).getStandardDays > age)
  }

  def validateThatDepositDirectoryIsReadable(): Try[Unit] = {
    validateThatFileIsReadable(depositDirPath)
  }

  def validateThatDepositPropertiesIsReadable(): Try[Unit] = {
    validateThatFileIsReadable(depositPropertiesFilePath)
  }

  def validateUserRightsForPropertiesFile(): Try[Unit] = Try {
    if (depositPropertiesFileExists && !Files.isReadable(depositPropertiesFilePath))
      throw NotReadableException(depositPropertiesFilePath)
  }

  def deleteDepositFromDir(filterOnDepositor: Option[DepositorId], age: Int, state: State, onlyData: Boolean): Try[Unit] = {
    for {
      _ <- validateThatDepositDirectoryIsReadable()
      _ <- validateThatDepositPropertiesIsReadable()
      shouldDelete = shouldDeleteDepositDir(filterOnDepositor, age, state)
      _ <- if (shouldDelete) deleteDepositFromDir(onlyData)
           else Success(())
    } yield ()
  }

  private def deleteDepositFromDir(onlyData: Boolean): Try[Unit] = {
    val depositorId = getDepositorId
    val depositState = getStateLabel
    if (onlyData) deleteOnlyDataFromDeposit(depositorId, depositState)
    else deleteDepositDirectory(depositorId, depositState)
  }

  private def deleteDepositDirectory(depositorId: Option[String], depositState: State): Try[Unit] = Try {
    logger.info(s"DELETE deposit for ${ depositorId.getOrElse("<unknown>") } from $depositState $depositDirPath")
    FileUtils.deleteDirectory(depositDirPath.toFile)
  }

  private def deleteOnlyDataFromDeposit(depositorId: Option[DepositorId], depositState: State): Try[Unit] = Try {
    depositDirPath.toFile.listFiles()
      .filter(_.getName != depositPropertiesFileName) // don't delete the deposit.properties file
      .map(_.toPath)
      .foreach(path => {
        validateThatFileIsReadable(path)
          .doIfSuccess(_ => {
            logger.info(s"DELETE data from deposit for ${ depositorId.getOrElse("<unknown>") } from $depositState $depositDirPath")
            FileUtils.deleteQuietly(path.toFile)
          }).unsafeGetOrThrow
      })
  }

  private def shouldDeleteDepositDir(filterOnDepositor: Option[DepositorId], age: Int, state: State): Boolean = {
    val depositorId: DepositorId = getDepositorId.orNull
    val depositState = getStateLabel
    val ageRequirementIsMet = depositAgeIsLargerThanRequiredAge(age)

    // forall returns true for the empty set, see https://en.wikipedia.org/wiki/Vacuous_truth
    filterOnDepositor.forall(depositorId ==) && ageRequirementIsMet && depositState == state
  }

  private def getProperty(key: String): Option[String] = {
    depositProperties.flatMap(props => Option(props.getString(key)))
  }

  private def depositPropertiesFileExists: Boolean = {
    Files.exists(depositPropertiesFilePath)
  }

  private def end = DateTime.now(DateTimeZone.UTC)

  private def depositPropertiesFilePath = depositDirPath.resolve(depositPropertiesFileName)

  private def readDepositProperties(depositDir: Path): PropertiesConfiguration = {
    new PropertiesConfiguration() {
      setDelimiterParsingDisabled(true)
      load(depositDir.resolve(depositPropertiesFileName).toFile)
    }
  }

  private def findDepositProperties: Option[PropertiesConfiguration] = {
    if (depositPropertiesFileExists) {
      debug(s"Getting info from $depositDirPath")
      Some(readDepositProperties(depositDirPath))
    }
    else {
      logger.error(s"$depositPropertiesFileName does not exist for $depositDirPath")
      None
    }
  }
}
