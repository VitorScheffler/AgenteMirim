package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutUsuario, layoutSenha;
    private TextInputEditText editUsuario, editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        layoutUsuario = findViewById(R.id.layoutUsuario);
        layoutSenha = findViewById(R.id.layoutSenha);
        editUsuario = findViewById(R.id.editUsuario);
        editSenha = findViewById(R.id.editSenha);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String usuario = editUsuario.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        layoutUsuario.setError(null);
        layoutSenha.setError(null);

        if (TextUtils.isEmpty(usuario)) {
            layoutUsuario.setError("Informe o usuário");
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            layoutSenha.setError("Informe a senha");
            return;
        }

        if (usuario.equals("admin") && senha.equals("admin")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            layoutUsuario.setError("Usuário ou senha incorretos");
            layoutSenha.setError("Usuário ou senha incorretos");
        }
    }

    @Override
    public void onBackPressed() {
        // bloqueia voltar na tela de login
    }
}