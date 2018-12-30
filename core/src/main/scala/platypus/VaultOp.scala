//: ----------------------------------------------------------------------------
//: Copyright (C) 2014 Verizon.  All Rights Reserved.
//:
//:   Licensed under the Apache License, Version 2.0 (the "License");
//:   you may not use this file except in compliance with the License.
//:   You may obtain a copy of the License at
//:
//:       http://www.apache.org/licenses/LICENSE-2.0
//:
//:   Unless required by applicable law or agreed to in writing, software
//:   distributed under the License is distributed on an "AS IS" BASIS,
//:   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//:   See the License for the specific language governing permissions and
//:   limitations under the License.
//:
//: ----------------------------------------------------------------------------
package platypus

import cats.free.Free._
import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.SortedMap

/**
  * An algebra which represents an operation interacting with a
  * [HashiCorp Vault](https://www.vaultproject.io/docs/) server
  */
sealed abstract class VaultOp[A] extends Product with Serializable

object VaultOp {

  case object IsInitialized extends VaultOp[Boolean]

  final case class Initialize(
    init: Initialization
  ) extends VaultOp[InitialCreds]

  final case class Unseal(
    key: MasterKey
  ) extends VaultOp[SealStatus]

  case object GetSealStatus extends VaultOp[SealStatus]

  case object Seal extends VaultOp[Unit]

  case object GetMounts extends VaultOp[SortedMap[String, Mount]]

  final case class CreatePolicy(
    name: String,
    rules: List[Rule]
  ) extends VaultOp[Unit]

  final case class DeletePolicy(
    name: String
  ) extends VaultOp[Unit]

  final case class CreateToken(
    policies: Option[List[String]],
    renewable: Boolean,
    ttl: Option[FiniteDuration],
    numUses: Long = 0L
  ) extends VaultOp[Token]

  def isInitialized: VaultOpF[Boolean] =
    liftF(IsInitialized)

  def initialize(masters: Int, quorum: Int): VaultOpF[InitialCreds] =
    liftF(Initialize(Initialization(masters, quorum)))

  def unseal(key: MasterKey): VaultOpF[SealStatus] =
    liftF(Unseal(key))

  def seal: VaultOpF[Unit] =
    liftF(Seal)

  def sealStatus: VaultOpF[SealStatus] =
    liftF(GetSealStatus)

  def createPolicy(name: String, rules: List[Rule]): VaultOpF[Unit] =
    liftF(CreatePolicy(name, rules))

  def deletePolicy(name: String): VaultOpF[Unit] =
    liftF(DeletePolicy(name))

  def getMounts: VaultOpF[SortedMap[String, Mount]] =
    liftF(GetMounts)

  def createToken(
    policies: Option[List[String]] = None,
    renewable: Boolean = true,
    ttl: Option[FiniteDuration] = None,
    numUses: Long = 0L
  ): VaultOpF[Token] = liftF(CreateToken(policies, renewable, ttl, numUses))
}
