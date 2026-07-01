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

import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import br.feevale.agentemirim.api.ApiClient;

public class CriarConteudoActivity extends AppCompatActivity {

    private static final int MAX_ARQUIVOS = 5;

    // ── Categorias disponíveis ─────────────────────────────────────────────────
    private static final String CAT_ENCHENTE     = "enchente";
    private static final String CAT_DESLIZAMENTO = "deslizamento";
    private static final String CAT_TEMPESTADE   = "tempestade";
    private static final String CAT_OUTROS       = "outros";

    // ── Views: texto ──────────────────────────────────────────────────────────
    private TextInputLayout   layoutTitulo, layoutDescricao, layoutTexto;
    private TextInputEditText editTitulo, editDescricao, editTexto;

    // ── Views: tipo ───────────────────────────────────────────────────────────
    private LinearLayout btnTipoDica, btnTipoVideo, btnTipoNoticia, btnTipoMaterial;

    // ── Views: categorias ─────────────────────────────────────────────────────
    private LinearLayout chipEnchente, chipDeslizamento, chipTempestade, chipOutros;
    private TextView     lblEnchente, lblDeslizamento, lblTempestade, lblOutros;
    private TextView     txtCategoriasErro;

    // ── Views: capa ───────────────────────────────────────────────────────────
    private LinearLayout layoutUploadCapa;
    private View         cardPreviewCapa;
    private ImageView    ivPreviewCapa;
    private TextView     btnRemoverCapa;

    // ── Views: arquivos ───────────────────────────────────────────────────────
    private LinearLayout layoutUploadArquivo;
    private LinearLayout layoutArquivosSelecionados;
    private TextView     txtContadorArquivos;

    // ── Views: geral ──────────────────────────────────────────────────────────
    private MaterialButton btnSalvar;
    private ProgressBar    progressBar;
    private TextView       txtProgresso;

    // ── Estado ────────────────────────────────────────────────────────────────
    private String       tipoSelecionado  = "dica";
    private Set<String>  categoriasSelecionadas = new HashSet<>();

    // Capa
    private Uri    capaUri  = null;
    private String capaNome = null;
    private String capaMime = null;

    // Lista de arquivos (múltiplos)
    private final List<ArquivoSelecionado> arquivos = new ArrayList<>();

    // ── Dados da cidade ───────────────────────────────────────────────────────
    private String cidadeId   = null;
    private String cidadeNome = null;
    private String docId      = null;
    private boolean modoEdicao = false;

    private FirebaseFirestore db;

    // ── Modelo interno ────────────────────────────────────────────────────────
    static class ArquivoSelecionado {
        Uri    uri;
        String nome;
        String mime;
        String uploadUrl;
        String uploadId;

        ArquivoSelecionado(Uri uri, String nome, String mime) {
            this.uri  = uri;
            this.nome = nome;
            this.mime = mime;
        }
    }

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
        docId      = getIntent().getStringExtra("docId");
        modoEdicao = docId != null;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(modoEdicao ? "Editar Conteúdo" : "Criar Conteúdo");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        configurarTipos();
        configurarCategorias();

        if (modoEdicao) {
            carregarDadosEdicao();
            btnSalvar.setText("SALVAR ALTERAÇÕES");
        }

        layoutUploadCapa.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        layoutUploadArquivo.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        layoutUploadCapa.setOnClickListener(v -> abrirCapaPicker());
        btnRemoverCapa.setOnClickListener(v -> removerCapa());
        layoutUploadArquivo.setOnClickListener(v -> {
            if (arquivos.size() >= MAX_ARQUIVOS) {
                new AlertDialog.Builder(this)
                        .setTitle("Limite atingido")
                        .setMessage("Você pode adicionar no máximo " + MAX_ARQUIVOS + " arquivos.")
                        .setPositiveButton("OK", null).show();
                return;
            }
            abrirArquivoPicker();
        });

