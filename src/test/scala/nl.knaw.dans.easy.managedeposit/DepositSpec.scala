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

import java.nio.file.Paths

import better.files.File
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.BeforeAndAfterEach

class DepositSpec extends TestSupportFixture with BeforeAndAfterEach {

  lazy private val depositDir = {
    val path = testDir / "inputForEasyManageDeposit/"
    if (path.exists) path.delete()
    path.createDirectories()
    path
  }

  private val dansDoiRegistered = (depositDir / "dans-doi-registered").path
  private val dansDoiNotRegistered = (depositDir / "dans-doi-not-registered").path
  private val dansDoiNoRegistration = (depositDir / "dans-doi-no-registration").path
  private val dansDoiNoRegistrationDepositFailed = (depositDir / "dans-doi-no-registration-deposit-failed").path
  private val dansDoiNoRegistrationDepositRejected = (depositDir / "dans-doi-no-registration-deposit-rejected").path
  private val otherDoi = (depositDir / "other-doi").path

  private val resourceDirString: String = Paths.get(getClass.getResource("/").toURI).toAbsolutePath.toString
  private val configuration = new Configuration("version x.y.z", new PropertiesConfiguration(resourceDirString + "/debug-config/application.properties"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    depositDir.clear()
    File(getClass.getResource("/inputForEasyManageDeposit/").toURI).copyTo(depositDir)
  }


  "doiRegistered" should "return a yes when DANS DOI and DOI is registered" in {
    val depositManager = new DepositManager(dansDoiRegistered)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "yes"
  }

  it should "return a no when DANS DOI and DOI is not registered" in {
    val depositManager = new DepositManager(dansDoiNotRegistered)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "no"
  }

  it should "return a yes when DANS DOI and DOI registration is not given and STATE is ARCHIVED" in {
    val depositManager = new DepositManager(dansDoiNoRegistration)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "yes"
  }

  it should "return a no when DANS DOI and DOI registration is not given and STATE is not ARCHIVED and not FAILED" in {
    val depositManager = new DepositManager(dansDoiNoRegistrationDepositRejected)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "no"
  }

  it should "return 'unknown' when DANS DOI and DOI registration is not given and STATE is FAILED" in {
    val depositManager = new DepositManager(dansDoiNoRegistrationDepositFailed)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "unknown"
  }

  it should "return a yes when NOT DANS DOI and DOI is not empty" in {
    val depositManager = new DepositManager(otherDoi)
    new EasyManageDepositApp(configuration).getDeposit(None, None, depositManager).get.get.doiRegistered shouldBe "yes"
  }
}
