package hu.szte.nyanusz.mobilalk.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;


public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
   // @Getter
    private Uri mediaUri;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";
/*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PLAY:
                        start();
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_STOP:
                        stop();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

 */

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void create(Uri uri) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaUri = uri;
        Log.d("MusicService", "Media player created: " + uri.toString());
    }

    public void start() {
        if (mediaPlayer != null) {
            Log.d("MusicService", "Media player started");
            mediaPlayer.start();
        }
        Log.d("MusicService", "Media player is null, cannot be started");
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d("MusicService", "Media player paused");
        }
        Log.d("MusicService", "Media player is null or not playing, cannot be paused");
    }
/*
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            if (mediaUri != null) {
                mediaPlayer = MediaPlayer.create(this, mediaUri);
                Log.d("MusicService", "Media player created: " + mediaUri.toString());
            }
            Log.d("MusicService", "Media player stopped");
        }
        Log.d("MusicService", "Media player is null, cannot be stopped");

        MusicNotificationManager musicNotificationManager = new MusicNotificationManager(this);
        NotificationManagerCompat notificationManager = musicNotificationManager.getNotificationManager();
        notificationManager.cancel(1);
    }
*/
    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}