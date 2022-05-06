package model;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;

import java.io.ByteArrayOutputStream;
//import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class RemoteInputStreamUtils {
    public static byte[] toBytes(RemoteInputStream remoteInputStream) throws Exception {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = RemoteInputStreamClient.wrap(remoteInputStream);

            org.apache.commons.io.IOUtils.copy(inputStream, baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
