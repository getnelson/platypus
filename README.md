# Platypus

[![Build Status](https://travis-ci.org/getnelson/platypus.svg?branch=master)](https://travis-ci.org/getnelson/platypus)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.getnelson.platypus/core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.getnelson.platypus/core_2.11)
[![codecov](https://codecov.io/gh/getnelson/platypus/branch/master/graph/badge.svg)](https://codecov.io/gh/getnelson/platypus)

A native Scala client for interacting with [Vault](https://www.vaultproject.io). There is currently only one supported client, which uses [http4s](https://http4s.org) to make HTTP calls to Vault. Alternative implementations could be added with relative ease by providing an additional free interpreter for the `VaultOp` algebra.

## Getting started

Add the following to your `build.sbt`:

    libraryDependencies += "io.getnelson.platypus" %% "http4s" % "x.y.z"

Where `x.y.z` is the desired Platypus version. Check for the latest release [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.getnelson.platypus%22).

### Algebra

Platypus currently only supports a limited subset of the total actions available within the [Vault HTTP API](https://www.vaultproject.io/api/overview.html). Supported operations are iterated within the [VaultOp source](core/src/main/scala/platypus/VaultOp.scala). For example, to create a new policy one can construct the operation as such:

```scala
import platypus._

val fooReadOnly = Rule(
  path = "secret/foo",
  capabilities = "read" :: Nil,
  policy = None
)

val createMyPolicy: VaultOpF[Unit] = VaultOp
  .createPolicy(
    name = "my-policy",
    rules = fooReadOnly :: Nil
  )
```

This however is just a description of what operation the program might perform in the future, just creating these operations does not
actually execute the operations. In order to create the policy, we need to use the [http4s](http://http4s.org) interpreter.

### http4s Interpreter

First we create an interpreter, which requires a [Vault token](https://www.vaultproject.io/docs/concepts/tokens.html), an http4s client, and
a base url for Vault:

```scala
import cats.effect.IO
import org.http4s.Uri.uri
import org.http4s.client.blaze.Http1Client
import platypus._
import platypus.http4s._

val token = Token("1c1cb196-a03c-4336-bfac-d551849e11de")
val client = Http1Client[IO]().unsafeRunSync
val baseUrl = uri("http://127.0.0.1:8200")

val interpreter = new Http4sVaultClient(baseUrl, client)
```

Now we can apply commands to our http4s client to get back IOs
which actually interact with Vault:

```scala
import cats.effect.IO

val c: IO[Unit] = platypus.run(interpreter, createMyPolicy)

// actually execute the calls
c.unsafeRunSync
```

Typically, the *Platypus* algebra would be a part of a `Coproduct` with other algebras in a larger program, so running the `IO` immediately after `platypus.run` is not typical.

## Supported Vault Versions
- 0.10.x

## Contributing

Contributions are welcome; particularly to expand the algebra with additional operations that are supported by Vault but not yet supported by *Platypus*.
