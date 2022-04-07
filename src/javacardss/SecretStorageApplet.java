package javacardss;

import javacard.framework.*;

/**
 * @author zargham ahmad
 */
public class SecretStorageApplet extends Applet {

    private OwnerPIN m_duress_pin = null;
    private OwnerPIN m_user_pin = null;
    private static final byte[] OK_PIN_MSG = {'P', 'i', 'n', ' ', 'O', 'K', '!'};
    private static final byte[] BAD_PIN_MSG = {'B', 'A', 'D', ' ', 'P', 'I', 'N', '!'};

    private final static short SW_WRONG_PIN = 0x63c0;

    // INS byte for the command that verifies the PIN (from ISO7816-4)
    private final static byte INS_VERIFY = 0x20;

    public final static byte PIN_TRY_LIMIT = 0x03;
    public final static byte PIN_SIZE = 0x04;
    private static byte[] default_pin = {0x31, 0x32, 0x33, 0x34};

    // INS byte for the command that adds a secret value pair entry
    public final static byte INS_ADD_KEYVALUE_ENTRY = (byte) 0x30;
    // Status word for a duplicate key
    public final static short SW_DUPLICATE_KEY = (short) 0x6A8A;
    // Tag byte for key
    public final static byte TAG_KEY = (byte) 0xF1;
    // Tag byte for secret records
    public final static byte TAG_SECRETVALUE = (byte) 0xF2;

    private keyValueManager current;

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

        switch (buffer[ISO7816.OFFSET_INS]) {
            case INS_VERIFY:
                verifyPin(apdu);
                break;
            case INS_ADD_KEYVALUE_ENTRY:
                Authorize();
                addSecretValue();
                break;

            default:
                // good practice: If you don't know the INStruction, say so:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }

    }

    private void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short msgLength;

        if (m_user_pin.check(buffer, (short) 5, PIN_SIZE)) {
            msgLength = (short) OK_PIN_MSG.length;
            Util.arrayCopyNonAtomic(OK_PIN_MSG, (short) 0, buffer, (short) 0, msgLength);
            apdu.setOutgoingAndSend((short) 0, msgLength);
        } else {
            msgLength = (short) BAD_PIN_MSG.length;
            Util.arrayCopyNonAtomic(BAD_PIN_MSG, (short) 0, buffer, (short) 0, msgLength);
            apdu.setOutgoingAndSend((short) 0, msgLength);
        }
    }

    void Authorize() {
        if (!m_user_pin.isValidated())
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }

    short checkTLV(byte[] buffer, short inOfs,
                   byte tag, short maxLen) {
        if (buffer[inOfs++] != tag)
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        short len = buffer[inOfs++];

        if (len > maxLen)
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        return (short) (inOfs + len);
    }

    void addSecretValue() {
        APDU apdu = APDU.getCurrentAPDU();
        byte[] buffer = APDU.getCurrentAPDUBuffer();

        // Checks the value of P1&P2
        if (Util.getShort(buffer, ISO7816.OFFSET_P1) != 0)
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        // Checks the minimum length
//        if ((short) (buffer[ISO7816.OFFSET_LC] & 0xFF) < 3)
//            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        // Receives data and checks its length
        if (apdu.setIncomingAndReceive() !=
                (short) (buffer[ISO7816.OFFSET_LC] & 0xFF))
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        short ofsKey = ISO7816.OFFSET_CDATA;

        short ofsSecretValue = checkTLV(buffer, ofsKey, TAG_KEY, keyValueManager.SIZE_KEY);
//        if (buffer[ISO7816.OFFSET_LC] < (short) (ofsSecretValue - 2))
//            ISOException.throwIt(ISO7816.SW_DATA_INVALID);

        // Checks the password
//        if (checkTLV(buffer, ofsSecretValue, TAG_SECRETVALUE, keyValueManager.SIZE_SECRETVALUE) !=
//                (short) (ISO7816.OFFSET_CDATA + (short) (buffer[ISO7816.OFFSET_LC] & 0xFF)))
//            ISOException.throwIt(ISO7816.SW_DATA_INVALID);

        // Search the identifier in the current base
        if (keyValueManager.search(buffer, (short) (ofsKey + 2), buffer[(short) (ofsKey + 1)]) != null)
            ISOException.throwIt(SW_DUPLICATE_KEY);

        // Allocates and initializes a password entry
        JCSystem.beginTransaction();
        keyValueManager keyManager = keyValueManager.getInstance();
        keyManager.setKey(buffer, (short) (ofsKey + 2), buffer[(short) (ofsKey + 1)]);
        keyManager.setSecretValue(buffer, (short) (ofsSecretValue + 2), buffer[(short) (ofsSecretValue + 1)]);
        JCSystem.commitTransaction();
    }
}
