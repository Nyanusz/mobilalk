package hu.szte.nyanusz.mobilalk.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.Objects;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.adapter.MusicAdapter;
import hu.szte.nyanusz.mobilalk.model.Music;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;

    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private CollectionReference musicCollection;
    private RecyclerView musicRecyclerView;
    private ArrayList<Music> musicItemsData;
    private MusicAdapter musicAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean backPressed = false;
    private String lastSearchText = "";

    private int limit = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        musicCollection = db.collection("zenek");

        musicRecyclerView = findViewById(R.id.musicRecyclerView);

        musicRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        musicItemsData = new ArrayList<>();

        musicAdapter = new MusicAdapter(this, musicItemsData);

        musicRecyclerView.setAdapter(musicAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::refreshItems);

        musicCollection.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w("Homeactivity", "Listen failed.", error);
                return;
            }
            if (value != null){
                swipeRefreshLayout.setRefreshing(true);
                refreshItems();
            }
        });

        musicRecyclerView.post(() -> {
            View item = LayoutInflater.from(this).inflate(R.layout.music_item, musicRecyclerView, false);
            item.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int itemHeight = item.getMeasuredHeight();

            int recyclerViewHeight = musicRecyclerView.getHeight();

            limit = recyclerViewHeight / itemHeight;

        });
        musicRecyclerView.setHasFixedSize(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        swipeRefreshLayout.setRefreshing(true);
        refreshItems();
    }

    public void refreshItems() {
        musicCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<Music> newMusicItemsData = new ArrayList<>();
                for (DocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                    Music music = document.toObject(Music.class);
                    assert music != null;
                    music.setFirebaseId(document.getId());

                    StorageReference albumArtRef = storage.getReference().child("albumArt/" + document.getId());
                    albumArtRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        music.setAlbumArtUri(uri);
                        int index = newMusicItemsData.indexOf(music);
                        if (index != -1) {
                            newMusicItemsData.set(index, music);
                            musicAdapter.notifyItemChanged(index);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("Homeactivity", "Error getting album art uri", e);
                    });

                    newMusicItemsData.add(music);
                }
                musicItemsData = newMusicItemsData;
                musicAdapter = new MusicAdapter(this, musicItemsData);
                if (!lastSearchText.isEmpty()) {
                    musicAdapter.getFilter().filter(lastSearchText);
                }
                musicRecyclerView.setAdapter(musicAdapter);
                swipeRefreshLayout.setRefreshing(false);

            } else {
                Log.d("Homeactivity", "Error getting documents: ", task.getException());
            }
        });
    }

    public void showMusicInfo(@NonNull Music music) {
        /*Intent intent = new Intent(this, HomeActivity.class);
        Gson gson = new GsonBuilder().registerTypeAdapter(Uri.class, new UriTypeAdapter()).create();
        intent.putExtra("music", gson.toJson(music));
        startActivity(intent);*/
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(!backPressed){
            backPressed = true;
            Toast.makeText(this, R.string.pressBackAgain, Toast.LENGTH_SHORT).show();
        } else {
            finishAffinity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (user != null && !user.isAnonymous()) {
            getMenuInflater().inflate(R.menu.default_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.guest_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.musicUpload) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout) {
            auth.signOut();
            finish();
            return true;
        } else if (item.getItemId() == R.id.search) {
            MaterialAlertDialogBuilder builder = getBuilder();
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private MaterialAlertDialogBuilder getBuilder() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Keresés címre vagy előadóra:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(lastSearchText);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String searchText = input.getText().toString();
            lastSearchText = searchText;
            musicAdapter.getFilter().filter(searchText);
        });
        builder.setNegativeButton("Mégse", (dialog, which) -> dialog.cancel());
        return builder;
    }
}