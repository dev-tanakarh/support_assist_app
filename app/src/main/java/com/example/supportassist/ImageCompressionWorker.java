package com.example.supportassist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ImageCompressionWorker extends Worker {

    public ImageCompressionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUriString = getInputData().getString("image_uri");
        int targetWidth = getInputData().getInt("target_width", 0);
        int targetHeight = getInputData().getInt("target_height", 0);
        boolean cropSquare = getInputData().getBoolean("crop_square", false);

        if (imageUriString == null) return Result.failure();

        Uri imageUri = Uri.parse(imageUriString);
        Context context = getApplicationContext();

        try {
            // Load bitmap with rotation handled
            Bitmap bitmap = loadBitmapWithRotation(context, imageUri);
            if (bitmap == null) return Result.failure();

            Bitmap processedBitmap = bitmap;

            if (targetWidth > 0 && targetHeight > 0) {
                if (cropSquare) {
                    processedBitmap = cropToSquare(bitmap);
                    processedBitmap = Bitmap.createScaledBitmap(processedBitmap, targetWidth, targetHeight, true);
                } else {
                    processedBitmap = resizeMaintainingAspectRatio(bitmap, targetWidth, targetHeight);
                }
            }

            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("compressed_" + UUID.randomUUID(), ".jpg", outputDir);
            
            FileOutputStream out = new FileOutputStream(outputFile);
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();

            // Cleanup
            if (processedBitmap != bitmap) processedBitmap.recycle();
            bitmap.recycle();

            Data outputData = new Data.Builder()
                    .putString("compressed_path", outputFile.getAbsolutePath())
                    .build();

            return Result.success(outputData);
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private Bitmap loadBitmapWithRotation(Context context, Uri uri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        is.close();

        if (bitmap == null) return null;

        // Handle rotation from EXIF
        InputStream exifIs = context.getContentResolver().openInputStream(uri);
        ExifInterface exif = new ExifInterface(exifIs);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        exifIs.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            default: return bitmap;
        }

        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotated;
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newEdge = Math.min(width, height);
        int xOffset = (width - newEdge) / 2;
        int yOffset = (height - newEdge) / 2;
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, newEdge, newEdge);
    }

    private Bitmap resizeMaintainingAspectRatio(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        if (ratio >= 1.0f) return bitmap; // Don't upscale

        int finalWidth = Math.round(ratio * width);
        int finalHeight = Math.round(ratio * height);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }
}
