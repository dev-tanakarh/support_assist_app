package com.example.supportassist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class ImageCompressionWorker extends Worker {

    public ImageCompressionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUriString = getInputData().getString("image_uri");
        if (imageUriString == null) return Result.failure();

        Uri imageUri = Uri.parse(imageUriString);
        Context context = getApplicationContext();

        try {
            // 4. Media Handling: Image Compression
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri));
            
            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("compressed_" + UUID.randomUUID(), ".jpg", outputDir);
            
            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out); // Compress to 70% quality
            out.flush();
            out.close();

            Data outputData = new Data.Builder()
                    .putString("compressed_path", outputFile.getAbsolutePath())
                    .build();

            return Result.success(outputData);
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
