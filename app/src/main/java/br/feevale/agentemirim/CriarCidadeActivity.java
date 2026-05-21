package br.feevale.agentemirim;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import br.feevale.agentemirim.api.ApiClient;

/**
 * Tela para o admin criar/cadastrar uma nova cidade.
 * Salva no Firestore (coleção "cidades") com: nome, descricao, imagemUrl.
 * A imagem é enviada via ApiClient e a URL é salva no Firestore.
 *
 * Destino: app/src/main/java/br/feevale/agentemirim/CriarCidadeActivity.java
 */
public class CriarCidadeActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputLayout   layoutNome, layoutDescricao;
    private TextInputEditText editNome, editDescricao;
    private LinearLayout      layoutUploadImagem;
    private ImageView         ivPreviewImagem;
    private TextView          txtNomeImagem;
    private MaterialButton    btnSalvar;
    private ProgressBar       progressBar;
    private TextView          txtProgresso;

    // ── Estado ────────────────────────────────────────────────────────────────
    private Uri    imagemUri  = null;
    private String imagemNome = null;
    private String imagemMime = null;

    private FirebaseFirestore db;

    // ── File picker ───────────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            processarImagem(result.getData().getData());
                        }
                    });

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_cidade);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();

        layoutUploadImagem.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        layoutUploadImagem.setOnClickListener(v -> abrirImagePicker());
        btnSalvar.setOnClickListener(v -> salvar());
    }

    // =========================================================================
    // BIND
    // =========================================================================

    private void bindViews() {
        layoutNome        = findViewById(R.id.layoutNome);
        layoutDescricao   = findViewById(R.id.layoutDescricao);
        editNome          = findViewById(R.id.editNome);
        editDescricao     = findViewById(R.id.editDescricao);
        layoutUploadImagem= findViewById(R.id.layoutUploadImagem);
        ivPreviewImagem   = findViewById(R.id.ivPreviewImagem);
        txtNomeImagem     = findViewById(R.id.txtNomeImagem);
        btnSalvar         = findViewById(R.id.btnSalvar);
        progressBar       = findViewById(R.id.progressBar);
        txtProgresso      = findViewById(R.id.txtProgresso);
    }

    // =========================================================================
    // IMAGE PICKER
    // =========================================================================

    private void abrirImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePicker.launch(Intent.createChooser(intent, "Selecionar foto da cidade"));
    }

    private void processarImagem(Uri uri) {
        imagemUri  = uri;
        imagemMime = getContentResolver().getType(uri);
        imagemNome = resolverNome(uri);

        // Preview da imagem selecionada
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            ivPreviewImagem.setImageBitmap(bmp);
            ivPreviewImagem.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            ivPreviewImagem.setVisibility(View.GONE);
        }

        txtNomeImagem.setText(imagemNome);
        txtNomeImagem.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // SALVAR
    // =========================================================================

    private void salvar() {
        layoutNome.setError(null);
        layoutDescricao.setError(null);

        String nome      = editNome.getText()      != null ? editNome.getText().toString().trim()      : "";
        String descricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nome)) {
            layoutNome.setError("Informe o nome da cidade"); return;
        }
        if (TextUtils.isEmpty(descricao)) {
            layoutDescricao.setError("Informe uma descrição"); return;
        }

        setCarregando(true);

        if (imagemUri != null) {
            // Passo 1: envia imagem para a API
            setStatus("Enviando imagem...");
            ApiClient.getInstance().uploadArquivo(
                    this, imagemUri, imagemNome, imagemMime,
                    new ApiClient.Callback<ApiClient.ArquivoApi>() {
                        @Override
                        public void onSucesso(ApiClient.ArquivoApi arquivo) {
                            runOnUiThread(() -> {
                                setStatus("Salvando cidade...");
                                salvarFirestore(nome, descricao, arquivo.getDownloadUrl());
                            });
                        }
                        @Override
                        public void onErro(String msg) {
                            runOnUiThread(() -> {
                                setCarregando(false);
                                layoutNome.setError("Erro no upload da imagem: " + msg);
                            });
                        }
                    });
        } else {
            // Sem imagem
            setStatus("Salvando cidade...");
            salvarFirestore(nome, descricao, null);
        }
    }

    private void salvarFirestore(String nome, String descricao, String imagemUrl) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome",          nome);
        dados.put("descricao",     descricao);
        dados.put("qtdConteudos",  0L);
        dados.put("criadoPor",     FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "");
        if (imagemUrl != null) dados.put("imagemUrl", imagemUrl);

        db.collection("cidades").add(dados)
                .addOnSuccessListener(ref -> {
                    setCarregando(false);
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Cidade criada!")
                            .setMessage("\"" + nome + "\" foi adicionada com sucesso.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutNome.setError("Erro ao salvar: " + e.getMessage());
                });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!c);
        btnSalvar.setText(c ? "Aguarde..." : "SALVAR CIDADE");
        if (!c) txtProgresso.setVisibility(View.GONE);
    }

    private void setStatus(String msg) {
        txtProgresso.setText(msg);
        txtProgresso.setVisibility(View.VISIBLE);
    }

    private String resolverNome(Uri uri) {
        String nome = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) nome = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        if (nome == null) nome = uri.getLastPathSegment();
        if (nome == null) nome = "imagem.jpg";
        return nome;
    }
}
