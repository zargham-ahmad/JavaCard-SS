package apdu;

import javacardss.SecretStorageApplet;
import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Test class.
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author Petr Svenda (petrs), Dusan Klinec (ph4r05)
 */
public class SecretStorageAPDU {
    private static String APPLET_AID = "73696d706c656170706c6574";
    private static String APPLET_AID2 = "0102030405060708090102";
    private static byte APPLET_AID_BYTE[] = Util.hexStringToByteArray(APPLET_AID);

    private static final String STR_APDU_INCORRECT_PIN = "B02000000431323333";
    
    private static final String STR_APDU_CORRECT_PIN = "B02000000431323334"; // default pin
    private static final String STR_APDU_KEY_VALUE = "B03004040AF163697479F262726e6f"; // city-brno
    private static final String STR_APDU_KEY_VALUE2 = "B03005050CF16369747979F262726e6f73"; // cityy-brnos
    private static final String STR_APDU_GET_VALUE = "B0400500056369747979"; // this retrieves value of key cityy
    
    private static final String STR_APDU_UNSUPPORTED_INS = "B0E1000000"; // This instruction is not supported by the card

    /**
     * Main entry point.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            SecretStorageAPDU main = new SecretStorageAPDU();
            
            main.demo();
            //main.demoEncryptDecrypt();
            //main.demoUseRealCard();
            //main.demoUseRealCard2();
            
        } catch (Exception ex) {
            System.out.println("Exception : " + ex);
        }
    }

    public void demo() throws Exception {
        // CardManager abstracts from real or simulated card, provide with applet AID
        final CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);          
        
        // Get default configuration for subsequent connection to card (personalized later)
        final RunConfig runCfg = RunConfig.getDefaultConfig();

        // A) If running on physical card
        //runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card

        // B) If running in the simulator 
        runCfg.setAppletToSimulate(SecretStorageApplet.class); // main class of applet to simulate
        runCfg.setTestCardType(RunConfig.CARD_TYPE.JCARDSIMLOCAL); // Use local simulator

        // Connect to first available card
        // NOTE: selects target applet based on AID specified in CardManager constructor
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
        }
        System.out.println(" Done.");

        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_INCORRECT_PIN)));
        
        if(response.getSW() != 0x6982) {
            System.out.println("Unexpected error code of incorrect PIN!");
        }
        
        final ResponseAPDU responsee = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_CORRECT_PIN)));
        final ResponseAPDU response2 = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_KEY_VALUE)));
        final ResponseAPDU response3 = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_KEY_VALUE2)));
        final ResponseAPDU response4 = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_GET_VALUE)));
        byte[] data = response4.getData();
        
        //final ResponseAPDU response2 = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_UNSUPPORTED_INS))); // Use other constructor for CommandAPDU
        
        System.out.println(Util.bytesToHex(data));
    }

    public void demoEncryptDecrypt() throws Exception {
        final CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        //runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card
        runCfg.setAppletToSimulate(SecretStorageApplet.class); 
        runCfg.setTestCardType(RunConfig.CARD_TYPE.JCARDSIMLOCAL); // Use local simulator

        // Connect to first available card
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
        }
        System.out.println(" Done.");

        
        // Task 1
        // TODO: Prepare and send APDU with 32 bytes of data for encryption, observe output

        // Task 2
        // TODO: Extract the encrypted data from the card's response. Send APDU with this data for decryption
        // TODO: Compare match between data for encryption and decrypted data
        
        // Task 3
        // TODO: What is the value of AES key used inside applet? Use debugger to figure this out

        // Task 4
        // TODO: Prepare and send APDU for setting different AES key, then encrypt and verify (with http://extranet.cryptomathic.com/aescalc/index
    }    
    
    public void demoUseRealCard() throws Exception {
        final CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card

        // Connect to first available card
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
        }
        System.out.println(" Done.");

        
        // Task 5 
        // TODO: Obtain random data from real card

        // Task 6 
        // TODO: Set new key value and encrypt on card
        
        cardMngr.Disconnect(true);
    }    
    public void demoUseRealCard2() throws Exception {
        final CardManager cardMngr = new CardManager(true, Util.hexStringToByteArray(APPLET_AID2));
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card

        // Connect to first available card
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
        }
        System.out.println(" Done.");

        // Transmit single APDU
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_UNSUPPORTED_INS)));
        byte[] data = response.getData();

        // Task 5 
        // TODO: Obtain random data from real card
        // Task 6 
        // TODO: Set new key value and encrypt on card
        cardMngr.Disconnect(true);
    }
}
