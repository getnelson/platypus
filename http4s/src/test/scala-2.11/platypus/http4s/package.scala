package platypus

package object http4s {

  private[http4s] implicit class EitherOps[A, B](val e: Either[A, B]) extends AnyVal {
    def toOption: Option[B] = e match {
      case Right(b) => Some(b)
      case _        => None
    }
  }
}
