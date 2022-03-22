/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package javacardss;
import javacard.framework.*;

/**
 *
 * @author zargham ahmad
 */
public final class JavaCardSS extends Applet {

    private OwnerPIN m_duress_pin = null;
    private OwnerPIN m_user_pin = null;
    private static final byte[] PIN_OK_MSG={(byte)'P',(byte)'i',(byte)'n',(byte)' ',(byte)'O',(byte)'K',(byte)'!'};
    private static final byte[] BAD_PIN_MSG={(byte)'B',(byte)'A',(byte)'D',(byte)' ',(byte)'P',(byte)'I',(byte)'N',(byte)'!'};

    private final static short SW_WRONG_PIN = (short) 0x63c0;

    /**
     * INS byte for the command that verifies the PIN
     * (from ISO7816-4)
     */
    public final static byte INS_VERIFY = (byte) 0x20;

    /**
     * PIN try limit
     */
    public final static byte PIN_TRY_LIMIT = (byte) 3;

    /**
     * PIN Maximum size
     */
    public final static byte PIN_MAX_SIZE = (byte) 4;
    private static byte[] pinDefault={0x31, 0x32, 0x33, 0x34};
    
    private JavaCardSS() {
	m_user_pin = new OwnerPIN ((byte)3, (byte)4);
	//byte[] pin, short offset, byte length
	m_user_pin.update(pinDefault,(short)0,(byte)4);
	}
    
    public static void install
        (byte[] bArray, short bOffset, byte bLength) {
        new JavaCardSS().register();
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
        }
         
    }

    private void verifyPin (APDU apdu) {
        byte[] buffer=apdu.getBuffer();
	short msgLength;
	//check(byte[] pin, short offset, byte length)
	if (m_user_pin.check(buffer,(short)0,buffer[(short)4])) {
            msgLength=(short)PIN_OK_MSG.length;
            Util.arrayCopyNonAtomic(PIN_OK_MSG, (short)0, buffer, (short)0, msgLength);
            apdu.setOutgoingAndSend((short)0, msgLength);
	}
	else {
            msgLength = (short) BAD_PIN_MSG.length;
            Util.arrayCopyNonAtomic(BAD_PIN_MSG, (short)0, buffer, (short)0, msgLength);
            apdu.setOutgoingAndSend((short)0, msgLength);
	}
    }

}
