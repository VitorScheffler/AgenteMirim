package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout   layoutEmail, layoutSenha;
    private TextInputEditText editEmail, editSenha;
    private MaterialButton    btnLogin;
    private TextView          txtEsqueciSenha;
    private ProgressBar       progressBar;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        layoutEmail     = findViewById(R.id.layoutEmail);
        layoutSenha     = findViewById(R.id.layoutSenha);
        editEmail       = findViewById(R.id.editEmail);
        editSenha       = findViewById(R.id.editSenha);
        btnLogin        = findViewById(R.id.btnLogin);
        progressBar     = findViewById(R.id.progressBar);
        txtEsqueciSenha = findViewById(R.id.txtEsqueciSenha);

        btnLogin.setOnClickListener(v -> login());
        txtEsqueciSenha.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarSenhaActivity.class)));

        // Bloqueia gesto de voltar na tela de login
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // bloqueado
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            verificarPerfil(usuario);
        }
    }

    private void login() {
        layoutEmail.setError(null);
        layoutSenha.setError(null);

        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String senha = editSenha.getText() != null ? editSenha.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) { layoutEmail.setError("Informe o e-mail"); return; }
        if (TextUtils.isEmpty(senha)) { layoutSenha.setError("Informe a senha");  return; }

        setCarregando(true);

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> verificarPerfil(result.getUser()))
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutSenha.setError("E-mail ou senha incorretos");
                    editSenha.setText("");
                });
    }

    /**
     * Consulta Firestore para verificar se o usuário já completou o perfil.
     * - perfilCompleto = true  → vai para MainActivity
     * - perfilCompleto = false → vai para CompletarPerfilActivity (primeiro acesso)
     */
    private void verificarPerfil(FirebaseUser user) {
        if (user == null) { setCarregando(false); return; }

        setCarregando(true);

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    setCarregando(false);
                    boolean completo = doc.exists()
                            && Boolean.TRUE.equals(doc.getBoolean("perfilCompleto"));

                    irPara(completo ? MainActivity.class : CompletarPerfilActivity.class);
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    // Em caso de falha, redireciona para completar o perfil
                    irPara(CompletarPerfilActivity.class);
                });
    }

    private void irPara(Class<?> destino) {
        Intent intent = new Intent(this, destino);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setCarregando(boolean c) {
        btnLogin.setEnabled(!c);
        btnLogin.setText(c ? "Entrando..." : "ENTRAR");
        if (progressBar != null) progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }
}