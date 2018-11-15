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
import java.util.Calendar

import nl.knaw.dans.easy.managedeposit.State.{ State, UNKNOWN }

object ReportService {
  private val KB = 1024L
  private val MB = 1024L * KB
  private val GB = 1024L * MB
  private val TB = 1024L * GB

  def filterDepositsByDepositor(deposits: Deposits, depositor: Option[DepositorId] = None): Deposits = {
    depositor.map(d => deposits.filter(_.depositor == d)).getOrElse(deposits)
  }

  def groupAndSortDepositsAlphabeticallyByState(deposits: Deposits): Seq[(State, Seq[Deposit])] = {
    val groupedByState = deposits.groupBy(deposit => Option(deposit.state).getOrElse(UNKNOWN))
    groupedByState.toSeq.sortBy { case (state, _) => state } //sort alphabetically by state
  }

  def outputSummary(deposits: Deposits, depositor: Option[DepositorId] = None)(implicit printStream: PrintStream): Unit = {
    val selectedDeposits = filterDepositsByDepositor(deposits, depositor)
    val depositsGroupedByState = groupAndSortDepositsAlphabeticallyByState(selectedDeposits)

    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    val currentTime = format.format(now)

    printStream.println("Grand totals:")
    printStream.println("-------------")
    printStream.println(s"Timestamp          : $currentTime")
    printStream.println(f"Number of deposits : ${ selectedDeposits.size }%10d")
    printStream.println(s"Total space        : ${ formatStorageSize(selectedDeposits.map(_.storageSpace).sum) }")
    printStream.println()
    printStream.println("Per state:")
    printStream.println("----------")
    depositsGroupedByState.foreach { case (state, deposits) => printLineForDepositGroup(state, deposits) }
    printStream.println()
  }

  private def printLineForDepositGroup(state: State, depositGroup: Seq[Deposit])(implicit printStream: PrintStream) = {
    printStream.println(formatCountAndSize(depositGroup, state))
  }

  private def formatStorageSize(nBytes: Long): String = {
    def formatSize(unitSize: Long, unit: String): String = {
      f"${ nBytes / unitSize.toFloat }%8.1f $unit"
    }

    if (nBytes > 1.1 * TB) formatSize(TB, "T")
    else if (nBytes > 1.1 * GB) formatSize(GB, "G")
    else if (nBytes > 1.1 * MB) formatSize(MB, "M")
    else if (nBytes > 1.1 * KB) formatSize(KB, "K")
    else formatSize(1, "B")
  }

  private def formatCountAndSize(deposits: Seq[Deposit], filterOnState: State): String = {
    f"${ filterOnState.toString }%-10s : ${ deposits.size }%5d (${ formatStorageSize(deposits.map(_.storageSpace).sum) })"
  }
}
