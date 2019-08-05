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
import nl.knaw.dans.easy.managedeposit.State._
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.BeforeAndAfterEach

import scala.util.{ Failure, Success }

class DepositManagerSpec extends TestSupportFixture with BeforeAndAfterEach {

  private val depositDirWithoutProperties = depositDir / "deposit-no-properties"
  private val depositDirWithoutPropertiesPath = depositDirWithoutProperties.path
  private val nonExistingDeposit = depositDir / "deposit-3"
  private val nonExistingDepositPath = nonExistingDeposit.path
  private val depositWithoutDepositor = depositDir / "deposit-no-depositor-id"
  private val depositWithoutDepositorPath = depositWithoutDepositor.path
  private val ruimteReis01 = depositDir / "input-ruimtereis01"
  private val ruimteReis01Path = ruimteReis01.path
  private val ruimteReis02 = depositDir / "input-ruimtereis02"
  private val ruimteReis02Path = ruimteReis02.path
  private val ruimteReis05 = depositDir / "input-ruimtereis05"
  private val ruimteReis05Path = ruimteReis05.path

  override def beforeEach(): Unit = {
    super.beforeEach()
    depositDir.clear()
    File(getClass.getResource("/inputForEasyManageDeposit/").toURI).copyTo(depositDir)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    addOwnerReadPermissionIfExist(depositOne)
    addOwnerReadPermissionIfExist(depositOne / "deposit.properties")
    addOwnerReadPermissionIfExist(depositOne / "bag.zip.1")
  }

  private def addOwnerReadPermissionIfExist(file: File): Any = {
    if (file.exists) {
      file.addPermission(PosixFilePermission.OWNER_READ)
    }
  }

