package platypus

final case class Rule(path: String,
                      capabilities: List[String],
                      policy: Option[String])
