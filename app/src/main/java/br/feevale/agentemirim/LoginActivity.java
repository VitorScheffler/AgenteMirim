package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutUsuario, layoutSenha;
    private TextInputEditText editUsuario, editSenha;

    private MaterialButton btnLogin;
    private TextView txtEsqueciSenha;

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        layoutUsuario = findViewById(R.id.layoutEmail);
        layoutSenha   = findViewById(R.id.layoutSenha);

        editUsuario   = findViewById(R.id.editEmail);
        editSenha     = findViewById(R.id.editSenha);

        btnLogin      = findViewById(R.id.btnLogin);
        progressBar   = findViewById(R.id.progressBar);

        txtEsqueciSenha = findViewById(R.id.txtEsqueciSenha);

        btnLogin.setOnClickListener(v -> login());

        txtEsqueciSenha.setOnClickListener(v -> {

            Intent intent = new Intent(LoginActivity.this, RecuperarSenhaActivity.class);
            startActivity(intent);

        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser usuarioAtual = mAuth.getCurrentUser();

        if (usuarioAtual != null) {
            irParaMain();
        }
    }

    private void login() {

        String email = editUsuario.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        layoutUsuario.setError(null);
        layoutSenha.setError(null);

        if (TextUtils.isEmpty(email)) {
            layoutUsuario.setError("Informe o e-mail");
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            layoutSenha.setError("Informe a senha");
            return;
        }

        setCarregando(true);

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {

                    setCarregando(false);

                    if (task.isSuccessful()) {

                        irParaMain();

                    } else {

                        layoutSenha.setError("E-mail ou senha incorretos");
                        editSenha.setText("");

                    }

                });
    }

    private void irParaMain() {

        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void setCarregando(boolean carregando) {

        btnLogin.setEnabled(!carregando);

        btnLogin.setText(
                carregando ? "Entrando..." : "ENTRAR"
        );

        if (progressBar != null) {

            progressBar.setVisibility(
                    carregando ? View.VISIBLE : View.GONE
            );

        }
    }

    @Override
    public void onBackPressed() {
        // bloqueia voltar
    }
}