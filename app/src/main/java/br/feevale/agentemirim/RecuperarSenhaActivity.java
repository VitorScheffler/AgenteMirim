package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperarSenhaActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private MaterialButton btnConfirmar;
    private MaterialButton btnVoltarLogin;
    private ProgressBar progressBar;
    private LinearLayout layoutSucesso;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);

        auth = FirebaseAuth.getInstance();

        editEmail = findViewById(R.id.editEmail);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        btnVoltarLogin = findViewById(R.id.btnVoltarLogin);
        progressBar = findViewById(R.id.progressBar);
        layoutSucesso = findViewById(R.id.layoutSucesso);

        btnConfirmar.setOnClickListener(v -> enviarEmail());

        btnVoltarLogin.setOnClickListener(v -> {

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();

        });
    }

    private void enviarEmail() {

        String email = editEmail.getText().toString().trim();

        if(TextUtils.isEmpty(email)){
            editEmail.setError("Digite seu e-mail");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    progressBar.setVisibility(View.GONE);

                    if(task.isSuccessful()){

                        layoutSucesso.setVisibility(View.VISIBLE);
                        btnConfirmar.setVisibility(View.GONE);
                        editEmail.setEnabled(false);

                    } else {

                        Toast.makeText(this,
                                "Erro ao enviar e-mail",
                                Toast.LENGTH_LONG).show();
                    }

                });
    }
}