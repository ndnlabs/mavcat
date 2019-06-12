package labs.ndn.mavcat.server.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTools {
    public static String getDirListReprezenation(String dirPath,String requestPath) {
        StringBuilder sb = new StringBuilder("dir ");
        sb.append(requestPath);

        File dir = new File(dirPath);
        if (!dir.exists()) {
            return "dir ###";
        }
        String res = "The directory is empty";
        String[] files = dir.list();
        if (files.length != 0) {
            for (String aFile : files) {
                sb.append(" ");
                sb.append(aFile);
                sb.append(",");
                File file1 = new File(dirPath+aFile);
                sb.append(file1.isDirectory()?1:0);
                sb.append(",");
                sb.append(file1.length());

            }
        }
        return sb.toString();
    }

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
}
