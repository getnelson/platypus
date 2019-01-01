
import cats.{~>, Monad}
import cats.free.Free

package object platypus {

  type MasterKey = String

  type VaultOpF[A] = Free[VaultOp, A]

  def run[F[_]: Monad, A](interpreter: VaultOp ~> F, op: VaultOpF[A]): F[A] =
    op.foldMap(interpreter)
}
