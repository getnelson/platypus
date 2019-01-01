package platypus

/**
  * This contains the result of the `initialize` call, it *must* have
  * the number of MasterKeys requested. They should distributed to N
  * trusted individuals, a quorum of which will have to present keys to
  * unseal a vault.
  *
  * the rootToken is a omnipotent bearer token for this vault, so treat
  * it with the utmost of care. It will be required to create other
  * less privileged tokens.
  */
final case class InitialCreds(keys: List[MasterKey],
                              rootToken: RootToken)
