package hu.szte.nyanusz.mobilalk.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.model.Users;

public class RegisterActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();
    private TextView username;
    private TextView email;
    private TextView password;
    private TextView passwordAgain;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private CollectionReference userDataCollection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if (secret_key != 55443123) {
            finish();
        }

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userDataCollection = firestore.collection("userData");

        username = findViewById(R.id.usernameEditText);
        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        passwordAgain = findViewById(R.id.passwordAgainEditText);
    }

    public void onRegister(View view) {
        String username = this.username.getText().toString();
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        String passwordAgain = this.passwordAgain.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || passwordAgain.isEmpty()) {
            Toast.makeText(this, "Minden mezőt ki kell tölteni!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(passwordAgain)) {
            Toast.makeText(this, "A két jelszó nem egyezik!", Toast.LENGTH_LONG).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(LOG_TAG, "createUserWithEmail:success");
                Objects.requireNonNull(auth.getCurrentUser()).updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(username).build());
                //userDataCollection.document(auth.getCurrentUser().getUid()).set(new Users(username));
                finish();
            } else {
                Log.d(LOG_TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(this, "Sikertelen regisztráció!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void backToLogin(View view) {
        finish();
    }
}