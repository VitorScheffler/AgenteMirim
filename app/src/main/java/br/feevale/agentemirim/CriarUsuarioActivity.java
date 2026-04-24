package br.feevale.agentemirim;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CriarUsuarioActivity extends AppCompatActivity {

    private TextInputLayout   layoutNome, layoutEmail, layoutSenha;
    private TextInputEditText editNome, editEmail, editSenha;
    private LinearLayout      btnPerfilUsuario, btnPerfilProjeto, btnPerfilAdmin;
    private TextView          txtPerfilUsuario, txtPerfilProjeto, txtPerfilAdmin;
    private MaterialButton    btnCriar;
    private ProgressBar       progressBar;

    private String perfilSelecionado = "usuario";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_usuario);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        configurarSeletorPerfil();

        btnCriar.setOnClickListener(v -> criarUsuario());
    }

    private void bindViews() {
        layoutNome       = findViewById(R.id.layoutNome);
        layoutEmail      = findViewById(R.id.layoutEmail);
        layoutSenha      = findViewById(R.id.layoutSenha);
        editNome         = findViewById(R.id.editNome);
        editEmail        = findViewById(R.id.editEmail);
        editSenha        = findViewById(R.id.editSenha);
        btnPerfilUsuario = findViewById(R.id.btnPerfilUsuario);
        btnPerfilProjeto = findViewById(R.id.btnPerfilProjeto);
        btnPerfilAdmin   = findViewById(R.id.btnPerfilAdmin);
        txtPerfilUsuario = findViewById(R.id.txtPerfilUsuario);
        txtPerfilProjeto = findViewById(R.id.txtPerfilProjeto);
        txtPerfilAdmin   = findViewById(R.id.txtPerfilAdmin);
        btnCriar         = findViewById(R.id.btnCriar);
        progressBar      = findViewById(R.id.progressBar);
    }

    // ── Seletor de perfil ─────────────────────────────────────────────────────

    private void configurarSeletorPerfil() {
        selecionarPerfil("usuario");
        btnPerfilUsuario.setOnClickListener(v -> selecionarPerfil("usuario"));
        btnPerfilProjeto.setOnClickListener(v -> selecionarPerfil("projeto"));
        btnPerfilAdmin.setOnClickListener(v -> selecionarPerfil("admin"));
    }

    private void selecionarPerfil(String perfil) {
        perfilSelecionado = perfil;

        // Reset todos
        int corAtivo   = 0xFF2E7D32;
        int corInativo = 0xFFE8F5E9;
        int textoAtivo = 0xFFFFFFFF;
        int textoInativo = 0xFF2E7D32;

        btnPerfilUsuario.setBackgroundColor("usuario".equals(perfil) ? corAtivo : corInativo);
        txtPerfilUsuario.setTextColor("usuario".equals(perfil) ? textoAtivo : textoInativo);

        btnPerfilProjeto.setBackgroundColor("projeto".equals(perfil) ? corAtivo : corInativo);
        txtPerfilProjeto.setTextColor("projeto".equals(perfil) ? textoAtivo : textoInativo);

        btnPerfilAdmin.setBackgroundColor("admin".equals(perfil) ? corAtivo : corInativo);
        txtPerfilAdmin.setTextColor("admin".equals(perfil) ? textoAtivo : textoInativo);
    }

    // ── Criar usuário ─────────────────────────────────────────────────────────

    private void criarUsuario() {
        layoutNome.setError(null);
        layoutEmail.setError(null);
        layoutSenha.setError(null);

        String nome  = editNome.getText()  != null ? editNome.getText().toString().trim()  : "";
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String senha = editSenha.getText() != null ? editSenha.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nome)) {
            layoutNome.setError("Informe o nome"); return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Informe um e-mail válido"); return;
        }
        if (TextUtils.isEmpty(senha) || senha.length() < 6) {
            layoutSenha.setError("Mínimo 6 caracteres"); return;
        }

        setCarregando(true);

        FirebaseApp appSecundario = obterAppSecundario();
        FirebaseAuth authSecundario = FirebaseAuth.getInstance(appSecundario);

        authSecundario.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) { setCarregando(false); return; }

                    String novoUid = result.getUser().getUid();

                    Map<String, Object> dados = new HashMap<>();
                    dados.put("nome",           nome);
                    dados.put("email",          email);
                    dados.put("perfil",         perfilSelecionado);
                    dados.put("perfilCompleto", false);

                    db.collection("usuarios").document(novoUid)
                            .set(dados)
                            .addOnSuccessListener(v -> {
                                authSecundario.signOut();
                                setCarregando(false);
                                mostrarSucesso(nome, email);
                            })
                            .addOnFailureListener(e -> {
                                setCarregando(false);
                                layoutEmail.setError("Erro ao salvar dados: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    String mensagem = e.getMessage() != null && e.getMessage().contains("already in use")
                            ? "Este e-mail já está cadastrado"
                            : "Erro: " + e.getMessage();
                    layoutEmail.setError(mensagem);
                });
    }

    private FirebaseApp obterAppSecundario() {
        final String NOME = "AppSecundario";
        for (FirebaseApp app : FirebaseApp.getApps(this)) {
            if (NOME.equals(app.getName())) return app;
        }
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        return FirebaseApp.initializeApp(this, options, NOME);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mostrarSucesso(String nome, String email) {
        String labelPerfil = perfilSelecionado.equals("projeto") ? "PROJETO"
                : perfilSelecionado.toUpperCase();

        new AlertDialog.Builder(this)
                .setTitle("✅ Usuário criado!")
                .setMessage("Nome: " + nome
                        + "\nE-mail: " + email
                        + "\nPerfil: " + labelPerfil
                        + "\n\nO usuário deverá completar seus dados no primeiro acesso.")
                .setPositiveButton("OK", (d, w) -> {
                    editNome.setText("");
                    editEmail.setText("");
                    editSenha.setText("");
                    selecionarPerfil("usuario");
                })
                .show();
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnCriar.setEnabled(!c);
        btnCriar.setText(c ? "Criando..." : "CRIAR USUÁRIO");
    }
}
