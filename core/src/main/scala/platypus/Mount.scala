package platypus

final case class Mount(path: String,
                       `type`: String,
                       description: String,
                       defaultLeaseTTL: Int,
                       maxLeaseTTL: Int)
