package br.feevale.agentemirim;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import br.feevale.agentemirim.api.ApiClient;

/**
 * Tela de criação de conteúdo com:
 *  - Título e descrição
 *  - Seleção de tipo (Dica/Vídeo/Notícia/Outro)
 *  - Upload de imagem de capa
 *  - Upload de arquivo (PDF, vídeo, imagem)
 *  - Vincula ao cidadeId passado via Intent
 *
 * Destino: app/src/main/java/br/feevale/agentemirim/CriarConteudoActivity.java
 */
public class CriarConteudoActivity extends AppCompatActivity {

    // ── Views: texto ──────────────────────────────────────────────────────────
    private TextInputLayout   layoutTitulo, layoutDescricao;
    private TextInputEditText editTitulo, editDescricao;

    // ── Views: tipo ───────────────────────────────────────────────────────────
    private LinearLayout btnTipoDica, btnTipoVideo, btnTipoNoticia, btnTipoOutro;

    // ── Views: capa ───────────────────────────────────────────────────────────
    private LinearLayout layoutUploadCapa;
    private ImageView    ivPreviewCapa;
    private TextView     txtNomeCapa;

    // ── Views: arquivo ────────────────────────────────────────────────────────
    private LinearLayout layoutUploadArquivo;
    private LinearLayout layoutArquivoSelecionado;

    // ── Views: geral ──────────────────────────────────────────────────────────
    private MaterialButton btnSalvar;
    private ProgressBar    progressBar;
    private TextView       txtProgresso;

    // ── Estado ────────────────────────────────────────────────────────────────
    private String tipoSelecionado = "dica";

    private Uri    capaUri  = null;
    private String capaNome = null;
    private String capaMime = null;

    private Uri    arquivoUri  = null;
    private String arquivoNome = null;
    private String arquivoMime = null;

    // ── Dados da cidade (recebidos via Intent) ────────────────────────────────
    private String cidadeId   = null;
    private String cidadeNome = null;

    private FirebaseFirestore db;

