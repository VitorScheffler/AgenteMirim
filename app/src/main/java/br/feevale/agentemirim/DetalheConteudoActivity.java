package br.feevale.agentemirim;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.feevale.agentemirim.api.ApiClient;

/**
 * Tela de detalhe de conteúdo com:
 *  - Badge + Título + Meta (tempo, data, cidade)
 *  - Imagem de capa
 *  - Tabs: "Sobre o conteúdo" | "Materiais e arquivos"
 *  - Seção "O que você vai aprender" (tópicos)
 *  - Lista de arquivos com download
 *  - Editar / Apagar (projeto + admin)
 *  - Compartilhar + Salvar para depois
 *
 * Destino: app/src/main/java/br/feevale/agentemirim/DetalheConteudoActivity.java
 */
public class DetalheConteudoActivity extends AppCompatActivity {

    // ── Extras ───────────────────────────────────────────────────────────────
    public static final String EXTRA_DOC_ID       = "docId";
    public static final String EXTRA_TITULO       = "titulo";
    public static final String EXTRA_DESCRICAO    = "descricao";
    public static final String EXTRA_DATA         = "data";
    public static final String EXTRA_CATEGORIA    = "categoria";
    public static final String EXTRA_TEM_ANEXO    = "temAnexo";
    public static final String EXTRA_ARQUIVO_URL  = "arquivoUrl";
    public static final String EXTRA_ARQUIVO_NOME = "arquivoNome";
    public static final String EXTRA_ARQUIVO_TIPO = "arquivoTipo";
    public static final String EXTRA_ARQUIVO_ID   = "arquivoId";
    public static final String EXTRA_CAPA_URL     = "capaUrl";

    // ── Views: cabeçalho ─────────────────────────────────────────────────────
    private TextView  txtBadgeDetalhe;
    private TextView  txtTitulo;
    private TextView  txtTempoLeitura;
    private TextView  txtData;
    private TextView  txtCidade;
    private ImageView ivCapaDetalhe;

    // ── Views: tabs ───────────────────────────────────────────────────────────
    private TextView     tabSobre;
    private TextView     tabMateriais;
    private LinearLayout painelSobre;
    private LinearLayout painelMateriais;

    // ── Views: painel "sobre" ─────────────────────────────────────────────────
    private TextView     txtDescricao;
    private View         cardAprender;
    private LinearLayout layoutTopicos;
    private LinearLayout layoutAcoes;
    private MaterialButton btnEditar, btnApagar;
    private LinearLayout layoutEdicao;
    private TextInputLayout layoutEditTitulo, layoutEditDescricao;
    private TextInputEditText editTitulo, editDescricao;
    private MaterialButton btnSalvarEdicao, btnCancelarEdicao;

    // ── Views: painel "materiais" ─────────────────────────────────────────────
    private LinearLayout layoutArquivos;
    private TextView     txtSemArquivos;

    // ── Views: rodapé ─────────────────────────────────────────────────────────
    private MaterialButton btnCompartilharRodape;
    private MaterialButton btnSalvarDepois;
    private ProgressBar    progressBar;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private String docId, arquivoUrl, arquivoNome, arquivoTipo, arquivoId, capaUrl;
    private String categoriaAtual, cidadeNome;

