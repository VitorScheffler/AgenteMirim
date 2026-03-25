package br.feevale.agentemirim;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CriarConteudoActivity extends AppCompatActivity {

    private TextInputLayout   layoutTitulo, layoutDescricao;
    private TextInputEditText editTitulo, editDescricao;
    private MaterialButton    btnSalvar;
    private ProgressBar       progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_conteudo);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutTitulo    = findViewById(R.id.layoutTitulo);
        layoutDescricao = findViewById(R.id.layoutDescricao);
        editTitulo      = findViewById(R.id.editTitulo);
        editDescricao   = findViewById(R.id.editDescricao);
        btnSalvar       = findViewById(R.id.btnSalvar);
        progressBar     = findViewById(R.id.progressBar);

        btnSalvar.setOnClickListener(v -> salvarConteudo());
    }

    private void salvarConteudo() {
        layoutTitulo.setError(null);
        layoutDescricao.setError(null);

        String titulo    = editTitulo.getText()    != null ? editTitulo.getText().toString().trim()    : "";
        String descricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo)) {
            layoutTitulo.setError("Informe o título"); return;
        }
        if (TextUtils.isEmpty(descricao)) {
            layoutDescricao.setError("Informe a descrição"); return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setCarregando(true);

        // Busca a maior ordem existente para incrementar
        db.collection("conteudos")
                .orderBy("ordem", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    long proximaOrdem = 1;
                    if (!query.isEmpty()) {
                        Long ordemAtual = query.getDocuments().get(0).getLong("ordem");
                        if (ordemAtual != null) proximaOrdem = ordemAtual + 1;
                    }

                    String dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date());

                    Map<String, Object> dados = new HashMap<>();
                    dados.put("titulo",    titulo);
                    dados.put("descricao", descricao);
                    dados.put("data",      dataAtual);
                    dados.put("ordem",     proximaOrdem);
                    dados.put("criadoPor", user.getUid());

                    db.collection("conteudos")
                            .add(dados)
                            .addOnSuccessListener(ref -> {
                                setCarregando(false);
                                new AlertDialog.Builder(this)
                                        .setTitle("✅ Conteúdo criado!")
                                        .setMessage("\"" + titulo + "\" foi publicado com sucesso.")
                                        .setPositiveButton("OK", (d, w) -> finish())
                                        .setCancelable(false)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                setCarregando(false);
                                layoutTitulo.setError("Erro ao salvar: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutTitulo.setError("Erro ao verificar ordem: " + e.getMessage());
                });
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!c);
        btnSalvar.setText(c ? "Salvando..." : "PUBLICAR CONTEÚDO");
    }
}
