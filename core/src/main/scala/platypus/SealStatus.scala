package platypus

/**
  * @param sealed    Whether or not the vault is sealed.
  * @param total     The number of existing MasterKeys
  * @param quorum     The number of keys needed to unseal the vault
  * @param progress  The number of keys out of total that have been provided so far to unseal
  */
final case class SealStatus(`sealed`: Boolean,
                            total: Int,
                            quorum: Int,
                            progress: Int)
