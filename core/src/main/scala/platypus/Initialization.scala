package platypus

/**
  * Parameters needed to initialize a new vault
  * @param secretShares     the numbers of unseal keys
  * @param secretThreshold  the quorum of unseal keys needed to unseal
  */
final case class Initialization(secretShares: Int,
                                secretThreshold: Int)
