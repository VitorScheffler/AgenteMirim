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
    private LinearLayout      btnPerfilUsuario, btnPerfilAdmin;
    private TextView          txtPerfilUsuario, txtPerfilAdmin;
    private MaterialButton    btnCriar;
    private ProgressBar       progressBar;

    private String perfilSelecionado = "usuario"; // padrão

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
        btnPerfilAdmin   = findViewById(R.id.btnPerfilAdmin);
        txtPerfilUsuario = findViewById(R.id.txtPerfilUsuario);
        txtPerfilAdmin   = findViewById(R.id.txtPerfilAdmin);
        btnCriar         = findViewById(R.id.btnCriar);
        progressBar      = findViewById(R.id.progressBar);
    }

    // ── Seletor de perfil ─────────────────────────────────────────────────────

    private void configurarSeletorPerfil() {
        selecionarPerfil("usuario"); // começa selecionado
        btnPerfilUsuario.setOnClickListener(v -> selecionarPerfil("usuario"));
        btnPerfilAdmin.setOnClickListener(v -> selecionarPerfil("admin"));
    }

    private void selecionarPerfil(String perfil) {
        perfilSelecionado = perfil;

        if ("usuario".equals(perfil)) {
            btnPerfilUsuario.setBackgroundColor(0xFF2E7D32);
            txtPerfilUsuario.setTextColor(0xFFFFFFFF);
            btnPerfilAdmin.setBackgroundColor(0xFFE8F5E9);
            txtPerfilAdmin.setTextColor(0xFF2E7D32);
        } else {
            btnPerfilAdmin.setBackgroundColor(0xFF2E7D32);
            txtPerfilAdmin.setTextColor(0xFFFFFFFF);
            btnPerfilUsuario.setBackgroundColor(0xFFE8F5E9);
            txtPerfilUsuario.setTextColor(0xFF2E7D32);
        }
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

        /*
         * Usa uma instância SECUNDÁRIA do Firebase para criar o usuário.
         * Sem isso, o createUserWithEmailAndPassword() deslogaria o admin,
         * pois o Firebase loga automaticamente o usuário recém-criado.
         */
        FirebaseApp appSecundario = obterAppSecundario();
        FirebaseAuth authSecundario = FirebaseAuth.getInstance(appSecundario);

        authSecundario.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) { setCarregando(false); return; }

                    String novoUid = result.getUser().getUid();

                    // Salva no Firestore vinculado ao UID do novo usuário
                    Map<String, Object> dados = new HashMap<>();
                    dados.put("nome",           nome);
                    dados.put("email",          email);
                    dados.put("perfil",         perfilSelecionado);
                    dados.put("perfilCompleto", false); // obriga a completar no 1º acesso

                    db.collection("usuarios").document(novoUid)
                            .set(dados)
                            .addOnSuccessListener(v -> {
                                authSecundario.signOut(); // desloga instância secundária
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

    /**
     * Retorna (ou cria) uma instância secundária do FirebaseApp.
     * Isso permite criar usuários sem deslogar o admin logado na instância principal.
     */
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
        new AlertDialog.Builder(this)
                .setTitle("✅ Usuário criado!")
                .setMessage("Nome: " + nome
                        + "\nE-mail: " + email
                        + "\nPerfil: " + perfilSelecionado.toUpperCase()
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