        btnSalvar.setOnClickListener(v -> publicar());
        atualizarContadorArquivos();
    }

    // =========================================================================
    // BIND
    // =========================================================================

    private void bindViews() {
        layoutTitulo               = findViewById(R.id.layoutTitulo);
        layoutDescricao            = findViewById(R.id.layoutDescricao);
        layoutTexto                = findViewById(R.id.layoutTexto);
        editTitulo                 = findViewById(R.id.editTitulo);
        editDescricao              = findViewById(R.id.editDescricao);
        editTexto                  = findViewById(R.id.editTexto);
        btnTipoDica                = findViewById(R.id.btnTipoDica);
        btnTipoVideo               = findViewById(R.id.btnTipoVideo);
        btnTipoNoticia             = findViewById(R.id.btnTipoNoticia);
        btnTipoMaterial            = findViewById(R.id.btnTipoMaterial);
        chipEnchente               = findViewById(R.id.chipEnchente);
        chipDeslizamento           = findViewById(R.id.chipDeslizamento);
        chipTempestade             = findViewById(R.id.chipTempestade);
        chipOutros                 = findViewById(R.id.chipOutros);
        lblEnchente                = findViewById(R.id.lblEnchente);
        lblDeslizamento            = findViewById(R.id.lblDeslizamento);
        lblTempestade              = findViewById(R.id.lblTempestade);
        lblOutros                  = findViewById(R.id.lblOutros);
        txtCategoriasErro          = findViewById(R.id.txtCategoriasErro);
        layoutUploadCapa           = findViewById(R.id.layoutUploadCapa);
        cardPreviewCapa            = findViewById(R.id.cardPreviewCapa);
        ivPreviewCapa              = findViewById(R.id.ivPreviewCapa);
        btnRemoverCapa             = findViewById(R.id.btnRemoverCapa);
        layoutUploadArquivo        = findViewById(R.id.layoutUploadArquivo);
        layoutArquivosSelecionados = findViewById(R.id.layoutArquivosSelecionados);
        txtContadorArquivos        = findViewById(R.id.txtContadorArquivos);
        btnSalvar                  = findViewById(R.id.btnSalvar);
        progressBar                = findViewById(R.id.progressBar);
        txtProgresso               = findViewById(R.id.txtProgresso);
    }

    // =========================================================================
    // TIPOS
    // =========================================================================

    private void configurarTipos() {
        selecionarTipo("dica");
        btnTipoDica.setOnClickListener(v     -> selecionarTipo("dica"));
        btnTipoVideo.setOnClickListener(v    -> selecionarTipo("video"));
        btnTipoNoticia.setOnClickListener(v  -> selecionarTipo("noticia"));
        btnTipoMaterial.setOnClickListener(v -> selecionarTipo("material"));
    }

    private void selecionarTipo(String tipo) {
        tipoSelecionado = tipo;
        atualizarVisualTipo(btnTipoDica,     "dica");
        atualizarVisualTipo(btnTipoVideo,    "video");
        atualizarVisualTipo(btnTipoNoticia,  "noticia");
        atualizarVisualTipo(btnTipoMaterial, "material");
    }

    private void atualizarVisualTipo(LinearLayout btn, String tipo) {
        boolean ativo = tipoSelecionado.equals(tipo);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(12));
        bg.setColor(ativo ? 0xFFE8F5E9 : 0xFFFAFAFA);
        bg.setStroke(ativo ? dp(2) : dp(1), ativo ? 0xFF2E7D32 : 0xFFE0E0E0);
        btn.setBackground(bg);
        if (btn.getChildCount() >= 1 && btn.getChildAt(0) instanceof ImageView)
            ((ImageView) btn.getChildAt(0)).setColorFilter(ativo ? 0xFF2E7D32 : 0xFF9E9E9E);
        if (btn.getChildCount() >= 2 && btn.getChildAt(1) instanceof TextView) {
            TextView lbl = (TextView) btn.getChildAt(1);
            lbl.setTextColor(ativo ? 0xFF2E7D32 : 0xFF9E9E9E);
            lbl.setTypeface(null, ativo ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    // =========================================================================
    // CATEGORIAS (múltipla seleção)
    // =========================================================================

    private void configurarCategorias() {
        chipEnchente.setOnClickListener(v     -> toggleCategoria(CAT_ENCHENTE,     chipEnchente,     lblEnchente));
        chipDeslizamento.setOnClickListener(v -> toggleCategoria(CAT_DESLIZAMENTO, chipDeslizamento, lblDeslizamento));
        chipTempestade.setOnClickListener(v   -> toggleCategoria(CAT_TEMPESTADE,   chipTempestade,   lblTempestade));
        chipOutros.setOnClickListener(v       -> toggleCategoria(CAT_OUTROS,       chipOutros,       lblOutros));
    }

    private void toggleCategoria(String cat, LinearLayout chip, TextView label) {
        txtCategoriasErro.setVisibility(View.GONE);

        boolean eraSelecionado = categoriasSelecionadas.contains(cat);
        if (eraSelecionado) {
            categoriasSelecionadas.remove(cat);
        } else {
            categoriasSelecionadas.add(cat);
        }
        atualizarVisualCategoria(chip, label, !eraSelecionado);
    }

    private void atualizarVisualCategoria(LinearLayout chip, TextView label, boolean ativo) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(12));
        bg.setColor(ativo ? 0xFFE8F5E9 : 0xFFFAFAFA);
        bg.setStroke(ativo ? dp(2) : dp(1), ativo ? 0xFF2E7D32 : 0xFFE0E0E0);
        chip.setBackground(bg);
        label.setTextColor(ativo ? 0xFF2E7D32 : 0xFF9E9E9E);
        label.setTypeface(null, ativo ? Typeface.BOLD : Typeface.NORMAL);
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
        } catch (Exception ignored) {}

        cardPreviewCapa.setVisibility(View.VISIBLE);
        layoutUploadCapa.setVisibility(View.GONE);
    }

    private void removerCapa() {
        capaUri  = null;
        capaNome = null;
        capaMime = null;
        ivPreviewCapa.setImageBitmap(null);
        cardPreviewCapa.setVisibility(View.GONE);
        layoutUploadCapa.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // ARQUIVOS (múltiplos)
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
        String mime = getContentResolver().getType(uri);
        String nome = resolverNome(uri);

        ArquivoSelecionado arq = new ArquivoSelecionado(uri, nome, mime);
        arquivos.add(arq);

        adicionarItemArquivoNaLista(arq);
        atualizarContadorArquivos();

        if (arquivos.size() >= MAX_ARQUIVOS) {
            layoutUploadArquivo.setVisibility(View.GONE);
        }
        layoutArquivosSelecionados.setVisibility(View.VISIBLE);
    }

    private void adicionarItemArquivoNaLista(ArquivoSelecionado arq) {
        LinearLayout row = new LinearLayout(this);
        row.setTag(arq);
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

        TextView chip = new TextView(this);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(dp(40), dp(22));
        chipLp.rightMargin = dp(10);
        chip.setLayoutParams(chipLp);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setText(chipLabel(arq.mime));
        chip.setTextSize(9f);
        chip.setTextColor(Color.WHITE);
        chip.setTypeface(null, Typeface.BOLD);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(dp(4));
        chipBg.setColor(corChip(arq.mime));
        chip.setBackground(chipBg);
        row.addView(chip);

        TextView txtNome = new TextView(this);
        txtNome.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        txtNome.setText(arq.nome);
        txtNome.setTextSize(13f);
        txtNome.setTextColor(0xFF1A1A1A);
        txtNome.setMaxLines(1);
        txtNome.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        row.addView(txtNome);

        TextView btnRemover = new TextView(this);
        btnRemover.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        btnRemover.setGravity(android.view.Gravity.CENTER);
        btnRemover.setText("✕");
        btnRemover.setTextSize(14f);
        btnRemover.setTextColor(0xFFE53935);
        btnRemover.setTypeface(null, Typeface.BOLD);
        btnRemover.setOnClickListener(v -> removerArquivo(arq, row));
        row.addView(btnRemover);

        layoutArquivosSelecionados.addView(row);
    }

    private void removerArquivo(ArquivoSelecionado arq, View row) {
        arquivos.remove(arq);
        layoutArquivosSelecionados.removeView(row);
        atualizarContadorArquivos();

        if (arquivos.size() < MAX_ARQUIVOS) {
            layoutUploadArquivo.setVisibility(View.VISIBLE);
        }
        if (arquivos.isEmpty()) {
            layoutArquivosSelecionados.setVisibility(View.GONE);
        }
    }

    private void atualizarContadorArquivos() {
        txtContadorArquivos.setText(arquivos.size() + " / " + MAX_ARQUIVOS);
        txtContadorArquivos.setTextColor(
                arquivos.size() >= MAX_ARQUIVOS ? 0xFFE53935 : 0xFF9E9E9E);
    }

    // =========================================================================
    // PUBLICAR
    // =========================================================================

    private void carregarDadosEdicao() {
        setCarregando(true);
        db.collection("conteudos").document(docId).get()
                .addOnSuccessListener(doc -> {
                    setCarregando(false);
                    if (!doc.exists()) {
                        finish();
                        return;
                    }

                    editTitulo.setText(doc.getString("titulo"));
                    editDescricao.setText(doc.getString("descricao"));
                    editTexto.setText(doc.getString("texto"));

                    String tipo = doc.getString("tipo");
                    if (tipo != null) selecionarTipo(tipo);

                    List<String> cats = (List<String>) doc.get("categorias");
                    if (cats != null) {
                        for (String cat : cats) {
                            switch (cat) {
                                case CAT_ENCHENTE:     toggleCategoria(CAT_ENCHENTE,     chipEnchente,     lblEnchente);     break;
                                case CAT_DESLIZAMENTO: toggleCategoria(CAT_DESLIZAMENTO, chipDeslizamento, lblDeslizamento); break;
                                case CAT_TEMPESTADE:   toggleCategoria(CAT_TEMPESTADE,   chipTempestade,   lblTempestade);   break;
                                case CAT_OUTROS:       toggleCategoria(CAT_OUTROS,       chipOutros,       lblOutros);       break;
                            }
                        }
                    }

                    String capa = doc.getString("capaUrl");
                    if (capa != null && !capa.isEmpty()) {
                        layoutUploadCapa.setVisibility(View.GONE);
                        cardPreviewCapa.setVisibility(View.VISIBLE);
                        // Usando Glide para preview da capa existente
                        GlideUrl glideUrl = new GlideUrl(capa, new LazyHeaders.Builder()
                                .addHeader("Authorization", "Bearer " + "-R,V*ox+>K,0o76MH=XYNG9.sRz@xLLR")
                                .addHeader("ngrok-skip-browser-warning", "true")
                                .build());
                        com.bumptech.glide.Glide.with(this).load(glideUrl).into(ivPreviewCapa);
                    }

                    List<Map<String, Object>> arqs = (List<Map<String, Object>>) doc.get("arquivos");
                    if (arqs != null) {
                        for (Map<String, Object> a : arqs) {
                            ArquivoSelecionado as = new ArquivoSelecionado(null, (String) a.get("nome"), (String) a.get("tipo"));
                            as.uploadUrl = (String) a.get("url");
                            as.uploadId  = (String) a.get("id");
                            arquivos.add(as);
                            adicionarItemArquivoNaLista(as);
                        }
                        layoutArquivosSelecionados.setVisibility(View.VISIBLE);
                        atualizarContadorArquivos();
                    }
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    Toast.makeText(this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
                });
    }

    private void publicar() {
        layoutTitulo.setError(null);
        layoutDescricao.setError(null);
        layoutTexto.setError(null);
        txtCategoriasErro.setVisibility(View.GONE);

        String titulo    = editTitulo.getText()    != null ? editTitulo.getText().toString().trim() : "";
        String descricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";
        String texto     = editTexto.getText()     != null ? editTexto.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo))             { layoutTitulo.setError("Informe o título");            return; }
        if (TextUtils.isEmpty(descricao))          { layoutDescricao.setError("Informe a descrição");      return; }
        if (TextUtils.isEmpty(texto))              { layoutTexto.setError("Informe o texto do conteúdo");  return; }
        if (categoriasSelecionadas.isEmpty()) {
            txtCategoriasErro.setText("Selecione ao menos uma categoria");
            txtCategoriasErro.setVisibility(View.VISIBLE);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setCarregando(true);

        if (capaUri != null) {
            setStatus("Enviando imagem de capa...");
            ApiClient.getInstance().uploadArquivo(
                    this, capaUri, capaNome, capaMime,
                    new ApiClient.Callback<ApiClient.ArquivoApi>() {
                        @Override public void onSucesso(ApiClient.ArquivoApi r) {
                            runOnUiThread(() -> uploadProximoArquivo(
                                    titulo, descricao, texto, user.getUid(), r.getDownloadUrl(), 0));
                        }
                        @Override public void onErro(String msg) {
                            runOnUiThread(() -> { setCarregando(false); layoutTitulo.setError("Erro na capa: " + msg); });
                        }
                    });
        } else {
            uploadProximoArquivo(titulo, descricao, texto, user.getUid(), null, 0);
        }
    }

    private void uploadProximoArquivo(String titulo, String descricao, String texto,
                                      String uid, String capaUrl, int indice) {
        if (indice >= arquivos.size()) {
            setStatus("Salvando conteúdo...");
            salvarFirestore(titulo, descricao, texto, uid, capaUrl);
            return;
        }

        ArquivoSelecionado arq = arquivos.get(indice);
        setStatus("Enviando arquivo " + (indice + 1) + " de " + arquivos.size() + "...");

        ApiClient.getInstance().uploadArquivo(
                this, arq.uri, arq.nome, arq.mime,
                new ApiClient.Callback<ApiClient.ArquivoApi>() {
                    @Override public void onSucesso(ApiClient.ArquivoApi r) {
                        arq.uploadUrl = r.getDownloadUrl();
                        arq.uploadId  = r.id;
                        runOnUiThread(() -> uploadProximoArquivo(
                                titulo, descricao, texto, uid, capaUrl, indice + 1));
                    }
                    @Override public void onErro(String msg) {
                        runOnUiThread(() -> {
                            setCarregando(false);
                            layoutTitulo.setError("Erro no arquivo \"" + arq.nome + "\": " + msg);
                        });
                    }
                });
    }

    // =========================================================================
    // FIRESTORE
    // =========================================================================

    private void salvarFirestore(String titulo, String descricao, String texto,
                                 String uid, String capaUrl) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo",      titulo);
        dados.put("descricao",   descricao);
        dados.put("texto",       texto);
        dados.put("tipo",        tipoSelecionado);
        dados.put("categorias",  new ArrayList<>(categoriasSelecionadas));

        if (capaUrl != null) dados.put("capaUrl", capaUrl);

        if (!arquivos.isEmpty()) {
            List<Map<String, Object>> listaArquivos = new ArrayList<>();
            for (ArquivoSelecionado arq : arquivos) {
                Map<String, Object> a = new HashMap<>();
                a.put("url",  arq.uploadUrl);
                a.put("id",   arq.uploadId);
                a.put("nome", arq.nome);
                a.put("tipo", arq.mime);
                listaArquivos.add(a);
            }
            dados.put("arquivos",    listaArquivos);
            dados.put("temAnexo",    true);
            dados.put("arquivoUrl",  arquivos.get(0).uploadUrl);
            dados.put("arquivoId",   arquivos.get(0).uploadId);
            dados.put("arquivoNome", arquivos.get(0).nome);
            dados.put("arquivoTipo", arquivos.get(0).mime);
        } else {
            dados.put("temAnexo", false);
            dados.put("arquivos", new ArrayList<>());
        }

        if (modoEdicao) {
            db.collection("conteudos").document(docId).update(dados)
                    .addOnSuccessListener(v -> {
                        setCarregando(false);
                        Toast.makeText(this, "Conteúdo atualizado!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setCarregando(false);
                        layoutTitulo.setError("Erro: " + e.getMessage());
                    });
            return;
        }

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

                    dados.put("data",        dataAtual);
                    dados.put("ordem",       proximaOrdem);
                    dados.put("criadoPor",   uid);

                    if (cidadeId   != null) dados.put("cidadeId",   cidadeId);
                    if (cidadeNome != null) dados.put("cidadeNome", cidadeNome);

                    db.collection("conteudos").add(dados)
                            .addOnSuccessListener(ref -> {
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
        btnSalvar.setText(c ? "Aguarde..." : "Publicar conteúdo");
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