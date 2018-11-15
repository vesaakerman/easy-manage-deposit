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
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.{ DateTime, DateTimeZone, Duration }

import scala.util.{ Failure, Success, Try }

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
    getProperty("creation.timestamp").map(stringTime => new DateTime(stringTime))
  }

  /**
   * getStateLabel returns the state of the deposit based on its deposit.properties file
   * if the state.label entry is absent or has a unrecognised value State.UNKNOWN is returned
   *
   * @return State enumeration
   */
  def getStateLabel: State = {
    Try(getProperty(stateLabelKey).fold(UNKNOWN)(state => State.withName(state)))
      .getOrElse(UNKNOWN)
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

  def validateThatFileIsReadable(path: Path): Try[Unit] = {
    if (!Files.isReadable(path)) {
      Failure(NotReadableException(path))
    }
    else {
      Success(())
    }
  }

  def depositAgeIsLargerThanRequiredAge(age: Int): Boolean = {
    getCreationTime.fold(false)(start => new Duration(start, end).getStandardDays > age)
  }

  def validateThatDepositDirectoryIsReadable(): Try[Unit] = {
    validateThatFileIsReadable(depositDirPath)
  }

  def validateThatDepositPropertiesIsReadable(): Try[Unit] = {
    validateThatFileIsReadable(depositPropertiesFilePath)
  }

  def validateUserRightsForPropertiesFile(): Try[Unit] = {
    if (depositPropertiesFileExists && !Files.isReadable(depositPropertiesFilePath)) {
      Failure(NotReadableException(depositPropertiesFilePath))
    }
    else {
      Success(())
    }
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
