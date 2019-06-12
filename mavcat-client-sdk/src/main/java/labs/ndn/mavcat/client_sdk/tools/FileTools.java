package labs.ndn.mavcat.client_sdk.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTools {


    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static String getFileSha256(String filename) throws IOException {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("sha not exist,exist now");
            System.exit(-1);
            e.printStackTrace();
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();

        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static void removeDownloadFile(String filename) {
        new File(filename+".download").delete();
        new File(filename+".downloadinfo").delete();
    }
}
