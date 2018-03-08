package com.example.magaton.testenfcapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;

public class MainActivity extends Activity {
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    TextView stats;

    class BotaoLimpar implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            stats.setText("");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btLimpar = findViewById(R.id.btLimpar);
        btLimpar.setOnClickListener(new BotaoLimpar());

        stats = findViewById(R.id.txtStatus);
        stats.append("\nInicializando\n");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "HABILITE O NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};
    }

    private void displayByteArray(byte[] bytes) {
        String res = "";
        StringBuilder builder = new StringBuilder().append("[");
        for (int i = 0; i < bytes.length; i++) {
            res += (char) bytes[i];
        }
        stats.append("RAW: " + res + "\n");
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        boolean isNfc = NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
        if (!isNfc) {
            return;
        }

        stats.append("Lendo NFC\n");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareClassic mifare = MifareClassic.get(tag);
        stats.append("Conectando a TAG NFC\n");
        try {
            Date ini = new Date();
            mifare.setTimeout(30000);
            mifare.connect();
            stats.append("Conectou com a TAG NFC\n");
            for (int i = 0; i < mifare.getSectorCount(); i++) {
                stats.append("Autenticando Setor " + i);
                boolean autenticou = mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT);
                int cont = 1;
                while (autenticou == false) {
                    cont++;
                    autenticou = mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT);
                    if (cont >= 200) {
                        break;
                    }
                }
                if (autenticou) {
                    stats.append(" " + cont + " autenticou\n");
                } else {
                    stats.append(" " + cont + " não autenticou\n");
                }
            }
            Date fim = new Date();

            long diff = fim.getTime() - ini.getTime();

            stats.append(mifare.getBlockCount() + " blocos encontrados em " + diff + " ms\n");
            stats.append("Desconectou com a TAG NFC\n\n");
            mifare.close();
        } catch (Throwable e) {
            stats.append("Erro ao conectar com a tag NFC: " + e.getMessage() + "\n\n");
        }

    }

    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}