  "getters" should "retrieve the values from deposit.properties if it exists" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.getDepositId.value shouldBe "aba410b6-1a55-40b2-9ebe-6122aad00285"
    depositManager.getCreationTime.value shouldBe new DateTime("2018-11-08T22:05:53.992Z")
    depositManager.getDepositorId.value shouldBe "user001"
    depositManager.getStateLabel shouldBe SUBMITTED
    depositManager.getStateDescription.value shouldBe "Deposit is valid and ready for post-submission processing"
    depositManager.getFedoraIdentifier.value shouldBe "fedora12345"
    depositManager.getDansDoiRegistered.value shouldBe "yes"
    depositManager.getDepositOrigin.value shouldBe "SWORD2"
    depositManager.getDoiIdentifier.value shouldBe "aba410b6-9090-40b2-8080-6122aad00285"
  }

  it should "return an empty properties file if there is not a deposit.propertiesFile available" in {
    val depositManager = new DepositManager(depositDirWithoutPropertiesPath)
    depositManager.getStateDescription shouldBe empty
    depositManager.getStateLabel shouldBe UNKNOWN
    depositManager.getDepositorId shouldBe empty
    depositManager.getCreationTime shouldBe empty
    depositManager.getDoiIdentifier shouldBe empty
    depositManager.getDansDoiRegistered shouldBe empty
    depositManager.getDepositId.value shouldBe depositDirWithoutPropertiesPath.getFileName.toString
  }

  "validateUserCanReadTheDepositDirectoryAndTheDepositProperties" should "succeed if all right are set properly" in {
    new DepositManager(depositOnePath).validateUserCanReadTheDepositDirectoryAndTheDepositProperties() shouldBe a[Success[_]]
  }

  it should "return a failure if the user has no access right to the depositProperties" in {
    val properties = depositOne / "deposit.properties"
    properties.removePermission(PosixFilePermission.OWNER_READ)
    new DepositManager(depositOnePath).validateUserCanReadTheDepositDirectoryAndTheDepositProperties() should matchPattern {
      case Failure(e: NotReadableException) if e.getMessage == s"cannot read ${ depositOne / "deposit.properties" }" =>
    }
  }

  it should "return a failure if the user has no access right to the deposit" in {
    removeOwnerPermissions(depositOne)
    new DepositManager(depositOnePath).validateUserCanReadTheDepositDirectoryAndTheDepositProperties() should matchPattern {
      case Failure(e: NotReadableException) if e.getMessage == s"cannot read $depositOne" =>
    }
  }

  it should "fail if the depositProperties do not exist" in {
    new DepositManager(depositDirWithoutPropertiesPath)
      .validateUserCanReadTheDepositDirectoryAndTheDepositProperties() should matchPattern {
      case Failure(nre: NotReadableException) if nre.getMessage == s"cannot read ${ depositDirWithoutProperties / "deposit.properties" }" =>
    }
  }

  it should "fail if the deposit does not exist" in {
    new DepositManager(nonExistingDepositPath)
      .validateUserCanReadTheDepositDirectoryAndTheDepositProperties() should matchPattern {
      case Failure(nre: NotReadableException) if nre.getMessage == s"cannot read $nonExistingDeposit" =>
    }
  }

  "getLastModifiedTimestamp" should "return an option from the latest modified TimeStamp" in {
    val today = DateTime.now()
    new DepositManager(depositOnePath).getLastModifiedTimestamp should matchPattern {
      case Success(Some(d: DateTime)) if Math.abs(d.minus(today.getMillis).getMillis) < 100000 =>
    }
  }

  it should "fail if the user does not have right to read the deposit directory" in {
    removeOwnerPermissions(depositOne)
    new DepositManager(depositOnePath).getLastModifiedTimestamp should matchPattern {
      case Failure(nre: NotReadableException) if nre.getMessage == s"cannot read $depositOne" =>
    }
  }

  it should "fail if a zip in the deposit is not readable" in {
    val zip = depositOne / "bag.zip.1"
    zip.removePermission(PosixFilePermission.OWNER_READ)
    new DepositManager(depositOnePath).getLastModifiedTimestamp should matchPattern {
      case Failure(nre: NotReadableException) if nre.getMessage == s"cannot read $zip" =>
    }
  }

  it should "fail if the deposit directory is not readable" in {
    new DepositManager(nonExistingDepositPath).getLastModifiedTimestamp should matchPattern {
      case Failure(nre: NotReadableException) if nre.getMessage == s"cannot read $nonExistingDeposit" =>
    }
  }

  "getDepositId" should "return the id field bag-store.bag-id field of deposit.properties file" in {
    new DepositManager(depositOnePath).getDepositId.value shouldBe depositOnePath.getFileName.toString
  }

  it should "return the name of the directory when the file deposit.properties does not exist" in {
    new DepositManager(depositDirWithoutPropertiesPath).getDepositId.value shouldBe "deposit-no-properties"
  }

  "saveProperties" should "save the updated state to the deposit.properties file" in {
    val depositManager = new DepositManager(depositOnePath)
    depositManager.getStateLabel shouldBe SUBMITTED
    depositManager.setState(REJECTED)
    depositManager.saveProperties()

    // new manager to check the updated state
    new DepositManager(depositOnePath).getStateLabel shouldBe REJECTED
    depositManager.getStateLabel shouldBe REJECTED
  }

  it should "not throw a NullPointerException if the deposit.properties cannot be found" in {
    val depositManagerNoProperties = new DepositManager(depositDirWithoutPropertiesPath)
    depositManagerNoProperties.setState(FAILED)
    depositManagerNoProperties.saveProperties()
  }

  "getStateLabel" should "return State.UNKNOWN when deposit.properties as an non-recognized" in {
    val setupDepositManager = new DepositManager(depositOnePath)
    setupDepositManager.setProperty("state.label", "NON-EXISTING-VALUE")
    setupDepositManager.saveProperties()
    new DepositManager(depositOnePath).getStateLabel shouldBe UNKNOWN
    setupDepositManager.getStateLabel shouldBe UNKNOWN
  }

  "depositAgeIsLargerThanMinimalRequiredAge" should "return true if required age is 4 days and the deposit age is 5 days " in {
    setCreationDateForTesting(daysAgo = 5)
    new DepositManager(depositOnePath).depositAgeIsLargerThanRequiredAge(4) shouldBe true
  }

  it should "return false if required age is 5 days and the deposit age is 4 days " in {
    setCreationDateForTesting(daysAgo = 4)
    new DepositManager(depositOnePath).depositAgeIsLargerThanRequiredAge(5) shouldBe false
  }

  it should "return false if required age is 4 days and the deposit age is 4 days " in {
    setCreationDateForTesting(daysAgo = 4)
    new DepositManager(depositOnePath).depositAgeIsLargerThanRequiredAge(4) shouldBe false
  }

  it should "return false if the deposit.properties file is not found " in {
    new DepositManager(depositDirWithoutPropertiesPath).depositAgeIsLargerThanRequiredAge(3) shouldBe false
  }

  "getNumberOfContinuedDeposits" should "return 2 when there are 2 zipped file in deposit dir" in {
    new DepositManager(depositOnePath).getNumberOfContinuedDeposits shouldBe 2
  }

  it should "return 0 when there are 0 zipped file in deposit dir" in {
    new DepositManager(depositDirWithoutPropertiesPath).getNumberOfContinuedDeposits shouldBe 0
  }

  it should "0 when directory does not exist" in {
    val depositManager = new DepositManager(nonExistingDepositPath)
    depositManager.getNumberOfContinuedDeposits shouldBe 0
  }

  "deleteDepositFromDir" should "delete the deposit directory from a deposit directory if all conditions are met and onlyData = false" in {
    ruimteReis01 should exist
    val depositManager = new DepositManager(ruimteReis01Path)
    depositManager.deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) shouldBe a[Success[_]]
    ruimteReis01 shouldNot exist
  }

  it should "keep the directory and the properties file if the conditions are met and onlyData = true" in {
    ruimteReis01 should exist
    val depositManager = new DepositManager(ruimteReis01Path)
    depositManager.deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = true) shouldBe a[Success[_]]
    ruimteReis01 should exist
    ruimteReis01.list.map(_.name).toList should contain only "deposit.properties"
  }

  it should "not delete anything if depositor id does not match" in {
    ruimteReis01 should exist
    val depositManager = new DepositManager(ruimteReis01Path)
    depositManager.deleteDepositFromDir(Some("user009"), 1, SUBMITTED, onlyData = true) shouldBe a[Success[_]]
    ruimteReis01 should exist
    ruimteReis01.list.find(file => file.name == "bag").value shouldBe (File(ruimteReis01Path) / "bag")
  }

  it should "not delete anything if the age requirement does not match" in {
    ruimteReis01 should exist
    val ageThreeHundredYears = 365 * 300 // approximately 300 years
    new DepositManager(ruimteReis01Path).deleteDepositFromDir(Some("user001"), ageThreeHundredYears, SUBMITTED, onlyData = true) shouldBe a[Success[_]]
    ruimteReis01 should exist
    (File(ruimteReis01Path) / "bag") should exist
  }

  it should "not delete any directories if a non existent path is given" in {
    val initialDirectories = depositDir.list.size
    new DepositManager(nonExistingDepositPath).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) should matchPattern {
      case Failure(NotReadableException(`nonExistingDepositPath`, _)) =>
    }
    initialDirectories shouldBe depositDir.list.size
  }

  it should "delete the directory if no depositor id is given and other conditions match" in {
    new DepositManager(ruimteReis01Path).deleteDepositFromDir(None, 1, SUBMITTED, onlyData = false) shouldBe a[Success[_]]
    ruimteReis01 shouldNot exist
  }

  it should "not delete the directory if no depositor id is given but the state does not match" in {
    new DepositManager(ruimteReis01Path).deleteDepositFromDir(None, 1, ARCHIVED, onlyData = false) shouldBe a[Success[_]]
    ruimteReis01 should exist
  }

  it should "not delete the deposit if the depositor id is not in the properties" in {
    new DepositManager(depositWithoutDepositorPath).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) shouldBe a[Success[_]]
    depositWithoutDepositor should exist
  }

  it should "delete the deposit if the depositor id is not in the properties, when no filterOnDepositor id given to match" in {
    new DepositManager(depositWithoutDepositorPath).deleteDepositFromDir(None, 1, SUBMITTED, onlyData = false) shouldBe a[Success[_]]
    depositWithoutDepositor shouldNot exist
  }

  it should "be able to delete an zipped bag if something during the ingest-flow went wrong" in {
    ruimteReis05.list.size shouldBe 2
    (ruimteReis05 / "bag.zip.1") should exist
    new DepositManager(ruimteReis05Path).deleteDepositFromDir(None, 1, REJECTED, onlyData = true) shouldBe a[Success[_]]
    ruimteReis05 should exist
    (ruimteReis05 / "bag.zip.1") shouldNot exist
    ruimteReis05.list.toSeq should have size 1
  }

  it should "be able to delete multiple zipped bags if something during the ingest-flow went wrong" in {
    depositOne.list.size shouldBe 3
    (depositOne / "bag.zip.1") should exist
    (depositOne / "bag.zip.2") should exist
    new DepositManager(depositOnePath).deleteDepositFromDir(None, 1, SUBMITTED, onlyData = true) shouldBe a[Success[_]]
    depositOne should exist
    (depositOne / "bag.zip.1") shouldNot exist
    (depositOne / "bag.zip.2") shouldNot exist
    depositOne.list.toSeq should have size 1
  }

  it should "not delete a directory if the creation date is not found" in {
    ruimteReis02 should exist
    new DepositManager(ruimteReis02Path).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) shouldBe a[Success[_]]
    ruimteReis02 should exist
  }

  it should "return an NotReadableException if the user does not have the permission to read the dir" in {
    removeOwnerPermissions(depositOne)
    new DepositManager(depositOnePath).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) should matchPattern {
      case Failure(NotReadableException(`depositOnePath`, _)) =>
    }
    depositOne should exist
  }

  it should "return an NotReadableException if the user does not have the permission to read the deposit.properties file" in {
    val properties = depositOne / "deposit.properties"
    val propertiesPath = properties.path
    properties.removePermission(PosixFilePermission.OWNER_READ)
    new DepositManager(depositOnePath).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) should matchPattern {
      case Failure(NotReadableException(`propertiesPath`, _)) =>
    }
    depositOne should exist
  }

  it should "return a NotReadAbleException when the deposit.properties file is absent" in {
    val propertiesPath = (depositDirWithoutProperties / "deposit.properties").path
    new DepositManager(depositDirWithoutPropertiesPath).deleteDepositFromDir(Some("user001"), 1, SUBMITTED, onlyData = false) should matchPattern {
      case Failure(NotReadableException(`propertiesPath`, _)) =>
    }
  }

  "getDeposit" should "succeed" in {
    new DepositManager(depositOnePath).getDepositInformation("")(List("10.17026/", "10.5072/")) should matchPattern {
      case Success(d: DepositInformation) if d.depositor == "user001" && d.state == SUBMITTED =>
    }
  }

  it should "succeed if a directory has no dataset.xml but has a doi in it deposit.properties" in {
    new DepositManager(depositWithoutDepositorPath).getDepositInformation("")(List("10.17026/", "10.5072/")) should matchPattern {
      case Success(d: DepositInformation) if d.depositor == notAvailable
        && d.state == SUBMITTED
        && d.bagDirName == notAvailable =>
    }
  }

  it should "fail if the deposit.properties do not exist" in {
    new DepositManager(depositDirWithoutPropertiesPath).getDepositInformation("")(List("10.17026/", "10.5072/")) should matchPattern {
      case Failure(e: NotReadableException) if e.getMessage == s"cannot read $depositDirWithoutPropertiesPath/deposit.properties" =>
    }
  }

  private def removeOwnerPermissions(file: File): Unit = {
    file.removePermission(PosixFilePermission.OWNER_READ)
  }

  private def setCreationDateForTesting(daysAgo: Int): Unit = {
    val setupDepositManager = new DepositManager(depositOnePath)
    val fiveDaysAgo = DateTime.now(DateTimeZone.UTC).minusDays(daysAgo)
    setupDepositManager.setProperty("creation.timestamp", fiveDaysAgo.toString)
    setupDepositManager.saveProperties()
  }
}
