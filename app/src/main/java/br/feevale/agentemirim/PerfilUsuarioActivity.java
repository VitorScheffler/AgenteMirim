package br.feevale.agentemirim;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilUsuarioActivity extends AppCompatActivity {

    // Alterar e-mail
    private TextInputLayout   layoutNovoEmail, layoutSenhaConfirmEmail;
    private TextInputEditText editNovoEmail, editSenhaConfirmEmail;
    private MaterialButton    btnAlterarEmail;

    // Alterar senha
    private TextInputLayout   layoutSenhaAtual, layoutNovaSenha, layoutConfirmarSenha;
    private TextInputEditText editSenhaAtual, editNovaSenha, editConfirmarSenha;
    private TextView          txtForcaSenha;
    private View              barra1, barra2, barra3, barra4, barra5;
    private MaterialButton    btnAlterarSenha;

    // Geral
    private TextView    txtEmailAtual;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_usuario);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        exibirEmailAtual();
        configurarForcaSenha();
        configurarNavegacao();

        btnAlterarEmail.setOnClickListener(v -> alterarEmail());
        btnAlterarSenha.setOnClickListener(v -> alterarSenha());
    }

    private void configurarNavegacao() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_perfil);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new android.content.Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_conteudos) {
                startActivity(new android.content.Intent(this, ConteudosActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_perfil) {
                return true;
            }
            return false;
        });
    }

    private void bindViews() {
        txtEmailAtual           = findViewById(R.id.txtEmailAtual);
        progressBar             = findViewById(R.id.progressBar);

        layoutNovoEmail         = findViewById(R.id.layoutNovoEmail);
        editNovoEmail           = findViewById(R.id.editNovoEmail);
        layoutSenhaConfirmEmail = findViewById(R.id.layoutSenhaConfirmEmail);
        editSenhaConfirmEmail   = findViewById(R.id.editSenhaConfirmEmail);
        btnAlterarEmail         = findViewById(R.id.btnAlterarEmail);

        layoutSenhaAtual        = findViewById(R.id.layoutSenhaAtual);
        editSenhaAtual          = findViewById(R.id.editSenhaAtual);
        layoutNovaSenha         = findViewById(R.id.layoutNovaSenha);
        editNovaSenha           = findViewById(R.id.editNovaSenha);
        layoutConfirmarSenha    = findViewById(R.id.layoutConfirmarSenha);
        editConfirmarSenha      = findViewById(R.id.editConfirmarSenha);
        txtForcaSenha           = findViewById(R.id.txtForcaSenha);
        barra1 = findViewById(R.id.barra1);
        barra2 = findViewById(R.id.barra2);
        barra3 = findViewById(R.id.barra3);
        barra4 = findViewById(R.id.barra4);
        barra5 = findViewById(R.id.barra5);
        btnAlterarSenha         = findViewById(R.id.btnAlterarSenha);
    }

    private void exibirEmailAtual() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) txtEmailAtual.setText(user.getEmail());
    }

    // ── Alterar e-mail ────────────────────────────────────────────────────────

    private void alterarEmail() {
        layoutNovoEmail.setError(null);
        layoutSenhaConfirmEmail.setError(null);

        String novoEmail = editNovoEmail.getText() != null ? editNovoEmail.getText().toString().trim() : "";
        String senha     = editSenhaConfirmEmail.getText() != null ? editSenhaConfirmEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(novoEmail)) {
            layoutNovoEmail.setError("Informe o novo e-mail"); return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(novoEmail).matches()) {
            layoutNovoEmail.setError("E-mail inválido"); return;
        }
        if (TextUtils.isEmpty(senha)) {
            layoutSenhaConfirmEmail.setError("Informe sua senha atual"); return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        setCarregando(true);

        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), senha);
        user.reauthenticate(cred)
                .addOnSuccessListener(u ->
                        user.updateEmail(novoEmail)
                                .addOnSuccessListener(v -> {
                                    setCarregando(false);
                                    txtEmailAtual.setText(novoEmail);
                                    editNovoEmail.setText("");
                                    editSenhaConfirmEmail.setText("");
                                    mostrarSucesso("E-mail atualizado com sucesso!");
                                })
                                .addOnFailureListener(e -> {
                                    setCarregando(false);
                                    layoutNovoEmail.setError("Erro: " + e.getMessage());
                                })
                )
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutSenhaConfirmEmail.setError("Senha incorreta");
                });
    }

    // ── Alterar senha ─────────────────────────────────────────────────────────

    private void alterarSenha() {
        layoutSenhaAtual.setError(null);
        layoutNovaSenha.setError(null);
        layoutConfirmarSenha.setError(null);

        String senhaAtual = editSenhaAtual.getText()    != null ? editSenhaAtual.getText().toString().trim()    : "";
        String novaSenha  = editNovaSenha.getText()     != null ? editNovaSenha.getText().toString().trim()     : "";
        String confirmar  = editConfirmarSenha.getText()!= null ? editConfirmarSenha.getText().toString().trim(): "";

        if (TextUtils.isEmpty(senhaAtual)) {
            layoutSenhaAtual.setError("Informe a senha atual"); return;
        }
        if (TextUtils.isEmpty(novaSenha)) {
            layoutNovaSenha.setError("Informe a nova senha"); return;
        }
        if (novaSenha.length() < 6) {
            layoutNovaSenha.setError("Mínimo 6 caracteres"); return;
        }
        if (!novaSenha.equals(confirmar)) {
            layoutConfirmarSenha.setError("As senhas não coincidem"); return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        setCarregando(true);

        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), senhaAtual);
        user.reauthenticate(cred)
                .addOnSuccessListener(u ->
                        user.updatePassword(novaSenha)
                                .addOnSuccessListener(v -> {
                                    setCarregando(false);
                                    editSenhaAtual.setText("");
                                    editNovaSenha.setText("");
                                    editConfirmarSenha.setText("");
                                    txtForcaSenha.setText("");
                                    resetarBarras();
                                    mostrarSucesso("Senha alterada com sucesso!");
                                })
                                .addOnFailureListener(e -> {
                                    setCarregando(false);
                                    layoutNovaSenha.setError("Erro: " + e.getMessage());
                                })
                )
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutSenhaAtual.setError("Senha incorreta");
                });
    }

    // ── Força da senha ────────────────────────────────────────────────────────

    private void configurarForcaSenha() {
        editNovaSenha.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                atualizarForca(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void atualizarForca(String senha) {
        if (senha.isEmpty()) { txtForcaSenha.setText(""); resetarBarras(); return; }

        int pts = 0;
        if (senha.length() >= 8)                pts++;
        if (senha.length() >= 12)               pts++;
        if (senha.matches(".*[A-Z].*"))         pts++;
        if (senha.matches(".*[0-9].*"))         pts++;
        if (senha.matches(".*[^a-zA-Z0-9].*")) pts++;

        int cor;
        String label;
        if (pts <= 1)      { cor = 0xFFC62828; label = "🔴 Senha fraca"; }
        else if (pts <= 3) { cor = 0xFFF57F17; label = "🟡 Senha média"; }
        else               { cor = 0xFF2E7D32; label = "🟢 Senha forte"; }

        View[] barras = {barra1, barra2, barra3, barra4, barra5};
        for (int i = 0; i < barras.length; i++) {
            barras[i].setBackgroundColor(i < pts ? cor : 0xFFE0E0E0);
        }
        txtForcaSenha.setText(label);
        txtForcaSenha.setTextColor(cor);
    }

    private void resetarBarras() {
        int cinza = 0xFFE0E0E0;
        barra1.setBackgroundColor(cinza);
        barra2.setBackgroundColor(cinza);
        barra3.setBackgroundColor(cinza);
        barra4.setBackgroundColor(cinza);
        barra5.setBackgroundColor(cinza);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mostrarSucesso(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("✅ Sucesso")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnAlterarEmail.setEnabled(!c);
        btnAlterarSenha.setEnabled(!c);
    }
}