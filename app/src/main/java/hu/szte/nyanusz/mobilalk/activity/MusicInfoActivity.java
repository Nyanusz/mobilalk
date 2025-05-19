package hu.szte.nyanusz.mobilalk.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.model.Music;

public class MusicInfoActivity extends AppCompatActivity {
    private TextView titleText, albumText, artistText, genreText, uploaderText, currentSongPosition, totalSongLength;
    private ImageView albumImage;
    private MaterialButton playPauseButton, stopButton;
    private SeekBar seekBar;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private MediaPlayer mediaPlayer;
    private Music song;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPreparing = false;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final ActivityResultLauncher<Intent> editMusicLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String updatedSongJson = result.getData().getStringExtra("updatedSong");
                    if (updatedSongJson != null) {
                        try {
                            String songId = song.getId(); // vagy akár extractáld az updatedSongJson-ból
                            fetchSongFromFirestore(songId);
                            reinitializeMediaPlayer();
                            Toast.makeText(this, "Song updated successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("MusicInfoActivity", "Error deserializing updated song JSON", e);
                            Toast.makeText(this, "Error loading updated song data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void fetchSongFromFirestore(String songId) {

        db.collection("songs").document(songId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        song = document.toObject(Music.class);
                        if (song != null) {
                            updateUI();
                            invalidateOptionsMenu();
                            reinitializeMediaPlayer();
                        } else {
                            Toast.makeText(this, "Failed to parse updated song", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Song not found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MusicInfoActivity", "Failed to fetch updated song", e);
                    Toast.makeText(this, "Error loading updated song", Toast.LENGTH_SHORT).show();
                });
    }

    private void reinitializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(song.getMp3Url());
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            isPreparing = true;
            mediaPlayer.prepareAsync();
            progressBar.setVisibility(ProgressBar.VISIBLE);

            mediaPlayer.setOnPreparedListener(mp -> {
                isPreparing = false;
                progressBar.setVisibility(ProgressBar.GONE);
                playPauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                seekBar.setMax(mediaPlayer.getDuration());
                totalSongLength.setText(formatDuration(mediaPlayer.getDuration()));
                handler.post(updateSeekBar);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setIconResource(R.drawable.ic_media_play);
                seekBar.setProgress(0);
                currentSongPosition.setText("0:00");
                playPauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            });

        } catch (IOException e) {
            Log.e("MusicInfoActivity", "Error reinitializing MediaPlayer", e);
            Toast.makeText(this, "Error loading updated MP3", Toast.LENGTH_SHORT).show();
            playPauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_info);

        // UI elemek inicializálása
        toolbar = findViewById(R.id.my_toolbar);
        progressBar = findViewById(R.id.progressBar);
        albumImage = findViewById(R.id.albumArt);
        titleText = findViewById(R.id.infoTitle);
        albumText = findViewById(R.id.infoAlbum);
        artistText = findViewById(R.id.infoArtist);
        genreText = findViewById(R.id.infoGenre);
        uploaderText = findViewById(R.id.infoUploader);
        seekBar = findViewById(R.id.seekBar);
        currentSongPosition = findViewById(R.id.currentSongPosition);
        totalSongLength = findViewById(R.id.totalSongLength);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Toolbar beállítása
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Gombok kezdetben letiltva
        playPauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        // Intentből adatok lekérése
        Intent intent = getIntent();
        String songJson = intent.getStringExtra("music");
        if (songJson == null) {
            Log.e("MusicInfoActivity", "No song data received in Intent");
            Toast.makeText(this, "No song data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Gson gson = new Gson();
            song = gson.fromJson(songJson, Music.class);
            if (song == null) {
                Log.e("MusicInfoActivity", "Failed to deserialize song JSON");
                Toast.makeText(this, "Invalid song data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e("MusicInfoActivity", "Error deserializing song JSON", e);
            Toast.makeText(this, "Error loading song data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Adatok megjelenítése
        updateUI();

        // MediaPlayer inicializálása
        if (song.getMp3Url() == null || song.getMp3Url().isEmpty()) {
            Log.e("MusicInfoActivity", "Invalid or missing mp3Url for song: " + song.getCim());
            Toast.makeText(this, "No MP3 URL available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(song.getMp3Url());
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            isPreparing = true;
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPreparing = false;
                progressBar.setVisibility(ProgressBar.GONE);
                playPauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                seekBar.setMax(mediaPlayer.getDuration());
                totalSongLength.setText(formatDuration(mediaPlayer.getDuration()));
                handler.post(updateSeekBar);
                Log.d("MusicInfoActivity", "MediaPlayer prepared for song: " + song.getCim());
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setIconResource(R.drawable.ic_media_play);
                seekBar.setProgress(0);
                currentSongPosition.setText("0:00");
                playPauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPreparing = false;
                progressBar.setVisibility(ProgressBar.GONE);
                playPauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                String errorMessage = getMediaPlayerErrorMessage(what, extra);
                Log.e("MusicInfoActivity", "MediaPlayer error: what=" + what + ", extra=" + extra + ", message=" + errorMessage);
                Toast.makeText(this, "Error playing MP3: " + errorMessage, Toast.LENGTH_LONG).show();
                return true;
            });
        } catch (IOException e) {
            Log.e("MusicInfoActivity", "Error initializing MediaPlayer for song: " + song.getCim(), e);
            Toast.makeText(this, "Error loading MP3: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(ProgressBar.GONE);
            finish();
            return;
        }

        // SeekBar eseménykezelés
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    currentSongPosition.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.postDelayed(updateSeekBar, 100);
            }
        });

        // SeekBar frissítése
        handler = new Handler(Looper.getMainLooper());
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentSongPosition.setText(formatDuration(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void updateUI() {
        titleText.setText(song.getCim());
        albumText.setText(song.getAlbumNev() != null ? song.getAlbumNev() : "Ismeretlen Album");
        artistText.setText(song.getEloado());
        genreText.setText(song.getMufaj());
        uploaderText.setText(song.getFeltolto() != null ? "Uploaded by: " + song.getFeltolto() : "Ismeretlen Uploader");

        String albumArtUri = song.getAlbumArtUri();
        if (albumArtUri != null && !albumArtUri.isEmpty()) {
            albumImage.setImageDrawable(null); // töröld előző képet
            albumImage.postDelayed(() -> {
                Glide.with(this)
                        .load(albumArtUri)
                        .error(R.drawable.baseline_album_64)
                        .into(albumImage);
            }, 1000); // 1 másodperces delay
        } else {
            albumImage.setImageResource(R.drawable.baseline_album_64);
            Log.w("MusicInfoActivity", "No album art URI for song: " + song.getCim());
        }

        albumImage.setAlpha(0f);
        albumImage.animate().alpha(1f).setDuration(1000).start();

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.music_info_menu, menu);
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && song != null && user.getUid().equals(song.getFeltolto())) {
            Log.d("MusicInfoActivity", "Showing edit and delete menu items for user: " + user.getUid() + ", uploader: " + song.getFeltolto());
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(true);
        } else {
            Log.d("MusicInfoActivity", "Hiding edit and delete menu items. User: " + (user != null ? user.getUid() : "null") + ", uploader: " + song.getFeltolto());
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            Intent intent = new Intent(this, EditMusicActivity.class);
            intent.putExtra("music", new Gson().toJson(song));
            editMusicLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_delete) {
            deleteSong();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void playPauseSelectedSong(View view) {
        if (mediaPlayer == null) {
            Toast.makeText(this, "Media player not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPreparing) {
            Toast.makeText(this, "Media player is preparing, please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setIconResource(R.drawable.ic_media_play);
            handler.removeCallbacks(updateSeekBar);
        } else {
            try {
                mediaPlayer.start();
                playPauseButton.setIconResource(R.drawable.ic_media_pause);
                sendNotification();
                handler.post(updateSeekBar);
            } catch (IllegalStateException e) {
                Log.e("MusicInfoActivity", "IllegalStateException in playPauseSelectedSong", e);
                Toast.makeText(this, "Error playing MP3: Player not ready", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stopSelectedSong(View view) {
        if (mediaPlayer == null) {
            Toast.makeText(this, "Media player not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPreparing) {
            Toast.makeText(this, "Media player is preparing, please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        mediaPlayer.stop();
        playPauseButton.setIconResource(R.drawable.ic_media_play);
        seekBar.setProgress(0);
        currentSongPosition.setText("0:00");
        playPauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getMp3Url());
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            isPreparing = true;
            mediaPlayer.prepareAsync();
            Log.d("MusicInfoActivity", "MediaPlayer reset and preparing for song: " + song.getCim());
        } catch (IOException e) {
            Log.e("MusicInfoActivity", "Error resetting MediaPlayer for song: " + song.getCim(), e);
            Toast.makeText(this, "Error resetting MP3: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSong() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !user.getUid().equals(song.getFeltolto())) {
            Toast.makeText(this, "You are not authorized to delete this song", Toast.LENGTH_SHORT).show();
            return;
        }

        // Törlés megerősítése (opcionális, később dialogus ablakkal bővíthető)
        Toast.makeText(this, "Deleting song...", Toast.LENGTH_SHORT).show();

        // Firestore dokumentum törlése
        db.collection("songs").document(song.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    // MP3 törlése
                    if (song.getMp3Url() != null && !song.getMp3Url().isEmpty()) {
                        storage.getReferenceFromUrl(song.getMp3Url()).delete()
                                .addOnSuccessListener(aVoid1 -> Log.d("MusicInfoActivity", "Deleted MP3: " + song.getMp3Url()))
                                .addOnFailureListener(e -> Log.w("MusicInfoActivity", "Failed to delete MP3: " + song.getMp3Url(), e));
                    }
                    // Kép törlése
                    if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()) {
                        storage.getReferenceFromUrl(song.getAlbumArtUri()).delete()
                                .addOnSuccessListener(aVoid1 -> Log.d("MusicInfoActivity", "Deleted image: " + song.getAlbumArtUri()))
                                .addOnFailureListener(e -> Log.w("MusicInfoActivity", "Failed to delete image: " + song.getAlbumArtUri(), e));
                    }
                    Toast.makeText(this, "Song deleted successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("MusicInfoActivity", "Failed to delete song: " + song.getId(), e);
                    Toast.makeText(this, "Failed to delete song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "song_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Now Playing")
                .setContentText(song.getCim() + " by " + song.getEloado())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    private String getMediaPlayerErrorMessage(int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                return "Unknown error";
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                return "Server died";
            case MediaPlayer.MEDIA_ERROR_IO:
                return "I/O error";
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                return "Malformed media";
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                return "Unsupported format";
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                return "Timed out";
            default:
                return "Error code: " + what + ", extra: " + extra;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setIconResource(R.drawable.ic_media_play);
            handler.removeCallbacks(updateSeekBar);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
    }
}