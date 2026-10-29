package io.github.petsaudedigitalgat1.isttimer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import io.github.petsaudedigitalgat1.isttimer.R;

public class ConsentActivity extends AppCompatActivity {

    private static final String TAG = "ConsentActivity";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "ConsentActivity iniciada");
            
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean alreadyAccepted = preferences.getBoolean(KEY_TERMS_ACCEPTED, false);

            if (alreadyAccepted) {
                Log.d(TAG, "Termos já aceitos, redirecionando para MainActivity");
                openMainScreen();
                return;
            }

            setContentView(R.layout.activity_consent);

            TextView termsLinkText = findViewById(R.id.text_terms_link);
            Button declineButton = findViewById(R.id.button_decline);
            Button acceptButton = findViewById(R.id.button_accept);

            if (termsLinkText == null || declineButton == null || acceptButton == null) {
                Log.e(TAG, "Erro: Componentes não encontrados no layout");
                Toast.makeText(this, "Erro ao carregar a tela", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            termsLinkText.setText(HtmlCompat.fromHtml(getString(R.string.consent_description), HtmlCompat.FROM_HTML_MODE_LEGACY));
            termsLinkText.setMovementMethod(LinkMovementMethod.getInstance());

            declineButton.setOnClickListener(v -> {
                Log.d(TAG, "Usuário recusou os termos");
                finishAffinity();
            });

            acceptButton.setOnClickListener(v -> {
                Log.d(TAG, "Usuário aceitou os termos");
                preferences.edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply();
                openMainScreen();
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro no onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao iniciar aplicação", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openMainScreen() {
        try {
            Log.d(TAG, "Abrindo MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir MainActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao continuar", Toast.LENGTH_SHORT).show();
        }
    }
}
