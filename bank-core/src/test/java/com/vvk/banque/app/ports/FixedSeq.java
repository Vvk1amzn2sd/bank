package com.vvk.banque.app.ports;
import com.vvk.banque.domain.ValueObj.DatabaseSequence;
public class FixedSeq implements DatabaseSequence {
    private int v = 12344;
    public int nextAcc() { return ++v; }
}
