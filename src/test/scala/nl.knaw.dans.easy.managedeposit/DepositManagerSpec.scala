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

import java.nio.file.attribute.PosixFilePermission

import better.files.File
import nl.knaw.dans.easy.managedeposit.State.{ FAILED, REJECTED, SUBMITTED, UNKNOWN }
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.BeforeAndAfterEach

import scala.util.{ Failure, Success }

class DepositManagerSpec extends TestSupportFixture with BeforeAndAfterEach {

  lazy private val depositDir = {
    val path = testDir / "inputForEasyManageDeposit/"
    if (path.exists) path.delete()
    path.createDirectories()
    path
  }

  private val depositOne = depositDir / "aba410b6-1a55-40b2-9ebe-6122aad00285"
  private val depositOnePath = depositOne.toJava.toPath
  private val depositDirWithoutProperties = depositDir / "deposit-no-properties"
  private val depositDirWithoutPropertiesPath = depositDirWithoutProperties.toJava.toPath
  private val nonExistingDeposit = depositDir / "deposit-3"

  override def beforeEach(): Unit = {
    super.beforeEach()
    depositDir.clear()
    File(getClass.getResource("/inputForEasyManageDeposit/").toURI).copyTo(depositDir)
  }

  "getters" should "retrieve the values from deposit.properties if it exists" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.getDepositId.value shouldBe "aba410b6-1a55-40b2-9ebe-6122aad00285"
    depositManager.getCreationTime.value shouldBe new DateTime("2018-11-08T22:05:53.992Z")
    depositManager.getDepositorId.value shouldBe "user001"
    depositManager.getStateLabel shouldBe SUBMITTED
    depositManager.getStateDescription.value shouldBe "Deposit is valid and ready for post-submission processing"
    depositManager.getFedoraIdentifier.value shouldBe "fedora12345"
    depositManager.getDoiRegistered.value shouldBe "yes"
    depositManager.getDoiIdentifier.value shouldBe "aba410b6-9090-40b2-8080-6122aad00285"
  }

  it should "return an empty properties file if there is not a deposit.propertiesFile available" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.getStateDescription shouldBe empty
    depositManager.getStateLabel shouldBe UNKNOWN
    depositManager.getDepositorId shouldBe empty
    depositManager.getCreationTime shouldBe empty
    depositManager.getDoiIdentifier shouldBe empty
    depositManager.getDoiRegistered shouldBe empty
    depositManager.getDepositId.value shouldBe depositDirWithoutPropertiesPath.getFileName.toString
  }


  "validateThatDirIsReadable" should "throw an NotReadableException if a directory does not exist" in {
    val depositManager = new DepositManager(nonExistingDeposit.toJava.toPath)
    depositManager.validateThatDepositDirectoryIsReadable() should matchPattern {
      case Failure(nre: NotReadableException) =>
    }
  }

  it should "throw an NotReadableException if the user does not have the permission to read the dir" in {
    depositDirWithoutProperties.removePermission(PosixFilePermission.OWNER_READ)
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.validateThatDepositDirectoryIsReadable() should matchPattern {
      case Failure(nre: NotReadableException) =>
    }
    depositDirWithoutProperties.addPermission(PosixFilePermission.OWNER_READ)
  }

  it should "not throw an exception if user does have permission to read the folder" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.validateThatDepositDirectoryIsReadable() should matchPattern {
      case Success(_) =>
    }
  }

  "validateUserRightsForPropertiesFile" should "throw an exception if an user does not have permission to read the deposit.properties file" in {
    val depositManager = new DepositManager(depositOnePath)
    val properties = depositOne / "deposit.properties"
    properties.removePermission(PosixFilePermission.OWNER_READ)
    depositManager.validateUserRightsForPropertiesFile() should matchPattern {
      case Failure(nre: NotReadableException) =>
    }
    properties.addPermission(PosixFilePermission.OWNER_READ)
  }

  it should "not throw an exception if the deposit.properties file does not exist" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.validateUserRightsForPropertiesFile() should matchPattern {
      case Success(_) =>
    }
  }

  "validateThatDepositPropertiesIsReadable" should "throw an exception if an user does not have permission to read the deposit.properties file" in {
    val depositManager = new DepositManager(depositOnePath)
    val properties = depositOne / "deposit.properties"
    properties.removePermission(PosixFilePermission.OWNER_READ)
    depositManager.validateThatDepositPropertiesIsReadable() should matchPattern {
      case Failure(nre: NotReadableException) =>
    }
    properties.addPermission(PosixFilePermission.OWNER_READ)
  }

  it should "throw an exception if deposit.properties does not exist" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.validateThatDepositPropertiesIsReadable() should matchPattern {
      case Failure(nre: NotReadableException) =>
    }
  }

  it should "not throw an null pointer exception if deposit.properties does exist" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.validateThatDepositPropertiesIsReadable() should matchPattern {
      case Success(_) =>
    }
  }

  "getDepositId" should "return the id field bag-store.bag-id field of deposit.properties file" in {
    val depositManager = new DepositManager(depositOnePath)
    val depositId = depositManager.getDepositId
    depositId.value shouldBe depositOnePath.getFileName.toString
  }

  it should "return the name of the directory when the file deposit.properties does not exist" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    val depositId = depositManager.getDepositId
    depositId.value shouldBe "deposit-no-properties"
  }

  "saveProperties" should "save the updated state to the deposit.properties file" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.getStateLabel shouldBe SUBMITTED
    depositManager.setState(REJECTED)
    depositManager.saveProperties()

    // new manager to check the updated state
    val updatedDepositManager = new DepositManager(depositOnePath)
    updatedDepositManager.getStateLabel shouldBe REJECTED
  }

  it should "not return a null pointer if the deposit.properties cannot be found" in {
    val depositManagerNoProperties = new DepositManager(depositDirWithoutPropertiesPath)
    depositManagerNoProperties.setState(FAILED)
    depositManagerNoProperties.saveProperties()
  }

  "getStateLabel" should "return State.UNKNOWN when deposit.properties as an non-recognized" in {
    val setupDepositManager = new DepositManager(depositOnePath)
    setupDepositManager.setProperty("state.label", "NON-EXISTING-VALUE")
    setupDepositManager.saveProperties()

    val depositManager = new DepositManager(depositOnePath)
    depositManager.getStateLabel
  }

  "depositAgeIsLargerThanMinimalRequiredAge" should "return true if required age is 4 days and the deposit age is 5 days " in {
    setCreationDateForTesting(daysAgo = 5)

    val depositManager = new DepositManager(depositOnePath)
    depositManager.depositAgeIsLargerThanRequiredAge(4) shouldBe true
  }


  it should "return false if required age is 5 days and the deposit age is 4 days " in {
    setCreationDateForTesting(daysAgo = 4)

    val depositManager = new DepositManager(depositOnePath)
    depositManager.depositAgeIsLargerThanRequiredAge(5) shouldBe false
  }

  it should "return false if required age is 4 days and the deposit age is 4 days " in {
    setCreationDateForTesting(daysAgo = 4)

    val depositManager = new DepositManager(depositOnePath)
    depositManager.depositAgeIsLargerThanRequiredAge(4) shouldBe false
  }

  it should "return false if the deposit.properties file is not found " in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.depositAgeIsLargerThanRequiredAge(3) shouldBe false
  }

  "getNumberOfContinuedDeposits" should "return 2 when there are 2 zipped file in deposit dir" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.getNumberOfContinuedDeposits shouldBe 2

  }

  it should "return 0 when there are 0 zipped file in deposit dir" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.getNumberOfContinuedDeposits shouldBe 0

  }

  it should "0 when directory does not exist" in {
    val depositManager = new DepositManager(nonExistingDeposit.toJava.toPath)
    depositManager.getNumberOfContinuedDeposits shouldBe 0
  }

  private def setCreationDateForTesting(daysAgo: Int) = {
    val setupDepositManager = new DepositManager(depositOnePath)
    val fiveDaysAgo = DateTime.now(DateTimeZone.UTC).minusDays(daysAgo)
    setupDepositManager.setProperty("creation.timestamp", fiveDaysAgo.toString)
    setupDepositManager.saveProperties()
  }
}
