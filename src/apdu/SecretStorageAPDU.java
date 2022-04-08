package apdu;

import javacardss.SecretStorageApplet;
import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import javax.smartcardio.CardException;

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
    
    private static final String STR_APDU_PIN_HEADER = "B0200000";
    private static final String STR_APDU_INSERT_KEY_VALUE_HEADER = "B030";
    private static final String STR_APDU_GET_VALUE_HEADER = "B040";
    
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

        int in_action = -1;
        Scanner sc = new Scanner(System.in);
        
        checkPIN(cardMngr);
        
        while(in_action != 0) {
            System.out.println("What do you want to do?\n1)Change PIN\n2)Add secret\n3)Get secret\n4)List keys\n0)Finish");
            in_action = sc.nextInt();
            
            switch(in_action) {
                case 1: 
                    break;
                case 2: 
                    addSecret(cardMngr);
                    break;
                case 3: 
                    retrieveValue(cardMngr);
                    break;
                case 4: 
                    break;
                default:
                    System.out.println("Unsupported operation!");
                    break;
            }
        }
        
    }

    private void addSecret(CardManager cardMngr) throws CardException {
        Scanner sc = new Scanner(System.in);
        
        int format_key = getFormat("key");
        System.out.print("Key: ");
        String key = sc.nextLine();
        
        int format_value = getFormat("value");
        System.out.print("Value: ");
        String value = sc.next();
        
        key = convertFormatToHex(key, format_key);
        value = convertFormatToHex(value, format_value);
                
        int key_len = key.length();
        int value_len = value.length();
        
        String apdu = String.format("%s%02x%02x%02xF1%sF2%s", 
                STR_APDU_INSERT_KEY_VALUE_HEADER,
                key_len/2,
                value_len/2,
                (key_len + value_len)/2 + 2,
                key,
                value);
        
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(apdu)));
        
        if(response.getSW() != 0x9000) {
            System.err.println("Incorrect key-value!\n" + response.getSW());
            System.exit(1);
        }
        
    }

    private void retrieveValue(CardManager cardMngr) throws CardException, UnsupportedEncodingException {
        Scanner sc = new Scanner(System.in);
        
        int format_key = getFormat("key");
        System.out.print("Key: ");
        String key = sc.nextLine();
        
        key = convertFormatToHex(key, format_key);
        int key_len = key.length();
        
        String apdu = String.format("%s%02x00%02x%s", 
                STR_APDU_GET_VALUE_HEADER,
                key_len/2,
                key_len/2,
                key);
        
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(apdu)));
        
        if(response.getSW() != 0x9000) {
            System.err.println("Incorrect key-value!\n" + response.getSW());
            System.exit(1);
        }
        
        String value = new String(response.getData(), "UTF-8");
        System.out.println("Value:" + value + "\n");
    }

    private void checkPIN(CardManager cardMngr) throws CardException {  
        Scanner sc = new Scanner(System.in);      
        
        System.out.print("Enter PIN please (in hex): ");
        String pin = sc.nextLine();
        String pin_apdu = buildPinAPDU(pin);
        System.out.println(pin_apdu);
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(pin_apdu)));
        
        if(response.getSW() != 0x9000) {
            System.err.println("Wrong PIN!\n");
            System.exit(1);
        }
    }    

    private String buildPinAPDU(String pin) {
        String apdu = new String();
        int pin_len = pin.length()/2; // dividing by 2 because it is in hex; will change later
        
        return String.format("%s%02x%s", STR_APDU_PIN_HEADER, pin_len, pin);
    }
    
    private int getFormat(String s) {
        Scanner sc = new Scanner(System.in);
        int format;        
        
        do {
            System.out.println("Which format is your " + s + " in?\n1)hex\n2)string\n3)bin");
            format = sc.nextInt();
        } while(format > 3 || format < 0);
        
        return format;
    }

    private String convertFormatToHex(String value, int format) {
        switch(format) {
            case 1: //hex
                return value;
            case 2: // string
                return Util.toHex(value.getBytes());
            case 3: // bin
                System.err.println("Unsupported for now!");
                System.exit(1);
            default:
                System.err.println("Unsupported format!");
                System.exit(1);
        }
        
        return "";
    }
}
