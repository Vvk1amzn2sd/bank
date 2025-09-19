package com.vvk.banque.domain.ValueObj;

/*-- Depend on abstractions, not on concretions. accid(domain) depends only on interface db seq here upholding DIP--*/
public interface DatabaseSequence {
    int nextAcc();   // limit: 10k -99999 only
}
