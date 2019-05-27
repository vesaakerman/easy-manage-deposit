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

import java.io.{ ByteArrayOutputStream, PrintStream }
import java.text.SimpleDateFormat
import java.util.{ Calendar, UUID }

import nl.knaw.dans.easy.managedeposit.State._
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Inspectors, Matchers }

import scala.util.matching.Regex

class ReportGeneratorSpec extends TestSupportFixture
  with Matchers
  with MockFactory
  with Inspectors {

  private implicit val dansDoiPrefixes: List[String] = List("10.17026/", "10.5072/")

  "filterDepositsByDepositor" should "only return deposits where the id of the depositor matches" in {
    val deposits = List(
      createDeposit("dans-1", DRAFT, "SRC2"),
      createDeposit("dans-1", SUBMITTED, "SRC1"),
      createDeposit("dans-3", SUBMITTED, "SRC1"),
    )
    ReportGenerator.filterDepositsByDepositor(deposits, Some("dans-1")).size shouldBe 2
  }

  it should "should return empty list if none of the id's matches" in {
    val deposits = List(
      createDeposit("dans-2", DRAFT, "SRC2"),
      createDeposit("dans-5", SUBMITTED, "SRC1"),
      createDeposit("dans-3", SUBMITTED, "SRC1"),
    )
    ReportGenerator.filterDepositsByDepositor(deposits, Some("dans-1")) shouldBe empty
  }

  it should "should return all deposits if the given depositorId is empty" in {
    val deposits = List(
      createDeposit("dans-2", DRAFT, "SRC2"),
      createDeposit("dans-5", SUBMITTED, "SRC1"),
      createDeposit("dans-3", SUBMITTED, "SRC1"),
    )
    ReportGenerator.filterDepositsByDepositor(deposits, None).size shouldBe 3
  }

  it should "should return all deposits if the given depositorId is empty and one of the depositorIds is null" in {
    val deposits = List(
      createDeposit(null, DRAFT, "SRC2"),
      createDeposit("dans-5", SUBMITTED, "SRC1"),
      createDeposit("dans-3", SUBMITTED, "SRC1"),
    )
    ReportGenerator.filterDepositsByDepositor(deposits, None).size shouldBe 3
  }

  it should "should skip all depositorId = null deposits if the given depositorId is given" in {
    val deposits = List(
      createDeposit(null, DRAFT, "SRC2"),
      createDeposit("dans-1", SUBMITTED, "SRC1"),
      createDeposit("dans-1", SUBMITTED, "SRC1"),
    )
    ReportGenerator.filterDepositsByDepositor(deposits, Some("dans-1")).size shouldBe 2
  }

  "groupDepositsByState" should "return a map with all deposits sorted By their state and null should be mapped to UNKNOWN" in {
    val deposits: List[DepositInformation] = createDeposits
    val mappedByState = ReportGenerator.groupAndSortDepositsAlphabeticallyByState(deposits).toMap
    mappedByState.getOrElse(ARCHIVED, Seq()).size shouldBe 2
    mappedByState.getOrElse(DRAFT, Seq()).size shouldBe 1
    mappedByState.getOrElse(FINALIZING, Seq()).size shouldBe 1
    mappedByState.getOrElse(INVALID, Seq()).size shouldBe 1
    mappedByState.getOrElse(REJECTED, Seq()).size shouldBe 1
    mappedByState.getOrElse(SUBMITTED, Seq()).size shouldBe 4
    mappedByState.getOrElse(UNKNOWN, Seq()).size shouldBe 4 // 2 + 2 null values
  }

  "output Summary" should "should contain all deposits" in {
    if (!testDir.exists) { //output of rapport is written to target
      testDir.createDirectories()
    }

    val deposits = createDeposits
    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    val currentTime = format.format(now)
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos, true)
    try {
      ReportGenerator.outputSummary(deposits, Some("dans-1"))(ps)
    } finally {
      ps.close()
    }

    val reportOutput = baos.toString
    reportOutput should include(s"Timestamp          : $currentTime")
    reportOutput should include(f"Number of deposits : ${ 16 }%10d")
    reportOutput should include("Total space        :      2.0 M") // (129000 * 16 ) / (1024 * 1024)
    reportOutput should include regex toStateDetailsRegex(ARCHIVED, 2, 252.0)
    reportOutput should include regex toStateDetailsRegex(DRAFT, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(FINALIZING, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(INVALID, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(IN_REVIEW, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(FEDORA_ARCHIVED, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(REJECTED, 1, 126.0)
    reportOutput should include regex toStateDetailsRegex(SUBMITTED, 4, 503.9)
    reportOutput should include regex toStateDetailsRegex(UNKNOWN, 4, 503.9)
  }

  "outputErrorReport" should "only print the deposits containing an error" in {
    val baos = new ByteArrayOutputStream()
    val errorDeposit = createDeposit("dans-0", ARCHIVED, "SRC1").copy(dansDoiRegistered = Some(false)) //violates the rule ARCHIVED must be registered when DANS doi
    val noDansDoiDeposit = createDeposit("dans-1", ARCHIVED, "SRC1").copy(dansDoiRegistered = Some(false), doiIdentifier = "11.11111/other-doi-123")
    val ps: PrintStream = new PrintStream(baos, true)
    val deposits = List(
      errorDeposit,
      noDansDoiDeposit, //does not violate any rule
      createDeposit("dans-2", SUBMITTED, "SRC1"), //does not violate any rule
      createDeposit("dans-3", SUBMITTED, "SRC1"), //does not violate any rule
    )
    outputErrorReportManaged(ps, deposits)
    val errorReport = baos.toString
    errorReport should include(createCsvRow(errorDeposit)) // only the first deposit should be added to the report
    errorReport should not include createCsvRow(noDansDoiDeposit)
  }

  it should "not print any csv rows if no deposits violate the rules" in {
    val baos = new ByteArrayOutputStream()
    val ps: PrintStream = new PrintStream(baos, true)
    val deposits = List(
      createDeposit("dans-0", DRAFT, "SRC2").copy(dansDoiRegistered = Some(false)),
      createDeposit("dans-1", SUBMITTED, "SRC1"),
      createDeposit("dans-1", SUBMITTED, "SRC1"),
    )
    outputErrorReportManaged(ps, deposits)

    val errorReport = baos.toString
    deposits.foreach(deposit => errorReport should not include createCsvRow(deposit)) // None of the deposits should be added to the report
  }

  it should "print any deposit that has one of the states null, UNKNOWN, INVALID, FAILED, REJECTED or ARCHIVED + not-registered" in {
    val baos = new ByteArrayOutputStream()
    val ps: PrintStream = new PrintStream(baos, true)
    val deposits = List(
      createDeposit("dans-0", ARCHIVED, "SRC1").copy(dansDoiRegistered = Some(false)), //violates the rule ARCHIVED must be registered
      createDeposit("dans-1", FAILED, "SRC1"),
      createDeposit("dans-2", REJECTED, "SRC1"),
      createDeposit("dans-3", INVALID, "SRC2"),
      createDeposit("dans-4", UNKNOWN, "SRC1"),
      createDeposit("dans-5", null, "SRC1"),
    )
    outputErrorReportManaged(ps, deposits)

    val errorReport = baos.toString
    forEvery(deposits)(deposit => errorReport should include(createCsvRow(deposit))) //all deposits should be added to the report
  }

  private def outputErrorReportManaged(ps: PrintStream, deposits: List[DepositInformation]): Unit = {
    try {
      ReportGenerator.outputErrorReport(deposits)(ps)
    } finally {
      ps.close()
    }
  }

  private def toStateDetailsRegex(state: State, amount: Int, size: Double): Regex = s"$state.+$amount.+$size".r

  private def createCsvRow(deposit: DepositInformation): String = {
    s"${ deposit.depositor }," +
      s"${ deposit.depositId }," +
      s"${ Option(deposit.state).getOrElse("") }," +
      s"${ deposit.source }," +
      s"${ deposit.doiIdentifier }," +
      s"${ deposit.registeredString }," +
      s"${ deposit.fedoraIdentifier.toString }," +
      s"${ deposit.creationTimestamp }," +
      s"${ deposit.lastModified }," +
      s"${ deposit.description }," +
      s"${ deposit.numberOfContinuedDeposits.toString }," +
      s"${ deposit.storageSpace.toString }"
  }

  private def createDeposit(depositorId: String, state: State, source: String): DepositInformation = {
    DepositInformation(UUID.randomUUID().toString, "10.17026/dans-12345", Some(true), "FedoraId", depositorId, state, "", DateTime.now().minusDays(3).toString(), 2, 129000, "", source)
  }

  private def createDeposits = List(
    createDeposit("dans-1", ARCHIVED, "SRC1"),
    createDeposit("dans-1", ARCHIVED, "SRC1"),
    createDeposit("dans-1", DRAFT, "SRC2"),
    createDeposit("dans-1", FINALIZING, "SRC2"),
    createDeposit("dans-1", INVALID, "SRC1"),
    createDeposit("dans-1", REJECTED, "SRC1"),
    createDeposit("dans-1", IN_REVIEW, "SRC1"),
    createDeposit("dans-1", FEDORA_ARCHIVED, "SRC1"),
    createDeposit("dans-1", SUBMITTED, "SRC1"),
    createDeposit("dans-1", SUBMITTED, "SRC1"),
    createDeposit("dans-1", SUBMITTED, "SRC1"),
    createDeposit("dans-1", SUBMITTED, "SRC1"), // duplicate deposits are allowed
    createDeposit("dans-1", UNKNOWN, "SRC2"),
    createDeposit("dans-1", UNKNOWN, "SRC1"),
    createDeposit("dans-1", null, "SRC1"), // mapped and added to unknown
    createDeposit("dans-1", null, "SRC1"),
  )
}

