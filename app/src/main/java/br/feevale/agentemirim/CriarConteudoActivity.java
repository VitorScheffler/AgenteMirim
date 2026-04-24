package br.feevale.agentemirim;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.view.MotionEvent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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

    private TextInputLayout layoutTitulo, layoutDescricao;
    private TextInputEditText editTitulo, editDescricao;
    private MaterialButton btnSalvar;
    private ProgressBar progressBar;

    private Spinner spinnerCor, spinnerIcone;

    private final int[] cores = {
            0xFFE87722, 0xFF4CAF50, 0xFFF9A825, 0xFFE53935
    };

    private final int[] icones = {
        android.R.drawable.ic_menu_sort_by_size,
        android.R.drawable.ic_menu_send,
        android.R.drawable.ic_menu_help,
        android.R.drawable.ic_menu_myplaces
    };

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_conteudo);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutTitulo    = findViewById(R.id.layoutTitulo);
        layoutDescricao = findViewById(R.id.layoutDescricao);
        editTitulo      = findViewById(R.id.editTitulo);
        editDescricao   = findViewById(R.id.editDescricao);
        btnSalvar       = findViewById(R.id.btnSalvar);
        progressBar     = findViewById(R.id.progressBar);

        spinnerCor   = findViewById(R.id.spinnerCor);
        spinnerIcone = findViewById(R.id.spinnerIcone);

        String[] nomesCores = {"Selecione a cor", "Laranja", "Verde", "Amarelo", "Vermelho"};
        String[] nomesIcones = {"Selecione o ícone", "Organizar", "Financeiro", "Estudo", "Saúde"};

        ArrayAdapter<String> adapterCores = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                nomesCores
        );

        ArrayAdapter<String> adapterIcones = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                nomesIcones
        );

        spinnerCor.setAdapter(adapterCores);
        spinnerIcone.setAdapter(adapterIcones);

        String[] opcoesCores = {"Laranja", "Verde", "Amarelo", "Vermelho"};
        String[] opcoesIcones = {"Organizar", "Financeiro", "Estudo", "Saúde"};

        spinnerCor.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                new AlertDialog.Builder(this)
                        .setTitle("Selecione a cor")
                        .setItems(opcoesCores, (dialog, which) -> spinnerCor.setSelection(which + 1))
                        .show();
            }
            return true;
        });

        spinnerIcone.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                new AlertDialog.Builder(this)
                        .setTitle("Selecione o ícone")
                        .setItems(opcoesIcones, (dialog, which) -> spinnerIcone.setSelection(which + 1))
                        .show();
            }
            return true;
        });

        btnSalvar.setOnClickListener(v -> salvarConteudo());
    }

    private void salvarConteudo() {
        layoutTitulo.setError(null);
        layoutDescricao.setError(null);

        String titulo = editTitulo.getText() != null ? editTitulo.getText().toString().trim() : "";
        String descricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo)) {
            layoutTitulo.setError("Informe o título");
            return;
        }

        if (TextUtils.isEmpty(descricao)) {
            layoutDescricao.setError("Informe a descrição");
            return;
        }

        int posCor = spinnerCor.getSelectedItemPosition();
        int posIcone = spinnerIcone.getSelectedItemPosition();

        if (posCor == 0) {
            Toast.makeText(this, "Selecione uma cor", Toast.LENGTH_SHORT).show();
            spinnerCor.requestFocus();
            return;
        }

        if (posIcone == 0) {
            Toast.makeText(this, "Selecione um ícone", Toast.LENGTH_SHORT).show();
            spinnerIcone.requestFocus();
            return;
        }

        int corSelecionada = cores[posCor - 1];
        int iconeSelecionado = icones[posIcone - 1];

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setCarregando(true);

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

                    salvarNovoConteudo(titulo, descricao, user.getUid(), proximaOrdem, corSelecionada, iconeSelecionado);
                })
                .addOnFailureListener(e -> {
                    // fallback (primeiro item)
                    salvarNovoConteudo(titulo, descricao, user.getUid(), 1, corSelecionada, iconeSelecionado);
                });
    }

    private void salvarNovoConteudo(String titulo, String descricao, String uid, long ordem, int cor, int icone){
        String dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date());

        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo", titulo);
        dados.put("descricao", descricao);
        dados.put("data", dataAtual);
        dados.put("ordem", ordem);
        dados.put("criadoPor", uid);
        dados.put("cor", cor);
        dados.put("icone", icone);

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
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!c);
        btnSalvar.setText(c ? "Salvando..." : "PUBLICAR CONTEÚDO");
    }
}
