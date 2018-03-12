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

import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.util.Arrays;

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
        //final ByteArrayBuffer bytes = new ByteArrayBuffer(64);
        MifareClassic m = conectarNFC(intent);
        int setores = contarSetores(intent);
        int offset = 0;
        int tam = 16;

        for (int setor = 0; setor < setores; setor++) {
            builder.append("Setor " + setor + "\n");
            autenticarSetor(intent, setor);
            int blocosSetor = m.getBlockCountInSector(setor);
            int bloco = m.sectorToBlock(setor);
            for (int i = 0; i < blocosSetor; i++) {
                byte[] blocoConteudo = lerBloco(intent, setor, bloco);
                builder.append("[" + bloco + "] " + StringUtils.bytesToHex(blocoConteudo) + "\n");
                int size = tam < blocoConteudo.length ? blocoConteudo.length : tam;
                //bytes.append(blocoConteudo, offset, size);
                bloco++;
                offset += tam;
            }
            disconnect();
        }
        //builder.append(" Bytes: "+StringUtils.bytesToHex(bytes.toByteArray()));
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

    private byte[] lerBloco(Intent intent, int setor, int bloco) throws LeitorNFCException {
        MifareClassic m = conectarNFC(intent);
        boolean leu = false;
        byte[] dadoBlocoSetor = null;
        while (!leu) {
            Log.d(LeitorNFC.class.getName(), "Lendo bloco: " + bloco);
            try {
                dadoBlocoSetor = m.readBlock(bloco);
                int length = dadoBlocoSetor.length;
                while (length != MifareClassic.BLOCK_SIZE) {
                    throw new Exception("Tamanho lido do bloco inválido: " + length + " data: " + StringUtils.bytesToHex(dadoBlocoSetor));
                }
                Log.d(LeitorNFC.class.getName(), "Leu bloco: " + bloco + " tamanho: " + dadoBlocoSetor.length);
                leu = true;
            } catch (Throwable e) {
                leu = false;
                Log.e(LeitorNFC.class.getName(), "Erro ao ler bloco: " + bloco + " : " + e.getMessage());
                //disconnect();
                autenticarSetor(intent, setor);
            }
        }
        return dadoBlocoSetor;
    }

    private void autenticarSetor(Intent intent, int setor) throws LeitorNFCException {
        MifareClassic m = conectarNFC(intent);
        boolean autenticou = false;
        int contador = 0;
        Log.d(LeitorNFC.class.getName(), "Autenticando setor: " + setor);
        while (!autenticou) {
            try {
                contador++;
                autenticou = m.authenticateSectorWithKeyA(setor, keyA);
            } catch (Throwable e) {
                autenticou = false;
                Log.e(LeitorNFC.class.getName(), "Erro ao autenticar setor: " + setor + " : " + e.getMessage());
            }
        }
        Log.d(LeitorNFC.class.getName(), "Autenticou setor: " + setor + " em:" + contador + " tentativas");

    }

    private void disconnect() {
        try {
            Log.i(LeitorNFC.class.getName(), "Desconectando com o NFC: ");
            mifare.close();
        } catch (IOException e) {
            Log.e(LeitorNFC.class.getName(), "Erro ao desconectar NFC: " + e.getMessage());
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
                mifare.setTimeout(10000);
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
