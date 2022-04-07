package javacardss;

import javacard.framework.*;

/**
 *
 * @author zargham ahmad
 */
public class keyValueManager {
    public static short SIZE_KEY = 16;
    public static short SIZE_SECRETVALUE = 16;

    private keyValueManager next;
    private static keyValueManager first;
    private static keyValueManager deleted;

    private byte[] key;
    private byte[] secretValue;

    private byte keyLength;
    private byte secretValueLength;

    private keyValueManager() {
        // Allocates all fields
        key = new byte[SIZE_KEY];
        secretValue = new byte[SIZE_SECRETVALUE];
        // The new element is inserted in front of the list
        next = first;
        first = this;
    }

    static keyValueManager getInstance() {
        if (deleted == null) {
            // There is no element to recycle
            return new keyValueManager();
        } else {
            // Recycle the first available element
            keyValueManager instance = deleted;
            deleted = instance.next;
            instance.next = first;
            first = instance;
            return instance;
        }
    }

    static keyValueManager search(byte[] buf, short ofs, byte len) {
        for (keyValueManager keyManager = first; keyManager != null; keyManager = keyManager.next) {
            if (keyManager.keyLength != len) continue;
            if (Util.arrayCompare(keyManager.key, (short) 0, buf, ofs, len) == 0)
                return keyManager;
        }
        return null;
    }

    public static keyValueManager getFirst() {
        return first;
    }

    private void remove() {
        if (first == this) {
            first = next;
        } else {
            for (keyValueManager keyManager = first; keyManager != null; keyManager = keyManager.next)
                if (keyManager.next == this)
                    keyManager.next = next;
        }
    }

    private void recycle() {
        next = deleted;
        keyLength = 0;
        secretValueLength = 0;
        deleted = this;
    }

    static void delete(byte[] buf, short ofs, byte len) {
        keyValueManager keyManager = search(buf, ofs, len);
        if (keyManager != null) {
            JCSystem.beginTransaction();
            keyManager.remove();
            keyManager.recycle();
            JCSystem.commitTransaction();
        }
    }

    byte getKey(byte[] buf, short ofs) {
        Util.arrayCopy(key, (short) 0, buf, ofs, keyLength);
        return keyLength;
    }

    byte getSecretValue(byte[] buf, short ofs) {
        Util.arrayCopy(secretValue, (short) 0, buf, ofs, secretValueLength);
        return secretValueLength;
    }

    public byte getKeyLength() {
        return keyLength;
    }

    public byte getSecretValueLength() {
        return secretValueLength;
    }

    public keyValueManager getNext() {
        return next;
    }

    public void setKey(byte[] buf, short ofs, byte len) {
        Util.arrayCopy(buf, ofs, key, (short) 0, len);
        keyLength = len;
    }

    public void setSecretValue(byte[] buf, short ofs, byte len) {
        Util.arrayCopy(buf, ofs, secretValue, (short) 0, len);
        secretValueLength = len;
    }
}
