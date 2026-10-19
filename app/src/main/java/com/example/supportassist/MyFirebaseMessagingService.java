package com.example.supportassist;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = null;
        String message = null;
        String ticketId = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
        }
        
        if (remoteMessage.getData().size() > 0) {
            if (title == null) title = remoteMessage.getData().get("title");
            if (message == null) message = remoteMessage.getData().get("message");
            ticketId = remoteMessage.getData().get("ticket_id");
        }

        if (title != null && message != null) {
            showNotification(title, message, ticketId);
        }
        
        // Refresh local cache silently when a notification arrives to ensure UI is fresh
        DataRepository.getInstance(this).syncDataSilently();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "FCM token rotated");
        if (tokenManager.getAccessToken() != null) {
            FcmTokenSync.send(apiService, token);
        }
    }

    private void showNotification(String title, String message, String ticketId) {
        String channelId = "ticket_updates";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Ticket Updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for ticket updates and alerts");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent;
        if (ticketId != null && !ticketId.isEmpty()) {
            // Stage 7: Direct navigation to ticket details
            intent = new Intent(this, TicketDetailsActivity.class);
            intent.putExtra("ticket_id", ticketId);
            // Default to trying unassigned access if user is staff, 
            // the Activity's self-healing logic will handle the rest.
            intent.putExtra("is_unassigned", tokenManager.isTechnician() || tokenManager.isAdmin());
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
