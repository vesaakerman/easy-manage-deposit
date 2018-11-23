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

object State extends Enumeration {
  type State = Value
  val ARCHIVED: State = Value("ARCHIVED")
  val DRAFT: State = Value("DRAFT")
  val FAILED: State = Value("FAILED")
  val FINALIZING: State = Value("FINALIZING")
  val INVALID: State = Value("INVALID")
  val REJECTED: State = Value("REJECTED")
  val STALLED: State = Value("STALLED")
  val SUBMITTED: State = Value("SUBMITTED")
  val UNKNOWN: State = Value("UNKNOWN")

  def toState(stateName: String): Option[State] = State.values.find(_.toString == stateName)
}