    private FirebaseFirestore db;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_conteudo);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Voltar
        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> finish());

        // Compartilhar toolbar
        ImageView btnShare = findViewById(R.id.btnCompartilhar);
        if (btnShare != null) btnShare.setOnClickListener(v -> compartilhar());

        bindViews();
        preencherDados();
        configurarTabs();
        verificarPermissao();
    }

    // =========================================================================
    // BIND
    // =========================================================================

    private void bindViews() {
        txtBadgeDetalhe     = findViewById(R.id.txtBadgeDetalhe);
        txtTitulo           = findViewById(R.id.txtTitulo);
        txtTempoLeitura     = findViewById(R.id.txtTempoLeitura);
        txtData             = findViewById(R.id.txtData);
        txtCidade           = findViewById(R.id.txtCidade);
        ivCapaDetalhe       = findViewById(R.id.ivCapaDetalhe);

        tabSobre            = findViewById(R.id.tabSobre);
        tabMateriais        = findViewById(R.id.tabMateriais);
        painelSobre         = findViewById(R.id.painelSobre);
        painelMateriais     = findViewById(R.id.painelMateriais);

        txtDescricao        = findViewById(R.id.txtDescricao);
        cardAprender        = findViewById(R.id.cardAprender);
        layoutTopicos       = findViewById(R.id.layoutTopicos);
        layoutAcoes         = findViewById(R.id.layoutAcoes);
        btnEditar           = findViewById(R.id.btnEditar);
        btnApagar           = findViewById(R.id.btnApagar);
        layoutEdicao        = findViewById(R.id.layoutEdicao);
        layoutEditTitulo    = findViewById(R.id.layoutEditTitulo);
        layoutEditDescricao = findViewById(R.id.layoutEditDescricao);
        editTitulo          = findViewById(R.id.editTitulo);
        editDescricao       = findViewById(R.id.editDescricao);
        btnSalvarEdicao     = findViewById(R.id.btnSalvarEdicao);
        btnCancelarEdicao   = findViewById(R.id.btnCancelarEdicao);

        layoutArquivos      = findViewById(R.id.layoutArquivos);
        txtSemArquivos      = findViewById(R.id.txtSemArquivos);

        btnCompartilharRodape = findViewById(R.id.btnCompartilharRodape);
        btnSalvarDepois       = findViewById(R.id.btnSalvarDepois);
        progressBar           = findViewById(R.id.progressBar);

        layoutAcoes.setVisibility(View.GONE);
        layoutEdicao.setVisibility(View.GONE);
    }

    // =========================================================================
    // PREENCHER DADOS
    // =========================================================================

    private void preencherDados() {
        Intent i = getIntent();

        docId          = i.getStringExtra(EXTRA_DOC_ID);
        arquivoUrl     = i.getStringExtra(EXTRA_ARQUIVO_URL);
        arquivoNome    = i.getStringExtra(EXTRA_ARQUIVO_NOME);
        arquivoTipo    = i.getStringExtra(EXTRA_ARQUIVO_TIPO);
        arquivoId      = i.getStringExtra(EXTRA_ARQUIVO_ID);
        capaUrl        = i.getStringExtra(EXTRA_CAPA_URL);
        categoriaAtual = i.getStringExtra(EXTRA_CATEGORIA);
        cidadeNome     = i.getStringExtra("cidadeNome");

        String titulo    = i.getStringExtra(EXTRA_TITULO);
        String descricao = i.getStringExtra(EXTRA_DESCRICAO);
        String data      = i.getStringExtra(EXTRA_DATA);
        boolean temAnexo = i.getBooleanExtra(EXTRA_TEM_ANEXO, false);

        // ── Textos ────────────────────────────────────────────────────────────
        txtTitulo.setText(titulo    != null ? titulo    : "");
        txtDescricao.setText(descricao != null ? descricao : "");
        txtData.setText(data        != null ? data      : "");
        txtCidade.setText(cidadeNome != null ? cidadeNome : "");

        // Tempo estimado de leitura
        int chars = descricao != null ? descricao.length() : 0;
        txtTempoLeitura.setText(Math.max(1, chars / 200) + " min de leitura");

        // Pré-preenche campos de edição
        editTitulo.setText(titulo    != null ? titulo    : "");
        editDescricao.setText(descricao != null ? descricao : "");

        // ── Badge de categoria ────────────────────────────────────────────────
        String badge = badgeLabel(categoriaAtual);
        int    cor   = corCategoria(categoriaAtual);
        txtBadgeDetalhe.setText(badge);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(dp(6));
        badgeBg.setColor(cor);
        txtBadgeDetalhe.setBackground(badgeBg);

        // ── Imagem de capa ────────────────────────────────────────────────────
        if (capaUrl != null && !capaUrl.isEmpty()) {
            carregarImagem(ivCapaDetalhe, capaUrl);
        } else {
            ivCapaDetalhe.setBackgroundColor(cor);
            ivCapaDetalhe.setImageResource(icone(categoriaAtual));
            ivCapaDetalhe.setColorFilter(0x44FFFFFF);
        }

        // ── Tópicos "O que você vai aprender" ─────────────────────────────────
        // Extrai linhas da descrição como tópicos (máx 3)
        if (descricao != null && !descricao.isEmpty()) {
            String[] linhas = descricao.split("[.!?]");
            int count = Math.min(linhas.length, 3);
            if (count > 0) {
                cardAprender.setVisibility(View.VISIBLE);
                for (int idx = 0; idx < count; idx++) {
                    String topico = linhas[idx].trim();
                    if (!topico.isEmpty()) adicionarTopico(topico);
                }
            }
        }

        // ── Arquivos ──────────────────────────────────────────────────────────
        if (temAnexo && arquivoUrl != null && !arquivoUrl.isEmpty()) {
            txtSemArquivos.setVisibility(View.GONE);
            adicionarLinhaArquivo(arquivoNome, arquivoTipo, arquivoUrl);
        } else {
            txtSemArquivos.setVisibility(View.VISIBLE);
        }

        // ── Rodapé ────────────────────────────────────────────────────────────
        btnCompartilharRodape.setOnClickListener(v -> compartilhar());
        btnSalvarDepois.setOnClickListener(v -> salvarDepois());
    }

    // =========================================================================
    // TABS
    // =========================================================================

    private void configurarTabs() {
        // Padrão: aba "Sobre" ativa
        ativarTab(true);

        tabSobre.setOnClickListener(v -> ativarTab(true));
        tabMateriais.setOnClickListener(v -> ativarTab(false));
    }

    private void ativarTab(boolean sobre) {
        painelSobre.setVisibility(sobre ? View.VISIBLE : View.GONE);
        painelMateriais.setVisibility(sobre ? View.GONE : View.VISIBLE);

        // Tab ativa
        aplicarEstiloTab(tabSobre, sobre);
        aplicarEstiloTab(tabMateriais, !sobre);
    }

    private void aplicarEstiloTab(TextView tab, boolean ativo) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(10));
        if (ativo) {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(2), 0xFF2E7D32);
            tab.setTextColor(0xFF2E7D32);
            tab.setTypeface(null, Typeface.BOLD);
        } else {
            bg.setColor(0xFFF0F0F0);
            tab.setTextColor(0xFF9E9E9E);
            tab.setTypeface(null, Typeface.NORMAL);
        }
        tab.setBackground(bg);
    }

    // =========================================================================
    // TÓPICOS
    // =========================================================================

    private void adicionarTopico(String texto) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Ícone de check
        TextView check = new TextView(this);
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        checkLp.rightMargin = dp(10);
        check.setLayoutParams(checkLp);
        check.setGravity(android.view.Gravity.CENTER);
        check.setText("✅");
        check.setTextSize(14f);
        row.addView(check);

        // Texto
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(texto);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF424242);
        row.addView(tv);

        layoutTopicos.addView(row);
    }

    // =========================================================================
    // LINHA DE ARQUIVO
    // =========================================================================

    private void adicionarLinhaArquivo(String nome, String mime, String url) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_arquivo_detalhe, layoutArquivos, false);

        TextView  chip        = row.findViewById(R.id.txtTipoChip);
        TextView  txtNome     = row.findViewById(R.id.txtNomeArquivo);
        TextView  txtTamanho  = row.findViewById(R.id.txtTamanho);
        ImageView btnDownload = row.findViewById(R.id.btnDownload);

        // Chip colorido
        String chipLabel = chipLabel(mime);
        int    chipCor   = corChip(mime);
        chip.setText(chipLabel);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(dp(4));
        chipBg.setColor(chipCor);
        chip.setBackground(chipBg);

        txtNome.setText(nome != null ? nome : "arquivo");
        txtTamanho.setText("");

        btnDownload.setOnClickListener(v -> baixarArquivo(url, nome));
        row.setOnClickListener(v -> abrirNoNavegador(url));

        layoutArquivos.addView(row);

        // Adiciona divisória (exceto último)
        View div = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.setMarginStart(dp(16));
        divLp.setMarginEnd(dp(16));
        div.setLayoutParams(divLp);
        div.setBackgroundColor(0xFFF0F0F0);
        layoutArquivos.addView(div);
    }

    // =========================================================================
    // COMPARTILHAR / SALVAR
    // =========================================================================

    private void compartilhar() {
        String titulo    = txtTitulo.getText().toString();
        String descricao = txtDescricao.getText().toString();
        String texto     = "📚 " + titulo
                + "\n\n" + descricao
                + (arquivoUrl != null ? "\n\n🔗 " + arquivoUrl : "")
                + "\n\nCompartilhado via Agente Mirim";

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(share, "Compartilhar via..."));
    }

    private void salvarDepois() {
        // TODO: implementar lista de favoritos no Firestore
        new AlertDialog.Builder(this)
                .setTitle("⭐ Salvo!")
                .setMessage("Este conteúdo foi salvo para você ler depois.")
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================================
    // PERMISSÃO DE EDIÇÃO
    // =========================================================================

    private void verificarPermissao() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String perfil = doc.exists() && doc.getString("perfil") != null
                            ? doc.getString("perfil") : "usuario";
                    boolean podeEditar = "projeto".equals(perfil) || "admin".equals(perfil);
                    if (podeEditar) {
                        layoutAcoes.setVisibility(View.VISIBLE);
                        btnEditar.setOnClickListener(v -> entrarModoEdicao());
                        btnApagar.setOnClickListener(v -> confirmarApagar());
                    }
                });
    }

    // =========================================================================
    // EDIÇÃO
    // =========================================================================

    private void entrarModoEdicao() {
        txtTitulo.setVisibility(View.GONE);
        txtDescricao.setVisibility(View.GONE);
        cardAprender.setVisibility(View.GONE);
        layoutAcoes.setVisibility(View.GONE);
        layoutEdicao.setVisibility(View.VISIBLE);
        btnSalvarEdicao.setOnClickListener(v -> salvarEdicao());
        btnCancelarEdicao.setOnClickListener(v -> cancelarEdicao());
    }

    private void cancelarEdicao() {
        layoutEdicao.setVisibility(View.GONE);
        txtTitulo.setVisibility(View.VISIBLE);
        txtDescricao.setVisibility(View.VISIBLE);
        cardAprender.setVisibility(View.VISIBLE);
        layoutAcoes.setVisibility(View.VISIBLE);
        layoutEditTitulo.setError(null);
        layoutEditDescricao.setError(null);
    }

    private void salvarEdicao() {
        layoutEditTitulo.setError(null);
        layoutEditDescricao.setError(null);

        String novoTitulo    = editTitulo.getText()    != null ? editTitulo.getText().toString().trim()    : "";
        String novaDescricao = editDescricao.getText() != null ? editDescricao.getText().toString().trim() : "";

        if (TextUtils.isEmpty(novoTitulo))    { layoutEditTitulo.setError("Informe o título");     return; }
        if (TextUtils.isEmpty(novaDescricao)) { layoutEditDescricao.setError("Informe a descrição"); return; }
        if (docId == null) return;

        setCarregando(true);

        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo",    novoTitulo);
        dados.put("descricao", novaDescricao);

        db.collection("conteudos").document(docId).update(dados)
                .addOnSuccessListener(v -> {
                    setCarregando(false);
                    txtTitulo.setText(novoTitulo);
                    txtDescricao.setText(novaDescricao);
                    editTitulo.setText(novoTitulo);
                    editDescricao.setText(novaDescricao);
                    cancelarEdicao();
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Salvo!")
                            .setMessage("Conteúdo atualizado com sucesso.")
                            .setPositiveButton("OK", null).show();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutEditTitulo.setError("Erro: " + e.getMessage());
                });
    }

    // =========================================================================
    // APAGAR
    // =========================================================================

    private void confirmarApagar() {
        new AlertDialog.Builder(this)
                .setTitle("Apagar conteúdo")
                .setMessage("Tem certeza? Esta ação não pode ser desfeita."
                        + (arquivoId != null ? "\n\nO arquivo anexado também será removido." : ""))
                .setPositiveButton("Apagar", (d, w) -> apagarConteudo())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarConteudo() {
        if (docId == null) return;
        setCarregando(true);

        db.collection("conteudos").document(docId).delete()
                .addOnSuccessListener(v -> {
                    if (arquivoId != null && !arquivoId.isEmpty()) {
                        ApiClient.getInstance().deletarArquivo(arquivoId,
                                new ApiClient.Callback<Void>() {
                                    @Override public void onSucesso(Void r) {}
                                    @Override public void onErro(String m) {}
                                });
                    }
                    setCarregando(false);
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Apagado!")
                            .setMessage("Conteúdo removido com sucesso.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .setCancelable(false).show();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Erro")
                            .setMessage("Não foi possível apagar: " + e.getMessage())
                            .setPositiveButton("OK", null).show();
                });
    }

    // =========================================================================
    // AÇÕES DE ARQUIVO
    // =========================================================================

    private void abrirNoNavegador(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            baixarArquivo(url, arquivoNome);
        }
    }

    private void baixarArquivo(String url, String nome) {
        try {
            android.app.DownloadManager.Request req =
                    new android.app.DownloadManager.Request(Uri.parse(url));
            req.setTitle(nome != null ? nome : "arquivo");
            req.setDescription("Baixando via Agente Mirim...");
            req.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    nome != null ? nome : "agentemirim_arquivo");
            req.addRequestHeader("ngrok-skip-browser-warning", "true");
            android.app.DownloadManager dm =
                    (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) dm.enqueue(req);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        if (btnEditar  != null) btnEditar.setEnabled(!c);
        if (btnApagar  != null) btnApagar.setEnabled(!c);
        if (btnSalvarEdicao != null) btnSalvarEdicao.setEnabled(!c);
    }

    private void carregarImagem(ImageView iv, String url) {
        new Thread(() -> {
            try {
                java.net.URL imgUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) imgUrl.openConnection();
                conn.setRequestProperty("ngrok-skip-browser-warning", "true");
                conn.connect();
                Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
                runOnUiThread(() -> { if (bmp != null) iv.setImageBitmap(bmp); });
            } catch (Exception ignored) {}
        }).start();
    }

    private String badgeLabel(String cat) {
        if (cat == null) return "OUTRO";
        switch (cat.toLowerCase()) {
            case "dica":     return "DICA";
            case "video":    return "VÍDEO";
            case "noticia":  return "NOTÍCIA";
            case "material": return "MATERIAL";
            default:         return "OUTRO";
        }
    }

    private int corCategoria(String cat) {
        if (cat == null) return 0xFF37474F;
        switch (cat.toLowerCase()) {
            case "dica":     return 0xFF2E7D32;
            case "video":    return 0xFF1565C0;
            case "noticia":  return 0xFFE65100;
            case "material": return 0xFF7B1FA2;
            default:         return 0xFF37474F;
        }
    }

    private int icone(String cat) {
        if (cat == null) return android.R.drawable.ic_menu_agenda;
        switch (cat.toLowerCase()) {
            case "dica":    return android.R.drawable.ic_menu_help;
            case "video":   return android.R.drawable.ic_media_play;
            case "noticia": return android.R.drawable.ic_menu_send;
            default:        return android.R.drawable.ic_menu_agenda;
        }
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
