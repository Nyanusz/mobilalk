package hu.szte.nyanusz.mobilalk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import hu.szte.nyanusz.mobilalk.R;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int SECRET_KEY = 55443123;

    private FirebaseAuth auth;

    EditText emailEditText;
    EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
    }

    @Override
    protected void onResume() {

           super.onResume();
           if (auth.getCurrentUser() != null) {
               Intent intent = new Intent(this, HomeActivity.class);
               startActivity(intent);
           }


    }

    public void onLogin(View view) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Az email cím és a jelszó mezők kitöltése kötelező!", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(LOG_TAG, "signInWithEmail:success");
                        Intent intent = new Intent(this, HomeActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Hibás email-cím vagy jelszó!", Toast.LENGTH_LONG).show();
                        Log.w(LOG_TAG, "signInWithEmail:failure", task.getException());
                    }
                });
    }

    public void onRegister(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);
    }

    public void onLoginAnonymous(View view) {
        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(LOG_TAG, "signInAnonymously:success");
                        Intent intent = new Intent(this, HomeActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Sikertelen bejelentkezés!", Toast.LENGTH_LONG).show();
                        Log.w(LOG_TAG, "signInAnonymously:failure", task.getException());
                    }
                });
    }

}