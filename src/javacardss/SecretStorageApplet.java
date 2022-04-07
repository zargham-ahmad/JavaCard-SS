package javacardss;

import javacard.framework.*;
import javacard.security.CryptoException;

public class SecretStorageApplet extends Applet {

    private OwnerPIN m_duress_pin = null;
    private OwnerPIN m_user_pin = null;
    private static final byte[] OK_PIN_MSG = {'P', 'i', 'n', ' ', 'O', 'K', '!'};

    final static byte APPLET_CLA = (byte) 0xB0;
    
    //exceptions 
    final static short SW_Exception = (short) 0xff01;
    final static short SW_ArrayIndexOutOfBoundsException = (short) 0xff02;
    final static short SW_ArithmeticException = (short) 0xff03;
    final static short SW_ArrayStoreException = (short) 0xff04;
    final static short SW_NullPointerException = (short) 0xff05;
    final static short SW_NegativeArraySizeException = (short) 0xff06;
    final static short SW_CryptoException_prefix = (short) 0xf100;
    final static short SW_SystemException_prefix = (short) 0xf200;
    final static short SW_PINException_prefix = (short) 0xf300;
    final static short SW_TransactionException_prefix = (short) 0xf400;
    final static short SW_CardRuntimeException_prefix = (short) 0xf500;
    
    //error return codes
    private final static short SW_WRONG_PIN = 0x63c0;
    public final static short SW_DUPLICATE_KEY = 0x6a8a;
    public final static short SW_NO_EXISTING_VALUE = 0x6dfa;
    
    //instruction codes
    private final static byte INS_VERIFY = 0x20;
    public final static byte INS_ADD_KEYVALUE_ENTRY = 0x30;
    public final static byte INS_GET_VALUE = 0x40;

    public final static byte PIN_TRY_LIMIT = 0x03;
    public final static byte PIN_SIZE = 0x04;
    private static byte[] default_pin = {0x31, 0x32, 0x33, 0x34};

    // Tag byte for key
    public final static byte TAG_KEY = (byte) 0xF1;
    // Tag byte for secret records
    public final static byte TAG_SECRETVALUE = (byte) 0xF2;

    private KeyValueRecord current;

    private SecretStorageApplet() {
        m_user_pin = new OwnerPIN((byte) 3, (byte) 4);
        m_user_pin.update(default_pin, (short) 0x00, PIN_SIZE);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new SecretStorageApplet().register();
    }

    public void process(APDU apdu) {
        // Nothing particular to do on SELECT
        if (selectingApplet()) {
            return;
        }
        
        byte[] buffer = apdu.getBuffer();
                
        if (!m_user_pin.isValidated() && buffer[ISO7816.OFFSET_INS] != INS_VERIFY)
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);

        try {
            if (buffer[ISO7816.OFFSET_CLA] == APPLET_CLA) {
                switch (buffer[ISO7816.OFFSET_INS]) {
                    case INS_VERIFY:
                        verifyPin(apdu);
                        break;
                    case INS_ADD_KEYVALUE_ENTRY:
                        addSecretValue(apdu);
                        break;
                    case INS_GET_VALUE:
                        getValue(apdu);
                        break;

                    default:
                        ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                        break;
                }
            } else {
                ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            }
        } catch (ISOException e) {
            throw e; // Our exception from code, just re-emit
        } catch (ArrayIndexOutOfBoundsException e) {
            ISOException.throwIt(SW_ArrayIndexOutOfBoundsException);
        } catch (ArithmeticException e) {
            ISOException.throwIt(SW_ArithmeticException);
        } catch (ArrayStoreException e) {
            ISOException.throwIt(SW_ArrayStoreException);
        } catch (NullPointerException e) {
            ISOException.throwIt(SW_NullPointerException);
        } catch (NegativeArraySizeException e) {
            ISOException.throwIt(SW_NegativeArraySizeException);
        } catch (CryptoException e) {
            ISOException.throwIt((short) (SW_CryptoException_prefix | e.getReason()));
        } catch (SystemException e) {
            ISOException.throwIt((short) (SW_SystemException_prefix | e.getReason()));
        } catch (PINException e) {
            ISOException.throwIt((short) (SW_PINException_prefix | e.getReason()));
        } catch (TransactionException e) {
            ISOException.throwIt((short) (SW_TransactionException_prefix | e.getReason()));
        } catch (CardRuntimeException e) {
            ISOException.throwIt((short) (SW_CardRuntimeException_prefix | e.getReason()));
        } catch (Exception e) {
            ISOException.throwIt(SW_Exception);
        }
    }

    private void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        if (!m_user_pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_SIZE))
            ISOException.throwIt(SW_WRONG_PIN);
            
        // this isn't necessary
        Util.arrayCopyNonAtomic(OK_PIN_MSG, (short) 0, buffer, ISO7816.OFFSET_CDATA, (short)OK_PIN_MSG.length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short)OK_PIN_MSG.length);
    }
    
    void checkTLV(byte[] buffer, short inOfs,
                   byte tag, short len, short maxLen) {
        if (buffer[inOfs] != tag)
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);

        if (len > maxLen)
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
    }
    
    
    void getValue(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte key_size = buffer[ISO7816.OFFSET_P1];
        KeyValueRecord record = KeyValueRecord.search(buffer, ISO7816.OFFSET_CDATA, key_size);
        
        if(record == null)
            ISOException.throwIt(SW_NO_EXISTING_VALUE);
        
        byte value_len = record.getSecretValue(buffer, (byte) ISO7816.OFFSET_CDATA);
        
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, value_len);
    }

    void addSecretValue(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte key_size = buffer[ISO7816.OFFSET_P1];
        byte value_size = buffer[ISO7816.OFFSET_P2];

        if (key_size == 0 || value_size == 0)
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        
        if (apdu.setIncomingAndReceive() != (short) (buffer[ISO7816.OFFSET_LC] & 0xFF))
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        short ofsKey = ISO7816.OFFSET_CDATA;
        short ofsSecretValue = (short) (ofsKey + 1 + key_size);
        checkTLV(buffer, ofsKey, TAG_KEY, key_size, KeyValueRecord.SIZE_KEY);
        checkTLV(buffer, ofsSecretValue, TAG_SECRETVALUE, value_size, KeyValueRecord.SIZE_VALUE);

        // Check that key does not exist
        if (KeyValueRecord.search(buffer, (short) (ofsKey + 1), key_size) != null)
            ISOException.throwIt(SW_DUPLICATE_KEY);

        // Allocates and initializes a key value pair entry
        JCSystem.beginTransaction();
        KeyValueRecord record = KeyValueRecord.getInstance();
        record.setKey(buffer, (short) (ofsKey + 1), key_size);
        record.setSecretValue(buffer, (short) (ofsSecretValue + 1), value_size);
        JCSystem.commitTransaction();
    }
}
