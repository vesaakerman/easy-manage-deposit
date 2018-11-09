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

import org.scalatest.{ FlatSpec, Matchers, OptionValues }

class DepositSpec extends FlatSpec with Matchers with OptionValues {

  "registeredString" should "return yes when its value is true" in {
    val doi = Doi(Option("doi_id"), Option(true))
    doi.registeredString.value shouldBe "yes"
  }

  it should "return no when the boolean is false" in {
    val doi = Doi(Option("doi_id"), Option(false))
    doi.registeredString.value shouldBe "no"
  }

  it should "return a None when the boolean is null" in {
    val doi = Doi()
    doi.registeredString shouldBe None
  }
}
