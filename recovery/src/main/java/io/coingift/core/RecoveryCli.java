package io.coingift.core;

import java.io.Console;
import java.nio.file.Path;

/** Portable offline recovery; passwords are accepted only through the non-echoing terminal prompt. */
public final class RecoveryCli {
    public static void main(String[] args) {
        char[] password=null;
        try {
            if(args.length!=2||(!args[0].equals("--inspect")&&!args[0].equals("--decrypt"))) {
                System.err.println("Usage: bittap-recover --inspect|--decrypt encrypted-backup.txt");System.exit(2);
            }
            Path path=Path.of(args[1]);
            String encrypted=BackupInput.read(path);
            WalletCrypto crypto=new WalletCrypto();
            if(args[0].equals("--inspect")) {
                if(encrypted.startsWith("6P")){System.out.println("BIP38: funding address type is not recorded; decrypt to inspect derived addresses.");return;}
                WalletCrypto.Metadata meta=crypto.inspect(encrypted);
                System.out.println("Network: "+meta.network);
                System.out.println("Format: CG"+meta.version);
                System.out.println("Public wallet note (unverified): "+meta.note);
                System.out.println("Public address claims (NOT authenticated until decrypted):");
                System.out.println("P2WPKH: "+meta.address);
                if(meta.addresses!=null){
                    System.out.println("P2PKH: "+meta.addresses.legacy);
                    System.out.println("P2SH-P2WPKH: "+meta.addresses.nestedSegwit);
                    System.out.println("P2TR: "+meta.addresses.taproot);
                }return;
            }
            Console console=System.console();
            if(console==null)throw new IllegalStateException("Run in an interactive terminal. Password arguments/files are not accepted.");
            password=console.readPassword("Decryption password: ");
            try(WalletCrypto.Unlocked unlocked=encrypted.startsWith("6P")?crypto.importBip38(encrypted,password):crypto.decrypt(encrypted,password)) {
                System.out.println("Network: "+unlocked.network);
                System.out.println("Authenticated wallet note: "+unlocked.note);
                System.out.println("P2WPKH address: "+unlocked.address);
                System.out.println("P2PKH address: "+unlocked.addresses.legacy);
                System.out.println("P2SH-P2WPKH address: "+unlocked.addresses.nestedSegwit);
                System.out.println("P2TR address (BIP341 key-only, no script tree): "+unlocked.addresses.taproot);
                System.out.println("Match the funded address. Generic WIF import does not select a script type or guarantee Taproot recovery.");
                System.out.println("Public key: "+unlocked.publicKey);
                String consent=console.readLine("Print sensitive WIF private key to this terminal? Type SHOW: ");
                if("SHOW".equals(consent))System.out.println("WIF: "+unlocked.wif());
            }
        } catch(Exception ex){System.err.println("Recovery failed. Check backup format, password, and data integrity.");System.exit(1);}
        finally{WalletCrypto.wipe(password);}
    }
}
