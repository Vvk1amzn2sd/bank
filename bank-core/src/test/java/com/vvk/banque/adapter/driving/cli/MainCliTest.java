package com.vvk.banque.adapter.driving.cli;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

class MainCliTest {

    @Test
    void printsHardCodedBalance() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        MainCli.main(new String[0]);

        assertThat(out.toString()).contains("Balance: 50 EUR");
    }
}
