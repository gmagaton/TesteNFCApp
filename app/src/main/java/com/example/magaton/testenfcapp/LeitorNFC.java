package com.example.magaton.testenfcapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.util.Log;

/**
 * Created by Gabriel.Magaton on 09/03/2018.
 */

public class LeitorNFC {

    private final NfcAdapter nfc;
    private final Activity activity;
    private PendingIntent pendingIntent;
    private IntentFilter filters[];
    private boolean writeMode;
    private static final int defaultTimeout = 3000;
    private static MifareClassic mifare;
    private byte[] keyA;

    public LeitorNFC(NfcAdapter nfc, Activity activity, PendingIntent pendingIntent, IntentFilter filters[], byte[] keyA) {
        this.nfc = nfc;
        this.activity = activity;
        this.pendingIntent = pendingIntent;
        this.filters = filters;
        this.keyA = keyA;
    }

    public boolean possui() {
        return nfc != null;
    }

    public boolean ligado() {
        return possui() && nfc.isEnabled();
    }

    public void on() {
        writeMode = true;
        nfc.enableForegroundDispatch(activity, pendingIntent, filters, null);
    }

    public void off() {
        writeMode = false;
        nfc.disableForegroundDispatch(activity);
    }

    public static boolean isNfcAction(Intent intent) {
        String action = intent.getAction();
        boolean isNfc = NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
        return isNfc;
    }

    public String lerSetores(Intent intent) throws LeitorNFCException {
        StringBuilder builder = new StringBuilder();
        MifareClassic m = conectarNFC(intent);
        int setores = contarSetores(intent);

        for (int setor = 0; setor < setores; setor++) {
            builder.append("Setor " + setor + "\n");
            autenticarSetor(intent, setor);
            int blocosSetor = m.getBlockCountInSector(setor);
            int bloco = m.sectorToBlock(setor);
            for (int i = 0; i < blocosSetor; i++) {
                String blocoConteudo = lerBloco(intent, bloco);
                builder.append("[" + bloco + "] " + blocoConteudo + "\n");
                bloco++;
            }
        }
        return builder.toString();
    }

    private void finalizar(Intent intent) throws LeitorNFCException {
        MifareClassic m = conectarNFC(intent);
        try {
            m.close();
        } catch (Throwable e) {
            Log.e(LeitorNFC.class.getName(), "Erro ao fechar NFC: " + e.getMessage());
        }
    }

    private int contarSetores(Intent intent) throws LeitorNFCException {
        boolean contou = false;
        int setores = 0;
        while (!contou) {
            Log.d(LeitorNFC.class.getName(), "Contando setores");
            try {
                MifareClassic m = conectarNFC(intent);
                setores = mifare.getSectorCount();
                contou = true;
            } catch (LeitorNFCException e) {
                throw e;
            } catch (Throwable e) {
                Log.e(LeitorNFC.class.getName(), "Erro ao contar setores: " + e.getMessage());
                contou = false;
            }
        }
        return setores;
    }

    private String lerBloco(Intent intent, int bloco) throws LeitorNFCException {
        MifareClassic m = conectarNFC(intent);
        boolean leu = false;
        String blocoConteudo = null;
        while (!leu) {
            Log.d(LeitorNFC.class.getName(), "Lendo bloco: " + bloco);
            try {
                byte[] dadoBlocoSetor = m.readBlock(bloco);
                blocoConteudo = StringUtils.bytesToHex(dadoBlocoSetor);
                leu = true;
            } catch (Throwable e) {
                leu = false;
                Log.e(LeitorNFC.class.getName(), "Erro ao ler bloco: " + bloco + " : " + e.getMessage());
            }
        }
        return blocoConteudo;
    }

    private void autenticarSetor(Intent intent, int setor) throws LeitorNFCException {
        MifareClassic m = conectarNFC(intent);
        boolean autenticou = false;
        while (!autenticou) {
            Log.d(LeitorNFC.class.getName(), "Autenticando setor: " + setor);
            try {
                autenticou = m.authenticateSectorWithKeyA(setor, keyA);
            } catch (Throwable e) {
                autenticou = false;
                Log.e(LeitorNFC.class.getName(), "Erro ao autenticar setor: " + setor + " : " + e.getMessage());
            }
        }
    }

    private MifareClassic conectarNFC(Intent intent) throws LeitorNFCException {
        if (mifare == null) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            mifare = MifareClassic.get(tag);
            if (mifare == null) {
                throw new LeitorNFCException("Dispositivo NÃO suportado para leitura", null);
            }
        }

        boolean conectado = mifare.isConnected();
        while (!conectado) {
            try {
                Log.i(LeitorNFC.class.getName(), "Conectando com o NFC: ");
                mifare.connect();
                conectado = mifare.isConnected();
            } catch (Throwable e) {
                conectado = false;
                Log.e(LeitorNFC.class.getName(), "Erro ao conectar NFC: " + e.getMessage());
            }
        }
        return mifare;

    }


}
