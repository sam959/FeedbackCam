package worker;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SaveImageRunnable implements Runnable {
    private ByteBuffer buffer;
    private BufferedOutputStream bufferedOutputStream;
    private String outputFileName;
    private int width;
    private int height;
    private Matrix matrix = new Matrix();

    public SaveImageRunnable(ByteBuffer buffer, String outputFileName, int width, int height) {
        this.buffer = buffer;
        this.outputFileName = outputFileName;
        this.width = width;
        this.height = height;
        matrix.postScale(1, -1, width / 2, height / 2);
    }

    @Override
    public void run() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFileName));
            //pixelBuffer.rewind();
            bitmap.copyPixelsFromBuffer(buffer);
            Bitmap bitmapToSave = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            buffer.rewind();
            bitmapToSave.copyPixelsFromBuffer(buffer);

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapToSave, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 90, bufferedOutputStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
