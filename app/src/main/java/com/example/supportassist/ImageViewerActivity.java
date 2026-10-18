package com.example.supportassist;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView ivFullImage = findViewById(R.id.iv_full_image);
        ImageButton btnClose = findViewById(R.id.btn_close);
        ProgressBar pbLoading = findViewById(R.id.pb_loading);

        String imageUrl = getIntent().getStringExtra("image_url");
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Error: Image URL not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnClose.setOnClickListener(v -> finish());

        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getAccessToken();

        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .build());

        pbLoading.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(glideUrl)
                .placeholder(android.R.color.black)
                .error(android.R.drawable.ic_menu_report_image)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(android.graphics.drawable.Drawable resource, com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        pbLoading.setVisibility(View.GONE);
                        ivFullImage.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                        ivFullImage.setImageDrawable(placeholder);
                    }

                    @Override
                    public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                        pbLoading.setVisibility(View.GONE);
                        super.onLoadFailed(errorDrawable);
                        Toast.makeText(ImageViewerActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
