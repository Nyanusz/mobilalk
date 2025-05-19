package hu.szte.nyanusz.mobilalk.activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.model.Music;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class AddMusicActivity extends AppCompatActivity {
    private EditText titleInput, artistInput, genreInput, albumInput;
    private ImageView albumImageView;
    private MaterialButton playPauseButton;
    private TextView currentPos, totalLength;
    private SeekBar seekBar;
    private Button selectMp3Button, selectImageButton, saveButton;
    private Uri mp3Uri, imageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBar;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (!allGranted) {
                    Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> mp3PickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    mp3Uri = result.getData().getData();
                    Toast.makeText(this, "MP3 selected", Toast.LENGTH_SHORT).show();
                    initializeMediaPlayer();
                } else {
                    Toast.makeText(this, "Failed to select MP3", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    Glide.with(this).load(imageUri).into(albumImageView);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_music);

        // Inicializálás
        titleInput = findViewById(R.id.titleInput);
        artistInput = findViewById(R.id.artistInput);
        genreInput = findViewById(R.id.genreInput);
        albumInput = findViewById(R.id.albumInput);
        albumImageView = findViewById(R.id.albumImage);
        playPauseButton = findViewById(R.id.playPauseButton);
        currentPos = findViewById(R.id.currentSongPosition);
        totalLength = findViewById(R.id.totalSongLength);
        seekBar = findViewById(R.id.seekBar);
        selectMp3Button = findViewById(R.id.selectMp3Button);
        selectImageButton = findViewById(R.id.selectImageButton);
        saveButton = findViewById(R.id.saveButton);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        handler = new Handler(Looper.getMainLooper());

        // Értesítési csatorna létrehozása
        createNotificationChannel();

        // Engedély kérése
        requestPermissions();

        // Gomb események
        selectMp3Button.setOnClickListener(v -> selectMp3());
        selectImageButton.setOnClickListener(v -> selectImage());
        saveButton.setOnClickListener(v -> {
            saveButton.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction(() -> saveButton.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
            saveSong();
        });
        playPauseButton.setOnClickListener(v -> playPauseMp3());

        // SeekBar eseménykezelés
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentPos.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    handler.postDelayed(updateSeekBar, 100);
                }
            }
        });

        // SeekBar frissítése
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentPos.setText(formatDuration(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void requestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        if (!hasPermissions(permissions)) {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "song_channel",
                    "Song Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for song actions");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void selectMp3() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/mpeg");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            mp3PickerLauncher.launch(Intent.createChooser(intent, "Select MP3"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to select MP3", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to select image", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, mp3Uri);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mediaPlayer.prepare();
            seekBar.setMax(mediaPlayer.getDuration());
            totalLength.setText(formatDuration(mediaPlayer.getDuration()));
            currentPos.setText("0:00");
            playPauseButton.setEnabled(true);
            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setIconResource(R.drawable.ic_media_play);
                seekBar.setProgress(0);
                currentPos.setText("0:00");
            });
        } catch (IOException e) {
            Toast.makeText(this, "Error loading MP3: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            playPauseButton.setEnabled(false);
        }
    }

    private void playPauseMp3() {
        if (mediaPlayer == null || mp3Uri == null) {
            Toast.makeText(this, "Please select an MP3 first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setIconResource(R.drawable.ic_media_play);
            handler.removeCallbacks(updateSeekBar);
        } else {
            mediaPlayer.start();
            playPauseButton.setIconResource(R.drawable.ic_media_pause);
            handler.postDelayed(updateSeekBar, 100);
            sendNotification("Playing Preview", "Previewing " + titleInput.getText().toString());
        }
    }

    private boolean isUriValid(Uri uri) {
        if (uri == null) return false;
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            if (stream != null) {
                stream.close();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private long getFileSize(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    private void saveSong() {
        String title = titleInput.getText().toString().trim();
        String artist = artistInput.getText().toString().trim();
        String genre = genreInput.getText().toString().trim();
        String album = albumInput.getText().toString().trim();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || artist.isEmpty() || mp3Uri == null) {
            Toast.makeText(this, "Please fill all required fields and select an MP3", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isUriValid(mp3Uri)) {
            Toast.makeText(this, "Invalid MP3 file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fájlméret ellenőrzése (max 10 MB)
        long fileSize = getFileSize(mp3Uri);
        if (fileSize > 10 * 1024 * 1024 || fileSize == -1) {
            Toast.makeText(this, "MP3 file is too large or invalid (max 10 MB)", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = (mediaPlayer != null) ? mediaPlayer.getDuration() / 1000 : 0;
        String songId = db.collection("songs").document().getId();
        StorageReference mp3Ref = storage.getReference().child("songs/" + songId + ".mp3");
        StorageReference imageRef = imageUri != null && isUriValid(imageUri) ?
                storage.getReference().child("images/" + songId + ".jpg") : null;

        // Metaadatok az MP3-hoz
        StorageMetadata mp3Metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

        // MP3 feltöltési feladat
        Task<Uri> mp3UploadTask = mp3Ref.putFile(mp3Uri, mp3Metadata).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return mp3Ref.getDownloadUrl();
        });

        // Képfeltöltési feladat (ha van kép)
        Task<Uri> imageUploadTask = imageRef != null ?
                imageRef.putFile(imageUri, new StorageMetadata.Builder().setContentType("image/jpeg").build())
                        .continueWithTask(task -> {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return imageRef.getDownloadUrl();
                        }) : Tasks.forResult(null);

        // Várjuk meg mindkét feladat befejezését
        Tasks.whenAllSuccess(mp3UploadTask, imageUploadTask).addOnSuccessListener(results -> {
            Uri mp3Url = (Uri) results.get(0);
            Uri imageUrl = (Uri) results.get(1);
            saveSongToFirestore(songId, title, artist, genre, album, duration, mp3Url, imageUrl.toString(), user.getUid());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveSongToFirestore(String id, String title, String artist, String genre, String album, int duration, Uri mp3Url, String imageUrl, String userId) {
        Music song = new Music(id, title, artist, genre, album, duration, mp3Url != null ? mp3Url.toString() : null, imageUrl, false, userId);
        db.collection("songs").document(id).set(song)
                .addOnSuccessListener(aVoid -> {
                    sendNotification("Song Added", title + " by " + artist + " has been added!");
                    Toast.makeText(this, "Song added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(String title, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "song_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
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