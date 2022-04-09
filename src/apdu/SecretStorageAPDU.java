package apdu;

import javacardss.SecretStorageApplet;
import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
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
    private static final String STR_APDU_CHANGE_NORMAL_PIN_HEADER = "B021";
    private static final String STR_APDU_CHANGE_DURESS_PIN_HEADER = "B022";
    
    private static final String STR_APDU_INSERT_KEY_VALUE_HEADER = "B030";
    private static final String STR_APDU_GET_VALUE_HEADER = "B040";
    private static final String STR_APDU_DELETE_KEY_HEADER = "B070";
    private static final String STR_APDU_GET_KEYS = "B0500000";
    private static final String STR_APDU_GET_KEY_LENS = "B0600000";
    
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
            System.out.println("-----------------------");
            System.out.println("What do you want to do?\n1)Change PIN\n2)Add secret\n3)Get secret\n4)Delete secret\n5)List keys\n0)Finish");
            in_action = sc.nextInt();
            
            switch(in_action) {
                case 1: 
                    changePin(cardMngr);
                    break;
                case 2: 
                    addSecret(cardMngr);
                    break;
                case 3: 
                    retrieveValue(cardMngr);
                    break;
                case 4: 
                    deleteSecret(cardMngr);
                    break;
                case 5: 
                    listKeys(cardMngr);
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
        
        int pin_format = getFormat("pin");
        ResponseAPDU response;
        do {
            System.out.print("Enter PIN please: ");
            String pin = sc.nextLine();
            pin = convertFormatToHex(pin, pin_format);
            String pin_apdu = buildPinAPDU(pin);
            response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(pin_apdu)));
            
            if((response.getSW() & 0xff00) == (0x63 << 8)) {
                int retries_remaining = response.getSW() & 0xff;
                System.err.println("Wrong PIN! (retries remaining: " + retries_remaining + ")\n");
            } else if(response.getSW() == 0x6dc0) {
                System.err.println("No more retries left! The card is blocked.\n");
                System.exit(1);
            }
        } while(response.getSW() != 0x9000);
        
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
            System.out.println("Which format is your " + s + " in?\n1)hex\n2)string");
            format = sc.nextInt();
        } while(format > 2 || format <= 0);
        
        return format;
    }
    
    private int getPinType() {
        Scanner sc = new Scanner(System.in);
        int pin;
        
        do {
            System.out.println("Which PIN do you want to change?\n1)Normal\n2)Duress");
            pin = sc.nextInt();
        } while(pin > 2 || pin < 1);
        
        return pin;
    }

    private String convertFormatToHex(String value, int format) {
        switch(format) {
            case 1: //hex
                return value;
            case 2: // string
                return Util.toHex(value.getBytes());
            default:
                System.err.println("Unsupported format!");
                System.exit(1);
        }
        
        return "";
    }

    private void listKeys(CardManager cardMngr) throws CardException {
        final ResponseAPDU response_keys = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_GET_KEYS)));
        
        final ResponseAPDU response_lens = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(STR_APDU_GET_KEY_LENS)));
        
        String[] keys = parseKeys(response_keys.getData(), response_lens.getData());
        
        if(keys.length == 0) {
            System.out.println("No keys available.");
        } else {
            System.out.println("Possible keys: ");
            for(String key : keys) {
                System.out.print(key +", ");
            }
            System.out.print("\n");
        }
    }

    private String[] parseKeys(byte[] keys, byte[] key_lens) {        
        byte[] trimmed_key_lens = removeZeros(key_lens);
        
        String[] parsed_keys = new String[trimmed_key_lens.length];
        int ofs = 0;
        for(int i = 0; i < trimmed_key_lens.length; i++) {
            byte key_len = trimmed_key_lens[i];
            
            String key = new String(Arrays.copyOfRange(keys, ofs, ofs + key_len));
            parsed_keys[i] = key;
            
            ofs += key_len;
        }
        
        return parsed_keys;
    }
    
    private byte[] removeZeros(byte[] key_lens) {
        int count = 0;
        for(byte k : key_lens) {
            if(k != 0) count++;
        }
        
        byte[] out = new byte[count];
        int i = 0;
        for(byte k : key_lens) {
            if(k != 0) out[i++] = k;
        }
        
        return out;
    }
    
    private void deleteSecret(CardManager cardMngr) throws CardException {
        Scanner sc = new Scanner(System.in);
        
        int format_key = getFormat("key");
        System.out.print("Key of the secret: ");
        String key_print = sc.nextLine();
        
        String key = convertFormatToHex(key_print, format_key);
        int key_len = key.length();
        
        String apdu = String.format("%s%02x00%02x%s", 
                STR_APDU_DELETE_KEY_HEADER,
                key_len/2,
                key_len/2,
                key);
        
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(apdu)));
        
        if(response.getSW() == 0x6dfa) {
            System.out.println("No such key exists!");
        } else {
            System.out.println("\'" + key_print + "\'" + " record deleted.");
        }
    }

    private void changePin(CardManager cardMngr) throws CardException {
        Scanner sc = new Scanner(System.in);
        
        int pin_type = getPinType();
        int format_pin = getFormat("pin");
        System.out.print("PIN: ");
        String pin = sc.nextLine();
        
        pin = convertFormatToHex(pin, format_pin);
        int pin_len = pin.length();
        
        String apdu = String.format("%s%02x00%02x%s", 
                pin_type == 1 ? STR_APDU_CHANGE_NORMAL_PIN_HEADER : STR_APDU_CHANGE_DURESS_PIN_HEADER,
                pin_len/2,
                pin_len/2,
                pin_len);
        
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(Util.hexStringToByteArray(apdu)));
        
        if(response.getSW() != 0x9000){
            System.out.println("Something went wrong!");
            System.exit(1);
        }
    }
}
