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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;

public class MainActivity extends Activity {

    private TextView stats;
    private LeitorNFC leitorNFC;
    private Context context;

    class BotaoLimpar implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            stats.setText("");
        }
    }

    class BotaoValidar implements View.OnClickListener {
        private final LeitorNFC leitorNFC;
        private final Context context;

        BotaoValidar(Context context, LeitorNFC leitorNFC) {
            this.leitorNFC = leitorNFC;
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            verificarNFC();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        Button btLimpar = findViewById(R.id.btLimpar);
        btLimpar.setOnClickListener(new BotaoLimpar());

        stats = findViewById(R.id.txtStatus);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter[] filters = new IntentFilter[]{tagDetected};

        leitorNFC = new LeitorNFC(NfcAdapter.getDefaultAdapter(this), this, pendingIntent, filters, MifareClassic.KEY_DEFAULT);

        Button btValidar = findViewById(R.id.btValidar);
        btValidar.setOnClickListener(new BotaoValidar(context, leitorNFC));

        verificarNFC();


    }

    private void verificarNFC() {
        if (leitorNFC.possui()) {
            if (leitorNFC.ligado()) {
                Toast.makeText(this, "NFC está LIGADO", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "NFC NÃO está LIGADO", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Não possui NFC", Toast.LENGTH_LONG).show();
        }
    }

    private void verificarAcaoNFC(Intent intent) {
        if (!LeitorNFC.isNfcAction(intent)) {
            return;
        }

        ByteArrayBuffer b = new ByteArrayBuffer(64);


        try {
            Date ini = new Date();
            String setores = leitorNFC.lerSetores(intent);
            Date fim = new Date();
            long diff = fim.getTime() - ini.getTime();
            stats.append("Dados \n" + setores + "\n");
            stats.append("Leitura efetuada em " + diff + " ms\n");
        } catch (Throwable e) {
            Log.i(this.getLocalClassName(), "Error: " + e.getMessage());
            stats.append("Erro ao conectar com a tag NFC: " + e.getMessage() + "\n\n");
        }

    }

    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
        verificarAcaoNFC(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        leitorNFC.off();
    }

    @Override
    public void onResume() {
        super.onResume();
        leitorNFC.on();
    }


}
