import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtil {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 16;

    public static void saveVaultToEncryptedFile(String masterPassword, HashMap<String, PasswordVault.PasswordEntry> vault, String filePath) {
        try {
            SecretKey key = generateKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(vault);
            objectStream.close();

            byte[] encryptedData = cipher.doFinal(byteStream.toByteArray());

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                fileOut.write(encryptedData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save encrypted vault.");
        }
    }

    public static boolean loadVaultFromEncryptedFile(String masterPassword, HashMap<String, PasswordVault.PasswordEntry> vault, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return true;
            }

            SecretKey key = generateKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] encryptedData = Files.readAllBytes(file.toPath());
            byte[] decryptedData = cipher.doFinal(encryptedData);

            ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(decryptedData));
            @SuppressWarnings("unchecked")
			HashMap<String, PasswordVault.PasswordEntry> loadedVault = (HashMap<String, PasswordVault.PasswordEntry>) objectStream.readObject();
            objectStream.close();

            vault.clear();
            vault.putAll(loadedVault);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static SecretKey generateKey(String masterPassword) throws UnsupportedEncodingException {
        byte[] keyBytes = Arrays.copyOf(masterPassword.getBytes("UTF-8"), KEY_SIZE);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static void saveMasterPassword(String masterPassword, String filePath) {
        try {
            SecretKey key = generateKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedPassword = cipher.doFinal(masterPassword.getBytes("UTF-8"));

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                fileOut.write(encryptedPassword);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadMasterPassword(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null; 
            }

            byte[] encryptedPassword = Files.readAllBytes(file.toPath());

            SecretKey key = generateKey(new String(encryptedPassword));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decryptedPasswordBytes = cipher.doFinal(encryptedPassword);
            return new String(decryptedPasswordBytes, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
