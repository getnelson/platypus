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
package http4s

import argonaut._
import Argonaut._
import cats.{FlatMap, ~>}
import cats.effect.IO
import cats.syntax.functor._
import journal._
import org.http4s.{argonaut => _, _}
import org.http4s.argonaut._
import org.http4s.client._

import scala.collection.immutable.SortedMap

final case class Initialized(init: Boolean)

final class Http4sVaultClient(authToken: Token,
                              baseUri: Uri,
                              client: Client[IO]) extends (VaultOp ~> IO) with Json {

  import VaultOp._
  import Method._
  import Status._

  val v1: Uri = baseUri / "v1"

  def apply[A](v: VaultOp[A]): IO[A] = v match {
    case IsInitialized          => isInitialized
    case Initialize(init)       => initialize(init)
    case GetSealStatus          => sealStatus
    case Seal                   => seal
    case Unseal(key)            => unseal(key)
    case cp @ CreatePolicy(_,_) => createPolicy(cp)
    case DeletePolicy(name)     => deletePolicy(name)
    case GetMounts              => getMounts
    case ct: CreateToken        => createToken(ct)
  }

  val log = Logger[this.type]

  val addCreds: Request[IO] => Request[IO] = _.putHeaders(Header("X-Vault-Token", authToken.value))

  def req[T: DecodeJson](req: Request[IO]): IO[T] =
    client.fetch(addCreds(req)){
      case Ok(resp) => resp.as(FlatMap[IO], jsonOf[IO, T])
      case resp =>
        resp.as[String].flatMap(body => {
          val msg = s"unexpected status: ${resp.status} from request: ${req.pathInfo}, msg: ${body}"
          IO.raiseError(new RuntimeException(msg))
        })
    }

  def reqVoid(req: Request[IO]): IO[Unit] =
    client.fetch(addCreds(req)) {
      case NoContent(_) => IO.pure(())
      case resp =>
        resp.as[String].flatMap(body => {
          val msg = s"unexpected status: ${resp.status} from request: ${req.pathInfo}, msg: ${body}"
          IO.raiseError(new RuntimeException(msg))
        })
    }

  def isInitialized: IO[Boolean] =
    req[Initialized](Request(GET, v1 / "sys" / "init")).map(_.init)

  def initialize(init: Initialization): IO[InitialCreds] =
    req[InitialCreds](Request(PUT, v1 / "sys" / "init").withEntity(init.asJson))

  def unseal(key: MasterKey): IO[SealStatus] =
    req[SealStatus](Request(PUT, v1 / "sys" / "unseal").withEntity(jsonUnseal(key)))

  def sealStatus: IO[SealStatus] =
    req[SealStatus](Request(GET, v1 / "sys" / "seal-status"))

  def seal: IO[Unit] =
    req[String](Request(GET, v1 / "sys" / "init")).void

  def createPolicy(cp: CreatePolicy): IO[Unit] =
    reqVoid(Request(POST, v1 / "sys" / "policy" / cp.name).withEntity(cp.asJson))

  def deletePolicy(name: String): IO[Unit] =
    reqVoid(Request(DELETE, v1 / "sys" / "policy" / name))

  def getMounts: IO[SortedMap[String, Mount]] =
    req[SortedMap[String, Mount]](Request(GET, uri = v1 / "sys" / "mounts"))

  def createToken(ct: CreateToken): IO[Token] =
    req[argonaut.Json](Request(POST, v1 / "auth" / "token" / "create").withEntity(ct.asJson)).flatMap { json =>
      val clientToken = for {
        cursor <- Some(json.cursor): Option[Cursor]
        auth   <- cursor.downField("auth")
        token  <- auth.downField("client_token")
        str    <- token.focus.string
      } yield str

      clientToken  match {
        case Some(token) => IO.pure(Token(token))
        case None => IO.raiseError(new RuntimeException("No auth/client_token in create token response"))
      }
    }
}