    // ── Launchers ─────────────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> capaPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            processarCapa(result.getData().getData());
                        }
                    });

    private final ActivityResultLauncher<Intent> arquivoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            processarArquivo(result.getData().getData());
                        }
                    });

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_conteudo);

        db = FirebaseFirestore.getInstance();

        cidadeId   = getIntent().getStringExtra("cidadeId");
        cidadeNome = getIntent().getStringExtra("cidadeNome");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        configurarTipos();

        layoutUploadCapa.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        layoutUploadArquivo.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        layoutUploadCapa.setOnClickListener(v -> abrirCapaPicker());
        layoutUploadArquivo.setOnClickListener(v -> abrirArquivoPicker());
        btnSalvar.setOnClickListener(v -> publicar());
    }

    // =========================================================================
    // BIND
    // =========================================================================

    private void bindViews() {
        layoutTitulo             = findViewById(R.id.layoutTitulo);
        layoutDescricao          = findViewById(R.id.layoutDescricao);
        editTitulo               = findViewById(R.id.editTitulo);
        editDescricao            = findViewById(R.id.editDescricao);
        btnTipoDica              = findViewById(R.id.btnTipoDica);
        btnTipoVideo             = findViewById(R.id.btnTipoVideo);
        btnTipoNoticia           = findViewById(R.id.btnTipoNoticia);
        btnTipoOutro             = findViewById(R.id.btnTipoOutro);
        layoutUploadCapa         = findViewById(R.id.layoutUploadCapa);
        ivPreviewCapa            = findViewById(R.id.ivPreviewCapa);
        txtNomeCapa              = findViewById(R.id.txtNomeCapa);
        layoutUploadArquivo      = findViewById(R.id.layoutUploadArquivo);
        layoutArquivoSelecionado = findViewById(R.id.layoutArquivosSelecionados);
        btnSalvar                = findViewById(R.id.btnSalvar);
        progressBar              = findViewById(R.id.progressBar);
        txtProgresso             = findViewById(R.id.txtProgresso);
    }

    // =========================================================================
    // TIPOS
    // =========================================================================

    private void configurarTipos() {
        selecionarTipo("dica");
        btnTipoDica.setOnClickListener(v    -> selecionarTipo("dica"));
        btnTipoVideo.setOnClickListener(v   -> selecionarTipo("video"));
        btnTipoNoticia.setOnClickListener(v -> selecionarTipo("noticia"));
        btnTipoOutro.setOnClickListener(v   -> selecionarTipo("outro"));
    }

    private void selecionarTipo(String tipo) {
        tipoSelecionado = tipo;
        atualizarVisualTipo(btnTipoDica,    "dica");
        atualizarVisualTipo(btnTipoVideo,   "video");
        atualizarVisualTipo(btnTipoNoticia, "noticia");
        atualizarVisualTipo(btnTipoOutro,   "outro");
    }

    private void atualizarVisualTipo(LinearLayout btn, String tipo) {
        boolean ativo = tipoSelecionado.equals(tipo);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(12));
        bg.setColor(ativo ? 0xFFE8F5E9 : 0xFFFAFAFA);
        bg.setStroke(ativo ? dp(2) : dp(1), ativo ? 0xFF2E7D32 : 0xFFE0E0E0);
        btn.setBackground(bg);

        if (btn.getChildCount() >= 1 && btn.getChildAt(0) instanceof ImageView) {
            ((ImageView) btn.getChildAt(0))
                    .setColorFilter(ativo ? 0xFF2E7D32 : 0xFF9E9E9E);
        }
        if (btn.getChildCount() >= 2 && btn.getChildAt(1) instanceof TextView) {
            TextView lbl = (TextView) btn.getChildAt(1);
            lbl.setTextColor(ativo ? 0xFF2E7D32 : 0xFF9E9E9E);
            lbl.setTypeface(null, ativo ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    // =========================================================================
    // CAPA
    // =========================================================================

    private void abrirCapaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        capaPickerLauncher.launch(Intent.createChooser(intent, "Selecionar imagem de capa"));
    }

    private void processarCapa(Uri uri) {
        capaUri  = uri;
        capaMime = getContentResolver().getType(uri);
        capaNome = resolverNome(uri);

        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            ivPreviewCapa.setImageBitmap(bmp);
            ivPreviewCapa.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {}

        txtNomeCapa.setText(capaNome);
        txtNomeCapa.setVisibility(View.VISIBLE);
        layoutUploadCapa.setVisibility(View.GONE);
    }

    // =========================================================================
    // ARQUIVO
    // =========================================================================

    private void abrirArquivoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "video/mp4", "application/pdf"
        });
        arquivoPickerLauncher.launch(Intent.createChooser(intent, "Selecionar arquivo"));
    }

    private void processarArquivo(Uri uri) {
        arquivoUri  = uri;
        arquivoMime = getContentResolver().getType(uri);
        arquivoNome = resolverNome(uri);

        layoutArquivoSelecionado.removeAllViews();
        layoutArquivoSelecionado.setVisibility(View.VISIBLE);
        adicionarItemArquivo(arquivoNome, arquivoMime);
        layoutUploadArquivo.setVisibility(View.GONE);
    }

    private void adicionarItemArquivo(String nome, String mime) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setShape(GradientDrawable.RECTANGLE);
        rowBg.setCornerRadius(dp(10));
        rowBg.setColor(0xFFF1F8F1);
        row.setBackground(rowBg);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        // Chip tipo
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(dp(40), dp(22));
        chipLp.rightMargin = dp(10);
        chip.setLayoutParams(chipLp);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setText(chipLabel(mime));
        chip.setTextSize(9f);
        chip.setTextColor(Color.WHITE);
        chip.setTypeface(null, Typeface.BOLD);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(dp(4));
        chipBg.setColor(corChip(mime));
        chip.setBackground(chipBg);
        row.addView(chip);

        // Nome
        TextView txtNome = new TextView(this);
        txtNome.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        txtNome.setText(nome);
        txtNome.setTextSize(13f);
        txtNome.setTextColor(0xFF1A1A1A);
        txtNome.setMaxLines(1);
        txtNome.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        row.addView(txtNome);

        // Remover
        TextView btnRemover = new TextView(this);
        btnRemover.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        btnRemover.setGravity(android.view.Gravity.CENTER);
        btnRemover.setText("✕");
        btnRemover.setTextSize(14f);
        btnRemover.setTextColor(0xFFE53935);
        btnRemover.setTypeface(null, Typeface.BOLD);
        btnRemover.setOnClickListener(v -> {
            arquivoUri  = null;
            arquivoNome = null;
            arquivoMime = null;
            layoutArquivoSelecionado.removeAllViews();
            layoutArquivoSelecionado.setVisibility(View.GONE);
            layoutUploadArquivo.setVisibility(View.VISIBLE);
        });
        row.addView(btnRemover);

        layoutArquivoSelecionado.addView(row);
    }

    // =========================================================================
    // PUBLICAR
    // =========================================================================

    private void publicar() {
        layoutTitulo.setError(null);
        layoutDescricao.setError(null);

        String titulo    = editTitulo.getText()    != null ? editTitulo.getText().toString().trim()    : "";
        String descricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo))    { layoutTitulo.setError("Informe o título");     return; }
        if (TextUtils.isEmpty(descricao)) { layoutDescricao.setError("Informe a descrição"); return; }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setCarregando(true);

        // Fluxo: upload capa → upload arquivo → salva Firestore
        if (capaUri != null) {
            setStatus("Enviando imagem de capa...");
            ApiClient.getInstance().uploadArquivo(
                    this, capaUri, capaNome, capaMime,
                    new ApiClient.Callback<ApiClient.ArquivoApi>() {
                        @Override
                        public void onSucesso(ApiClient.ArquivoApi capa) {
                            runOnUiThread(() -> uploadArquivoSeHouver(
                                    titulo, descricao, user.getUid(), capa.getDownloadUrl()));
                        }
                        @Override
                        public void onErro(String msg) {
                            runOnUiThread(() -> {
                                setCarregando(false);
                                layoutTitulo.setError("Erro na capa: " + msg);
                            });
                        }
                    });
        } else {
            uploadArquivoSeHouver(titulo, descricao, user.getUid(), null);
        }
    }

    private void uploadArquivoSeHouver(String titulo, String descricao,
                                        String uid, String capaUrl) {
        if (arquivoUri != null) {
            setStatus("Enviando arquivo...");
            ApiClient.getInstance().uploadArquivo(
                    this, arquivoUri, arquivoNome, arquivoMime,
                    new ApiClient.Callback<ApiClient.ArquivoApi>() {
                        @Override
                        public void onSucesso(ApiClient.ArquivoApi arq) {
                            runOnUiThread(() -> {
                                setStatus("Salvando conteúdo...");
                                salvarFirestore(titulo, descricao, uid, capaUrl,
                                        arq.getDownloadUrl(), arq.id,
                                        arq.contentType, arq.filename);
                            });
                        }
                        @Override
                        public void onErro(String msg) {
                            runOnUiThread(() -> {
                                setCarregando(false);
                                layoutTitulo.setError("Erro no arquivo: " + msg);
                            });
                        }
                    });
        } else {
            setStatus("Salvando conteúdo...");
            salvarFirestore(titulo, descricao, uid, capaUrl,
                    null, null, null, null);
        }
    }

    // =========================================================================
    // FIRESTORE
    // =========================================================================

    private void salvarFirestore(String titulo, String descricao, String uid,
                                  String capaUrl,
                                  String arquivoUrl, String arquivoId,
                                  String arquivoTipo, String arquivoNomeOriginal) {
        db.collection("conteudos")
                .orderBy("ordem", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    long proximaOrdem = 1;
                    if (!query.isEmpty()) {
                        Long atual = query.getDocuments().get(0).getLong("ordem");
                        if (atual != null) proximaOrdem = atual + 1;
                    }

                    String dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date());

                    Map<String, Object> dados = new HashMap<>();
                    dados.put("titulo",    titulo);
                    dados.put("descricao", descricao);
                    dados.put("data",      dataAtual);
                    dados.put("ordem",     proximaOrdem);
                    dados.put("criadoPor", uid);
                    dados.put("categoria", tipoSelecionado);

                    // Cidade
                    if (cidadeId != null)   dados.put("cidadeId",   cidadeId);
                    if (cidadeNome != null) dados.put("cidadeNome", cidadeNome);

                    // Capa
                    if (capaUrl != null) dados.put("capaUrl", capaUrl);

                    // Arquivo
                    if (arquivoUrl != null) {
                        dados.put("temAnexo",    true);
                        dados.put("arquivoUrl",  arquivoUrl);
                        dados.put("arquivoId",   arquivoId);
                        dados.put("arquivoTipo", arquivoTipo);
                        dados.put("arquivoNome", arquivoNomeOriginal);
                    } else {
                        dados.put("temAnexo", false);
                    }

                    db.collection("conteudos").add(dados)
                            .addOnSuccessListener(ref -> {
                                // Incrementa qtdConteudos na cidade
                                if (cidadeId != null && !cidadeId.equals("todas")) {
                                    db.collection("cidades").document(cidadeId)
                                            .update("qtdConteudos",
                                                    com.google.firebase.firestore.FieldValue.increment(1));
                                }
                                setCarregando(false);
                                new AlertDialog.Builder(this)
                                        .setTitle("✅ Publicado!")
                                        .setMessage("\"" + titulo + "\" foi publicado com sucesso.")
                                        .setPositiveButton("OK", (d, w) -> finish())
                                        .setCancelable(false).show();
                            })
                            .addOnFailureListener(e -> {
                                setCarregando(false);
                                layoutTitulo.setError("Erro: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutTitulo.setError("Erro: " + e.getMessage());
                });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!c);
        btnSalvar.setText(c ? "Aguarde..." : "☁  Publicar conteúdo");
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
        if (nome == null) nome = "arquivo";
        return nome;
    }

    private String chipLabel(String mime) {
        if (mime == null) return "FILE";
        if (mime.equals("application/pdf")) return "PDF";
        if (mime.startsWith("video/"))       return "MP4";
        if (mime.equals("image/png"))        return "PNG";
        if (mime.equals("image/jpeg"))       return "JPG";
        return "FILE";
    }

    private int corChip(String mime) {
        if (mime == null) return 0xFF607D8B;
        if (mime.equals("application/pdf")) return 0xFFE53935;
        if (mime.startsWith("video/"))       return 0xFF1565C0;
        if (mime.startsWith("image/"))       return 0xFF2E7D32;
        return 0xFF607D8B;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
