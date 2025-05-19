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
import android.util.Log;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.model.Music;

public class EditMusicActivity extends AppCompatActivity {
    private EditText titleInput, artistInput, genreInput, albumInput;
    private ImageView albumImageView;
    private MaterialButton playPauseButton;
    private TextView currentPos, totalLength;
    private SeekBar seekBar;
    private Button selectMp3Button, selectImageButton, saveButton;
    private MaterialToolbar toolbar;
    private Uri mp3Uri, imageUri;
    private String oldMp3Url, oldImageUrl;
    private Music song;
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
                    Log.d("EditMusicActivity", "MP3 selected: " + mp3Uri.toString());
                    Toast.makeText(this, "MP3 selected", Toast.LENGTH_SHORT).show();
                    initializeMediaPlayer();
                } else {
                    Log.w("EditMusicActivity", "Failed to select MP3");
                    Toast.makeText(this, "Failed to select MP3", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    Log.d("EditMusicActivity", "Image selected: " + imageUri.toString());
                    Glide.with(this).load(imageUri).into(albumImageView);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("EditMusicActivity", "Failed to select image");
                    Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_music);

        // Inicializálás
        toolbar = findViewById(R.id.my_toolbar);
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

        // Toolbar beállítása
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Értesítési csatorna létrehozása
        createNotificationChannel();

        // Engedély kérése
        requestPermissions();

        // Dal adatainak betöltése
        Intent intent = getIntent();
        String songJson = intent.getStringExtra("music");
        if (songJson == null) {
            Log.e("EditMusicActivity", "No song data received in Intent");
            Toast.makeText(this, "No song data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Gson gson = new Gson();
            song = gson.fromJson(songJson, Music.class);
            if (song == null) {
                Log.e("EditMusicActivity", "Failed to deserialize song JSON");
                Toast.makeText(this, "Invalid song data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e("EditMusicActivity", "Error deserializing song JSON", e);
            Toast.makeText(this, "Error loading song data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Jogosultság ellenőrzése
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !user.getUid().equals(song.getFeltolto())) {
            Log.e("EditMusicActivity", "Unauthorized edit attempt. User: " + (user != null ? user.getUid() : "null") + ", Feltolto: " + song.getFeltolto());
            Toast.makeText(this, "You are not authorized to edit this song", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Adatok megjelenítése
        titleInput.setText(song.getCim());
        artistInput.setText(song.getEloado());
        genreInput.setText(song.getMufaj());
        albumInput.setText(song.getAlbumNev());
        oldMp3Url = song.getMp3Url();
        oldImageUrl = song.getAlbumArtUri();
        mp3Uri = oldMp3Url != null && !oldMp3Url.isEmpty() ? Uri.parse(oldMp3Url) : null;
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Glide.with(this).load(oldImageUrl).error(R.drawable.baseline_album_64).into(albumImageView);
        } else {
            albumImageView.setImageResource(R.drawable.baseline_album_64);
        }

        // MediaPlayer inicializálása
        initializeMediaPlayer();

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
            Log.e("EditMusicActivity", "Error launching MP3 picker", e);
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
            Log.e("EditMusicActivity", "Error launching image picker", e);
            Toast.makeText(this, "No app available to select image", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeMediaPlayer() {
        if (mp3Uri == null) {
            Log.w("EditMusicActivity", "No MP3 URI available for MediaPlayer initialization");
            playPauseButton.setEnabled(false);
            return;
        }
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
            Log.d("EditMusicActivity", "MediaPlayer initialized with MP3: " + mp3Uri.toString());
        } catch (IOException e) {
            Log.e("EditMusicActivity", "Error loading MP3: " + mp3Uri.toString(), e);
            Toast.makeText(this, "Error loading MP3: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            playPauseButton.setEnabled(false);
        }
    }

    private void playPauseMp3() {
        if (mediaPlayer == null || mp3Uri == null) {
            Log.w("EditMusicActivity", "MediaPlayer or MP3 URI is null");
            Toast.makeText(this, "Please select an MP3 first", Toast.LENGTH_SHORT).show();
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
                handler.postDelayed(updateSeekBar, 100);
                sendNotification("Playing Preview", "Previewing " + titleInput.getText().toString());
                Log.d("EditMusicActivity", "Playing MP3: " + mp3Uri.toString());
            } catch (IllegalStateException e) {
                Log.e("EditMusicActivity", "Error playing MP3", e);
                Toast.makeText(this, "Error playing MP3", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isUriValid(Uri uri) {
        if (uri == null) {
            Log.w("EditMusicActivity", "URI is null");
            return false;
        }
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            if (stream != null) {
                stream.close();
                Log.d("EditMusicActivity", "URI is valid: " + uri.toString());
                return true;
            } else {
                Log.w("EditMusicActivity", "Failed to open input stream for URI: " + uri.toString());
                return false;
            }
        } catch (IOException e) {
            Log.e("EditMusicActivity", "Error validating URI: " + uri.toString(), e);
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
                Log.d("EditMusicActivity", "File size for URI " + uri.toString() + ": " + size + " bytes");
                return size;
            } else {
                Log.w("EditMusicActivity", "Cursor is null for URI: " + uri.toString());
                return -1;
            }
        } catch (Exception e) {
            Log.e("EditMusicActivity", "Error getting file size for URI: " + uri.toString(), e);
            return -1;
        }
    }

    private void saveSong() {
        String title = titleInput.getText().toString().trim();
        String artist = artistInput.getText().toString().trim();
        String genre = genreInput.getText().toString().trim();
        String album = albumInput.getText().toString().trim();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.e("EditMusicActivity", "User is not logged in");
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || artist.isEmpty()) {
            Log.w("EditMusicActivity", "Required fields are empty");
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mp3Uri == null || !isUriValid(mp3Uri)) {
            Log.e("EditMusicActivity", "Invalid MP3 URI: " + (mp3Uri != null ? mp3Uri.toString() : "null"));
            Toast.makeText(this, "Invalid or missing MP3 file", Toast.LENGTH_LONG).show();
            return;
        }

        long fileSize = getFileSize(mp3Uri);
        if (fileSize > 10 * 1024 * 1024 || fileSize == -1) {
            Log.e("EditMusicActivity", "MP3 file size invalid: " + fileSize + " bytes");
            Toast.makeText(this, "MP3 file is too large or invalid (max 10 MB)", Toast.LENGTH_LONG).show();
            return;
        }

        if (imageUri != null && !isUriValid(imageUri)) {
            Log.e("EditMusicActivity", "Invalid image URI: " + imageUri.toString());
            Toast.makeText(this, "Invalid image file selected", Toast.LENGTH_LONG).show();
            return;
        }

        int duration = (mediaPlayer != null) ? mediaPlayer.getDuration() / 1000 : 0;
        if (duration <= 0 || duration > 3600) {
            Log.e("EditMusicActivity", "Invalid duration: " + duration + " seconds");
            Toast.makeText(this, "Invalid song duration (max 1 hour)", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("EditMusicActivity", "Raw duration: " + mediaPlayer.getDuration() + " ms, converted: " + duration + " s");

        saveButton.setEnabled(false);
        String songId = song.getId();
        Log.d("EditMusicActivity", "Processing song with ID: " + songId);
        StorageReference mp3Ref = storage.getReference().child("songs/" + songId + "_" + System.currentTimeMillis() + ".mp3");
        StorageReference imageRef = imageUri != null && isUriValid(imageUri) ?
                storage.getReference().child("images/" + songId + "_" + System.currentTimeMillis() + ".jpg") : null;

        StorageMetadata mp3Metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

        // MP3 feltöltés
        Task<String> mp3UploadTask;
        boolean isNewMp3 = mp3Uri != null && (oldMp3Url == null || !mp3Uri.toString().equals(oldMp3Url));
        Log.d("EditMusicActivity", "isNewMp3: " + isNewMp3 + ", mp3Uri: " + (mp3Uri != null ? mp3Uri.toString() : "null") + ", oldMp3Url: " + (oldMp3Url != null ? oldMp3Url : "null"));
        if (isNewMp3) {
            Log.d("EditMusicActivity", "Uploading new MP3 to: " + mp3Ref.getPath());
            mp3UploadTask = mp3Ref.putFile(mp3Uri, mp3Metadata).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("EditMusicActivity", "MP3 upload progress: " + progress + "%");
            }).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Log.e("EditMusicActivity", "MP3 upload failed: " + errorMsg, task.getException());
                    throw new Exception("MP3 upload failed: " + errorMsg, task.getException());
                }
                Log.d("EditMusicActivity", "MP3 uploaded successfully, fetching download URL for: " + mp3Ref.getPath());
                return retryGetDownloadUrl(mp3Ref, 5, 1000); // 5 próbálkozás
            });
        } else {
            Log.d("EditMusicActivity", "Keeping old MP3: " + (oldMp3Url != null ? oldMp3Url : "none"));
            if (oldMp3Url == null || oldMp3Url.isEmpty()) {
                Log.e("EditMusicActivity", "No valid old MP3 URL available");
                Toast.makeText(this, "No valid MP3 file to keep", Toast.LENGTH_LONG).show();
                saveButton.setEnabled(true);
                return;
            }
            mp3UploadTask = Tasks.forResult(oldMp3Url);
        }

        // Képfeltöltés
        Task<String> imageUploadTask;
        boolean isNewImage = imageUri != null && isUriValid(imageUri) && (oldImageUrl == null || !imageUri.toString().equals(oldImageUrl));
        Log.d("EditMusicActivity", "isNewImage: " + isNewImage + ", imageUri: " + (imageUri != null ? imageUri.toString() : "null") + ", oldImageUrl: " + (oldImageUrl != null ? oldImageUrl : "null"));
        if (isNewImage && imageRef != null) {
            Log.d("EditMusicActivity", "Uploading new image to: " + imageRef.getPath());
            imageUploadTask = imageRef.putFile(imageUri, new StorageMetadata.Builder().setContentType("image/jpeg").build())
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d("EditMusicActivity", "Image upload progress: " + progress + "%");
                    }).continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Log.e("EditMusicActivity", "Image upload failed: " + errorMsg, task.getException());
                            throw new Exception("Image upload failed: " + errorMsg, task.getException());
                        }
                        Log.d("EditMusicActivity", "Image uploaded successfully, fetching download URL for: " + imageRef.getPath());
                        return retryGetDownloadUrl(imageRef, 5, 1000); // 5 próbálkozás
                    });
        } else {
            Log.d("EditMusicActivity", "Keeping old image: " + (oldImageUrl != null ? oldImageUrl : "none"));
            imageUploadTask = Tasks.forResult(oldImageUrl != null ? oldImageUrl : "");
        }

        // Várjuk meg mindkét feladat befejezését
        Tasks.whenAllSuccess(mp3UploadTask, imageUploadTask).addOnSuccessListener(results -> {
            String mp3Url = (String) results.get(0);
            String imageUrl = (String) results.get(1);
            Log.d("EditMusicActivity", "MP3 URL: " + mp3Url + ", Image URL: " + (imageUrl != null ? imageUrl : "null"));

            // Régi fájlok törlése csak sikeres új URL-ek után
            if (isNewMp3 && oldMp3Url != null && !oldMp3Url.isEmpty()) {
                try {
                    storage.getReferenceFromUrl(oldMp3Url).delete()
                            .addOnSuccessListener(aVoid -> Log.d("EditMusicActivity", "Deleted old MP3: " + oldMp3Url))
                            .addOnFailureListener(e -> Log.w("EditMusicActivity", "Failed to delete old MP3: " + oldMp3Url, e));
                } catch (IllegalArgumentException e) {
                    Log.w("EditMusicActivity", "Invalid old MP3 URL: " + oldMp3Url, e);
                }
            }
            if (isNewImage && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    storage.getReferenceFromUrl(oldImageUrl).delete()
                            .addOnSuccessListener(aVoid -> Log.d("EditMusicActivity", "Deleted old image: " + oldImageUrl))
                            .addOnFailureListener(e -> Log.w("EditMusicActivity", "Failed to delete old image: " + oldImageUrl, e));
                } catch (IllegalArgumentException e) {
                    Log.w("EditMusicActivity", "Invalid old image URL: " + oldImageUrl, e);
                }
            }

            saveSongToFirestore(songId, title, artist, genre, album, duration, mp3Url, imageUrl, user.getUid());
        }).addOnFailureListener(e -> {
            Log.e("EditMusicActivity", "Failed to upload files: " + e.getMessage(), e);
            String toastMsg = "Failed to upload files: " + e.getMessage();
            if (e.getMessage().contains("PERMISSION_DENIED")) {
                toastMsg = "Permission denied. Check Firebase Storage rules or authentication.";
            } else if (e.getMessage().contains("Object does not exist")) {
                toastMsg = "File upload succeeded but URL retrieval failed. Keeping old files.";
            } else if (e.getMessage().contains("Image upload failed")) {
                toastMsg = "Failed to upload image. Keeping old image.";
            } else if (e.getMessage().contains("MP3 upload failed")) {
                toastMsg = "Failed to upload MP3. Keeping old MP3.";
            }
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            saveButton.setEnabled(true);
            // Hiba esetén a régi URL-ekkel mentjük a Firestore-t
            saveSongToFirestore(songId, title, artist, genre, album, duration, oldMp3Url, oldImageUrl, user.getUid());
        });
    }

    private Task<String> retryGetDownloadUrl(StorageReference ref, int maxAttempts, long delayMs) {
        return ref.getDownloadUrl().continueWithTask(task -> {
            if (task.isSuccessful()) {
                Log.d("EditMusicActivity", "Download URL retrieved: " + task.getResult().toString());
                return Tasks.forResult(task.getResult().toString());
            }
            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
            Log.w("EditMusicActivity", "Attempt to get download URL failed: " + errorMsg);
            if (maxAttempts <= 1) {
                Log.e("EditMusicActivity", "Max attempts reached for getDownloadUrl: " + ref.getPath());
                throw task.getException();
            }
            Log.d("EditMusicActivity", "Retrying getDownloadUrl for " + ref.getPath() + ", attempts left: " + (maxAttempts - 1));
            return Tasks.call(() -> {
                Thread.sleep(delayMs);
                return null;
            }).continueWithTask(ignored -> retryGetDownloadUrl(ref, maxAttempts - 1, delayMs * 2));
        });
    }

    private void saveSongToFirestore(String id, String title, String artist, String genre, String album, int duration, String mp3Url, String imageUrl, String userId) {
        Music updatedSong = new Music(id, title, artist, genre, album, duration, mp3Url, imageUrl, song.isLikeolt(), userId);
        Log.d("EditMusicActivity", "Saving song to Firestore with duration: " + duration + " s, mp3Url: " + mp3Url + ", albumArtUri: " + (imageUrl != null ? imageUrl : "null"));
        db.collection("songs").document(id).set(updatedSong)
                .addOnSuccessListener(aVoid -> {
                    Log.d("EditMusicActivity", "Song updated successfully: " + id);
                    sendNotification("Song Updated", title + " by " + artist + " has been updated!");
                    Toast.makeText(this, "Song updated successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updatedSong", new Gson().toJson(updatedSong));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("EditMusicActivity", "Failed to save song: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to save song: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                });
    }

    private void sendNotification(String title, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("EditMusicActivity", "Notification permission not granted");
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
        Log.d("EditMusicActivity", "Notification sent: " + title + " - " + message);
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