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
package nl.knaw.dans.easy

import java.nio.file.{ Files, Path }

import resource._

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

package object report {

  type DepositId = String
  type Deposits = Map[DepositId, Deposit]
  type DepositorId = String

  implicit class TryExtensions2[T](val t: Try[T]) extends AnyVal {
    // TODO candidate for dans-scala-lib
    def unsafeGetOrThrow: T = {
      t match {
        case Success(value) => value
        case Failure(throwable) => throw throwable
      }
    }
  }

  implicit class PathExtensions(val path: Path) extends AnyVal {
    def list[T](f: Stream[Path] => T): T = {
      managed(Files.list(path)).acquireAndGet(stream => f(stream.iterator().asScala.toStream))
    }
  }
}
