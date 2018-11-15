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

import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.{ Calendar, UUID }

import nl.knaw.dans.easy.managedeposit.State._
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

class ReportServiceSpec extends TestSupportFixture with Matchers with MockFactory {

  "filterDepositsByDepositor" should "only return deposits where the id of the depositor matches" in {
    val deposits = List(
      createDeposit("dans-1", DRAFT),
      createDeposit("dans-1", SUBMITTED),
      createDeposit("dans-3", SUBMITTED),
    )
    ReportService.filterDepositsByDepositor(deposits, Some("dans-1")).size shouldBe 2
  }

  it should "should return empty list if none of the id's matches" in {
    val deposits = List(
      createDeposit("dans-2", DRAFT),
      createDeposit("dans-5", SUBMITTED),
      createDeposit("dans-3", SUBMITTED),
    )
    ReportService.filterDepositsByDepositor(deposits, Some("dans-1")) shouldBe empty
  }

  it should "should return all deposits if the given depositorId is empty" in {
    val deposits = List(
      createDeposit("dans-2", DRAFT),
      createDeposit("dans-5", SUBMITTED),
      createDeposit("dans-3", SUBMITTED),
    )
    ReportService.filterDepositsByDepositor(deposits, None).size shouldBe 3
  }

  it should "should return all deposits if the given depositorId is empty and one of the depositorIds is null" in {
    val deposits = List(
      createDeposit(null, DRAFT),
      createDeposit("dans-5", SUBMITTED),
      createDeposit("dans-3", SUBMITTED),
    )
    ReportService.filterDepositsByDepositor(deposits, None).size shouldBe 3
  }

  it should "should skip all depositorId = null deposits if the given depositorId is given" in {
    val deposits = List(
      createDeposit(null, DRAFT),
      createDeposit("dans-1", SUBMITTED),
      createDeposit("dans-1", SUBMITTED),
    )
    ReportService.filterDepositsByDepositor(deposits, Some("dans-1")).size shouldBe 2
  }

  "groupDepositsByState" should "return a map with all deposits sorted By their state and null should be mapped to UNKNOWN" in {
    val deposits: List[Deposit] = createDeposits
    val mappedByState = ReportService.groupAndSortDepositsAlphabeticallyByState(deposits).toMap
    mappedByState.getOrElse(ARCHIVED, Seq()).size shouldBe 2
    mappedByState.getOrElse(DRAFT, Seq()).size shouldBe 1
    mappedByState.getOrElse(FINALIZING, Seq()).size shouldBe 1
    mappedByState.getOrElse(INVALID, Seq()).size shouldBe 1
    mappedByState.getOrElse(REJECTED, Seq()).size shouldBe 1
    mappedByState.getOrElse(STALLED, Seq()).size shouldBe 1
    mappedByState.getOrElse(SUBMITTED, Seq()).size shouldBe 4
    mappedByState.getOrElse(UNKNOWN, Seq()).size shouldBe 4 // 2 + 2 null values
  }

  class MockPrintStream extends PrintStream((testDir / "fancySummary.txt").path.toString) {
    override def println(): Unit = super.println()
  }

  "output Summary" should "should contain all deposits" in {
    if (!testDir.exists) { //ouptut of rapport is written to target
      testDir.createDirectories()
    }

    val deposits = createDeposits
    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    val currentTime = format.format(now)

    val mockedPrintStream = mock[MockPrintStream]

    inSequence {
      (mockedPrintStream.println(_: String)) expects "Grand totals:" once()
      (mockedPrintStream.println(_: String)) expects "-------------" once()
      (mockedPrintStream.println(_: String)) expects s"Timestamp          : $currentTime" once()
      (mockedPrintStream.println(_: String)) expects f"Number of deposits : ${ 15 }%10d" once()
      (mockedPrintStream.println(_: String)) expects "Total space        :      1.8 M" once() // (129000 * 15 ) / (1024 * 1024)
      mockedPrintStream.println _ expects() once()
      (mockedPrintStream.println(_: String)) expects "Per state:" once()
      (mockedPrintStream.println(_: String)) expects "----------" once()
      (mockedPrintStream.println(_: String)) expects s"$ARCHIVED   :     2 (   252.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$DRAFT      :     1 (   126.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$FINALIZING :     1 (   126.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$INVALID    :     1 (   126.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$REJECTED   :     1 (   126.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$STALLED    :     1 (   126.0 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$SUBMITTED  :     4 (   503.9 K)" once()
      (mockedPrintStream.println(_: String)) expects s"$UNKNOWN    :     4 (   503.9 K)" once()
      mockedPrintStream.println _ expects() once()
    }
    ReportService.outputSummary(deposits, Some("dans-1"))(mockedPrintStream)
  }

  private def createDeposit(depositorId: String, state: State) = {
    Deposit(UUID.randomUUID().toString, null, depositorId, state, "", DateTime.now().minusDays(3).toString(), 2, 129000, "")
  }

  private def createDeposits = {
    val deposits = List(
      createDeposit("dans-1", ARCHIVED),
      createDeposit("dans-1", ARCHIVED),
      createDeposit("dans-1", DRAFT),
      createDeposit("dans-1", FINALIZING),
      createDeposit("dans-1", INVALID),
      createDeposit("dans-1", REJECTED),
      createDeposit("dans-1", STALLED),
      createDeposit("dans-1", SUBMITTED),
      createDeposit("dans-1", SUBMITTED),
      createDeposit("dans-1", SUBMITTED),
      createDeposit("dans-1", SUBMITTED), // duplicate deposits are allowed
      createDeposit("dans-1", UNKNOWN),
      createDeposit("dans-1", UNKNOWN),
      createDeposit("dans-1", null), // mapped and added to unknown
      createDeposit("dans-1", null),
    )
    deposits
  }
}

