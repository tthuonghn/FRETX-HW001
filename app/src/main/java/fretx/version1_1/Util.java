package fretx.version1_1;

import android.content.Context;
import java.io.*;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import fretx.version1_1.item.SongItem;

/**
 * Created by Misho on 2/21/2016.
 */
public final class Util {
    private Util() {
    }

    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;
    private static TransferUtility sTransferUtility;

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param context An Context instance.
     * @return A default credential provider.
     */
    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    Constants.COGNITO_POOL_ID,
                    Regions.EU_WEST_1);
        }
        return sCredProvider;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param context An Context instance.
     * @return A default S3 client.
     */
    public static AmazonS3Client getS3Client(Context context) {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
        }
        return sS3Client;
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @param context
     * @return a TransferUtility instance
     */
    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(context.getApplicationContext()),
                    context.getApplicationContext());
        }

        return sTransferUtility;
    }

    public static void saveHWAccess(Context ctx, String btMACAddress){
        File hwAccessFile = new File(ctx.getFilesDir().toString() + "/" + Constants.HW_BUCKET_MAPPING_FILE);
        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(hwAccessFile);
            outputStream.write(btMACAddress.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SongItem setSongItem(String songName, String songUrl, String sontText){
        SongItem itemData = new SongItem();
        itemData.songName = songName;
        itemData.songURl = songUrl;
        itemData.songTxt = sontText;

        return itemData;
    }
    ///Read the text file from resource(Raw) and divide by end line mark('\n")
    public static String readRawTextFile(Context ctx, String chordFilePath) {
        //InputStream inputStream = ctx.getResources().openRawResource(chordFilePath);
        try {
            InputStreamReader inputreader = new InputStreamReader(new FileInputStream(new File(chordFilePath)));
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            StringBuilder text = new StringBuilder();

            try {
                while ((line = buffreader.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
            } catch (IOException e) {
                return null;
            }
            return text.toString();
        } catch (FileNotFoundException e){
            return null;
        }
    }
    public static void setDefaultValues(Boolean[] bArray)
    {
        for (int i = 0; i < bArray.length; i ++){
            bArray[i] = false;
        }
    }
    public static byte[] str2array(String string){
        String strSub = string.replaceAll("[{}]", "");
        String[] parts = strSub.split(",");
        byte[] array = new byte[parts.length];
        for (int i = 0; i < parts.length; i ++)
        {
            array[i] = Byte.valueOf(parts[i]);
        }
        return array;
    }
    public static void startViaData(byte[] array) {
        BluetoothClass.mHandler.obtainMessage(BluetoothClass.ARDUINO, array).sendToTarget();
    }

    public static void stopViaData() {
        byte[] array = new byte[]{0};
        BluetoothClass.mHandler.obtainMessage(BluetoothClass.ARDUINO, array).sendToTarget();
    }

    /*
     * Fills in the map with information in the observer so that it can be used
     * with a SimpleAdapter to populate the UI
     */
    public static void fillMap(Map<String, Object> map, TransferObserver observer, boolean isChecked) {
        int progress = (int) ((double) observer.getBytesTransferred() * 100 / observer
                .getBytesTotal());
        map.put("id", observer.getId());
        map.put("checked", isChecked);
        map.put("fileName", observer.getAbsoluteFilePath());
        map.put("progress", progress);
        map.put("bytes",
                getBytesString(observer.getBytesTransferred()) + "/"
                        + getBytesString(observer.getBytesTotal()));
        map.put("state", observer.getState());
        map.put("percentage", progress + "%");
    }

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    public static String getBytesString(long bytes) {
        String[] quantifiers = new String[] {
                "KB", "MB", "GB", "TB"
        };
        double speedNum = bytes;
        for (int i = 0;; i++) {
            if (i >= quantifiers.length) {
                return "";
            }
            speedNum /= 1024;
            if (speedNum < 512) {
                return String.format("%.2f", speedNum) + " " + quantifiers[i];
            }
        }
    }

}
