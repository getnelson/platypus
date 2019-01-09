//: ----------------------------------------------------------------------------
//: Copyright (C) 2017 Verizon.  All Rights Reserved.
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
package http4s

import argonaut.Argonaut._
import cats.effect.IO
import com.whisk.docker.scalatest._
import org.http4s.Uri
import org.http4s.client.blaze._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect._
import org.http4s._
import org.http4s.client.Client

class Http4sVaultSpec extends FlatSpec
  with DockerTestKit
  with Matchers
  with DockerVaultService
  with http4s.Json {
  override val PullImagesTimeout = 20.minutes
  override val StartContainersTimeout = 1.minute
  override val StopContainersTimeout = 1.minute

  def vaultHost: Option[Uri] =
    for {
      host <- Option(dockerExecutor.host)
      yolo <- Uri.fromString(s"http://$host:8200").toOption
    } yield yolo

  var masterKey: MasterKey = _
  var rootToken: RootToken = _

  type Interpreter = Client[IO] => Http4sVaultClient

  var interp: Interpreter = _

  val baseUrl: Uri = vaultHost getOrElse Uri.uri("http://0.0.0.0:8200")

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val resource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource

  val token = Token("asdf")
  interp = client => new Http4sVaultClient(token, baseUrl, client)

  def withClient[A](op : VaultOpF[A])(interpreter:Interpreter):IO[A] = resource.use( client => op.foldMap(interpreter(client)))

  if (sys.env.get("BUILDKITE").isEmpty) {
    behavior of "vault"

    it should "not be initialized" in {
      withClient(VaultOp.isInitialized)(interp).unsafeRunSync() should be(false)
    }

    it should "initialize" in {
      val result = withClient(VaultOp.initialize(1, 1))(interp).unsafeRunSync()
      result.keys.size should be(1)
      this.masterKey = result.keys(0)
      this.rootToken = result.rootToken
      this.interp = client => new Http4sVaultClient(Token(rootToken.value), baseUrl, client)
    }

    it should "be initialized now" in {
      withClient(VaultOp.isInitialized)(interp).unsafeRunSync() should be(true)
    }

    it should "be sealed at startup" in {
      val sealStatus = withClient(VaultOp.sealStatus)(interp).unsafeRunSync()
      sealStatus.`sealed` should be(true)
      sealStatus.total should be(1)
      sealStatus.progress should be(0)
      sealStatus.quorum should be(1)
    }

    it should "be unsealable" in {
      val sealStatus = withClient(VaultOp.unseal(this.masterKey))(interp).unsafeRunSync()
      sealStatus.`sealed` should be(false)
      sealStatus.total should be(1)
      sealStatus.progress should be(0)
      sealStatus.quorum should be(1)
    }

    it should "be unsealed after unseal" in {
      val sealStatus = withClient(VaultOp.sealStatus)(interp).unsafeRunSync()
      sealStatus.`sealed` should be(false)
      sealStatus.total should be(1)
      sealStatus.progress should be(0)
      sealStatus.quorum should be(1)
    }

    it should "have cubbyhole, secret, sys mounted" in {
      val mounts = withClient(VaultOp.getMounts)(interp).attempt.unsafeRunSync()
      mounts.toOption.get.size should be(4)
      mounts.toOption.get.contains("cubbyhole/") should be(true)
      mounts.toOption.get.contains("secret/") should be(true)
      mounts.toOption.get.contains("identity/") should be(true)
      mounts.toOption.get.contains("sys/") should be(true)
    }

    // This is how platypus writes policies.  It provides a good test case for us.
    val StaticRules = List(
      Rule("sys/*", policy = Some("deny"), capabilities = Nil),
      Rule("auth/token/revoke-self", policy = Some("write"), capabilities = Nil)
    )
    val cp: VaultOp.CreatePolicy =
      VaultOp.CreatePolicy(
        name = s"qa__howdy",
        rules = StaticRules :::
          List("example/qa/mysql", "example/qa/cassandra").map { resource =>
            Rule(
              path = s"${resource}/creds/howdy",
              capabilities = List("read"),
              policy = None
            )
          }
      )

    it should "write policies" in {
      withClient(VaultOp.createPolicy(cp.name, cp.rules))(interp).unsafeRunSync() should be(())
    }

    it should "delete policies" in {
      withClient(VaultOp.deletePolicy(cp.name))(interp).unsafeRunSync() should be(())
    }

    it should "encode policies correctly" in {
      cp.asJson.field("policy") should be(Some(jString("""{"path":{"sys/*":{"policy":"deny"},"auth/token/revoke-self":{"policy":"write"},"example/qa/mysql/creds/howdy":{"capabilities":["read"]},"example/qa/cassandra/creds/howdy":{"capabilities":["read"]}}}""")))
    }

    it should "create tokens" in {
      val token2 = withClient(VaultOp.createToken(
        policies = Some(List("default")),
        ttl = Some(1.minute)
      ))(interp).unsafeRunSync()
      val interp2 : Interpreter = client => new Http4sVaultClient(token2, baseUrl, client)
      withClient(VaultOp.isInitialized)(interp2).unsafeRunSync() should be(true)
    }
  }
}
