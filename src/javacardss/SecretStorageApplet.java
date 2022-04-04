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

        byte[] buf = apdu.getBuffer();

        switch (buf[ISO7816.OFFSET_INS]) {
            case INS_VERIFY:
                verifyPin(apdu);
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
        //check(byte[] pin, short offset, byte length)

        if (m_user_pin.check(buffer, (short) 0, buffer[(short) 4])) {
            msgLength = (short) OK_PIN_MSG.length;
            Util.arrayCopyNonAtomic(OK_PIN_MSG, (short) 0, buffer, (short) 0, msgLength);
            apdu.setOutgoingAndSend((short) 0, msgLength);
        } else {
            msgLength = (short) BAD_PIN_MSG.length;
            Util.arrayCopyNonAtomic(BAD_PIN_MSG, (short) 0, buffer, (short) 0, msgLength);
            apdu.setOutgoingAndSend((short) 0, msgLength);
        }
    }

}
