package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.bitcoinj.base.Base58;

/** Independent Python-created public fixtures. Never fund these addresses. */
public class Bip38ImporterTest {
    @Test public void independentlyGeneratedBoundariesAndEcMultiplyVectors()throws Exception{
        try(BufferedReader input=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/bip38-public-vectors.tsv"),StandardCharsets.UTF_8))){
            for(String line;(line=input.readLine())!=null;){
                if(line.startsWith("#")||line.isEmpty())continue;String[] vector=line.split("\t");
                if(vector[5].equals("reject")){assertThrows(Exception.class,()->new WalletCrypto().importBip38(vector[1],vector[2].toCharArray()));continue;}
                try(WalletCrypto.Unlocked key=new WalletCrypto().importBip38(vector[1],vector[2].toCharArray())){assertEquals(vector[0],vector[4],key.wif());assertNotNull(key.addresses.taproot);}
                assertThrows(Exception.class,()->new WalletCrypto().importBip38(vector[1],(vector[2]+" wrong").toCharArray()));
            }
        }
    }
    @Test public void secondOfficialCompressedVector()throws Exception{
        try(WalletCrypto.Unlocked key=new WalletCrypto().importBip38("6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7","Satoshi".toCharArray())){
            assertEquals("KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7",key.wif());
        }
    }
    @Test public void checksumAndReservedFlagChangesCannotReleaseAKey()throws Exception{
        String encrypted="6PYW57v4Fk5tnPAPQcXt578N4kxYVrz9FurvD63dSpUo3usVfchv4YVgCx";
        String wrongChecksum=encrypted.substring(0,57)+(encrypted.endsWith("1")?"2":"1");
        assertThrows(Exception.class,()->new WalletCrypto().importBip38(wrongChecksum,"public boundary fixture".toCharArray()));
        byte[] payload=Base58.decodeChecked(encrypted);payload[2]|=1;
        String reserved=Base58.encodeChecked(payload[0]&255,java.util.Arrays.copyOfRange(payload,1,payload.length));
        assertThrows(IllegalArgumentException.class,()->new WalletCrypto().importBip38(reserved,"public boundary fixture".toCharArray()));
    }
}
