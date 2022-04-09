package javacardss;

import javacard.framework.*;

/**
 *
 * @author zargham ahmad
 */
public class KeyValueRecord {
    public static short SIZE_KEY = 32;
    public static short SIZE_VALUE = 64;
    
    private KeyValueRecord next;
    private static KeyValueRecord first;
    private static KeyValueRecord deleted;

    private byte[] key;
    private byte[] secretValue;

    private byte keyLength;
    private byte secretValueLength;

    private KeyValueRecord() {
        key = new byte[SIZE_KEY];
        secretValue = new byte[SIZE_VALUE];
        next = first;
        first = this;
    }

    static KeyValueRecord getInstance() {
        if (deleted == null) {
            // There is no element to recycle
            return new KeyValueRecord();
        } else {
            // Recycle the first available element
            KeyValueRecord instance = deleted;
            deleted = instance.next;
            instance.next = first;
            first = instance;
            return instance;
        }
    }

    static KeyValueRecord search(byte[] buf, short ofs, byte len) {
        for (KeyValueRecord record = first; record != null; record = record.next) {
            if (record.keyLength != len) continue;
            if (Util.arrayCompare(record.key, (short) 0, buf, ofs, len) == 0)
                return record;
        }
        return null;
    }

    public static KeyValueRecord getFirst() {
        return first;
    }

    private void remove() {
        if (first == this) {
            first = next;
        } else {
            for (KeyValueRecord record = first; record != null; record = record.next)
                if (record.next == this)
                    record.next = next;
        }
    }

    private void recycle() {
        next = deleted;
        keyLength = 0;
        secretValueLength = 0;
        deleted = this;
    }

    static void delete(byte[] buf, short ofs, byte len) {
        KeyValueRecord keyManager = search(buf, ofs, len);
        if (keyManager != null) {
            JCSystem.beginTransaction();
            keyManager.remove();
            keyManager.recycle();
            JCSystem.commitTransaction();
        }
    }
    
    static short getAllKeys(byte[] buf, byte ofs) {
        short len = 0;
        
        for (KeyValueRecord record = first; record != null; record = record.next) {                
            Util.arrayCopy(record.key, (short) 0, buf, ofs, record.keyLength);
            ofs += record.keyLength ;
            len += record.keyLength;
        }
        
        return len;
    }
    
    static short getAllKeyLens(byte[] buf, byte ofs) {
        short len = 0;
        
        for (KeyValueRecord record = first; record != null; record = record.next) {
            buf[ofs++] = record.keyLength;
            len++;
        }

        return len;
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

    public KeyValueRecord getNext() {
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
