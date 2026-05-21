package br.feevale.agentemirim;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela de conteúdos de uma cidade específica.
 * Recebe via Intent: cidadeId, cidadeNome, cidadeDescricao, cidadeImagemUrl
 *
 * Destino: app/src/main/java/br/feevale/agentemirim/ConteudosCidadeActivity.java
 */
public class ConteudosCidadeActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private ImageView            ivCapaCidade;
    private TextView             txtNomeCidadeTitulo;
    private TextView             txtDescricaoCidade;
    private LinearLayout         layoutChipsCategorias;
    private RecyclerView         recyclerConteudos;
    private LinearLayout         layoutVazio;
    private ProgressBar          progressBar;
    private FloatingActionButton fabNovoConteudo;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private String cidadeId, cidadeNome, cidadeImagemUrl;
    private String categoriaAtiva = "todos";

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerConteudos;
    private ConteudoCidadeAdapter adapter;

    // ── Categorias ────────────────────────────────────────────────────────────
    private static final String[] CATEGORIAS     = {"todos", "dica", "video", "noticia", "material"};
    private static final String[] CATEGORIAS_LABEL = {"Todos", "Enchentes", "Deslizamentos", "Tempestades", "Outros"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos_cidade);

        db = FirebaseFirestore.getInstance();

        cidadeId       = getIntent().getStringExtra("cidadeId");
        cidadeNome     = getIntent().getStringExtra("cidadeNome");
        cidadeImagemUrl= getIntent().getStringExtra("cidadeImagemUrl");
        String cidadeDescricao = getIntent().getStringExtra("cidadeDescricao");

        bindViews();

        // Voltar
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        // Preenche cabeçalho
        txtNomeCidadeTitulo.setText(cidadeNome != null ? cidadeNome : "");
        txtDescricaoCidade.setText(cidadeDescricao != null ? cidadeDescricao : "");

        // Imagem da cidade
        if (cidadeImagemUrl != null && !cidadeImagemUrl.isEmpty()) {
            carregarImagem(ivCapaCidade, cidadeImagemUrl);
        }

        configurarChips();
        verificarPerfil();
        iniciarListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerConteudos != null) { listenerConteudos.remove(); listenerConteudos = null; }
    }

    private void bindViews() {
        ivCapaCidade         = findViewById(R.id.ivCapaCidade);
        txtNomeCidadeTitulo  = findViewById(R.id.txtNomeCidadeTitulo);
        txtDescricaoCidade   = findViewById(R.id.txtDescricaoCidade);
        layoutChipsCategorias= findViewById(R.id.layoutChipsCategorias);
        recyclerConteudos    = findViewById(R.id.recyclerConteudos);
        layoutVazio          = findViewById(R.id.layoutVazio);
        progressBar          = findViewById(R.id.progressBar);
        fabNovoConteudo      = findViewById(R.id.fabNovoConteudo);
        fabNovoConteudo.setVisibility(View.GONE);
        recyclerConteudos.setLayoutManager(new LinearLayoutManager(this));
    }

    // =========================================================================
    // CHIPS DE CATEGORIA
    // =========================================================================

    private void configurarChips() {
        layoutChipsCategorias.removeAllViews();
        for (int i = 0; i < CATEGORIAS.length; i++) {
            final String cat   = CATEGORIAS[i];
            final String label = CATEGORIAS_LABEL[i];
            TextView chip = criarChip(label, cat.equals(categoriaAtiva));
            chip.setOnClickListener(v -> selecionarCategoria(cat));
            layoutChipsCategorias.addView(chip);
        }
    }

    private TextView criarChip(String label, boolean ativo) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        tv.setLayoutParams(lp);
        tv.setPadding(dp(14), dp(7), dp(14), dp(7));
        tv.setText(label);
        tv.setTextSize(13f);
        tv.setClickable(true);
        tv.setFocusable(true);
        aplicarEstiloChip(tv, ativo);
        return tv;
    }

    private void aplicarEstiloChip(TextView tv, boolean ativo) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(20));
        if (ativo) {
            bg.setColor(0xFF2E7D32);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), 0xFFE0E0E0);
            tv.setTextColor(0xFF555555);
            tv.setTypeface(null, Typeface.NORMAL);
        }
        tv.setBackground(bg);
    }

    private void selecionarCategoria(String cat) {
        categoriaAtiva = cat;
        // Reaplica visual de todos os chips
        for (int i = 0; i < layoutChipsCategorias.getChildCount(); i++) {
            View child = layoutChipsCategorias.getChildAt(i);
            if (child instanceof TextView) {
                aplicarEstiloChip((TextView) child, CATEGORIAS[i].equals(cat));
            }
        }
        if (adapter != null) adapter.filtrar(cat);
    }

    // =========================================================================
    // PERFIL
    // =========================================================================

    private void verificarPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String perfil = doc.exists() && doc.getString("perfil") != null
                            ? doc.getString("perfil") : "usuario";
                    boolean podeGerenciar = "projeto".equals(perfil) || "admin".equals(perfil);
                    fabNovoConteudo.setVisibility(podeGerenciar ? View.VISIBLE : View.GONE);
                    fabNovoConteudo.setOnClickListener(v -> {
                        Intent intent = new Intent(this, CriarConteudoActivity.class);
                        intent.putExtra("cidadeId",   cidadeId);
                        intent.putExtra("cidadeNome", cidadeNome);
                        startActivity(intent);
                    });
                });
    }

    // =========================================================================
    // LISTENER
    // =========================================================================

    private void iniciarListener() {
        if (listenerConteudos != null) return;
        setCarregando(true);

        Query query;

        if (cidadeId != null && !cidadeId.equals("todas")) {
            query = db.collection("conteudos")
                    .whereEqualTo("cidadeId", cidadeId)
                    .orderBy("ordem");
        } else {
            query = db.collection("conteudos")
                    .orderBy("ordem");
        }

        listenerConteudos = query.addSnapshotListener((snapshot, error) -> {
            setCarregando(false);

            if (error != null) {
                android.util.Log.e("CONTEUDOS", "Erro: " + error.getMessage());
                exibirVazio("Erro ao carregar");
                return;
            }

            if (snapshot == null || snapshot.isEmpty()) {
                exibirVazio("Nenhum conteúdo disponível");
                return;
            }

            List<DocumentSnapshot> docs = snapshot.getDocuments();
            layoutVazio.setVisibility(View.GONE);
            recyclerConteudos.setVisibility(View.VISIBLE);
            adapter = new ConteudoCidadeAdapter(docs);
            recyclerConteudos.setAdapter(adapter);
            adapter.filtrar(categoriaAtiva);
        });
    }

    private void exibirVazio(String msg) {
        recyclerConteudos.setVisibility(View.GONE);
        layoutVazio.setVisibility(View.VISIBLE);
        TextView t = layoutVazio.findViewWithTag("txtVazio");
        if (t != null) t.setText(msg);
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // ABRIR DETALHE
    // =========================================================================

    private void abrirDetalhe(DocumentSnapshot doc) {
        Intent intent = new Intent(this, DetalheConteudoActivity.class);
        intent.putExtra(DetalheConteudoActivity.EXTRA_DOC_ID,       doc.getId());
        intent.putExtra(DetalheConteudoActivity.EXTRA_TITULO,       doc.getString("titulo"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_DESCRICAO,    doc.getString("descricao"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_DATA,         doc.getString("data"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_CATEGORIA,    doc.getString("categoria"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_TEM_ANEXO,
                Boolean.TRUE.equals(doc.getBoolean("temAnexo")));
        intent.putExtra(DetalheConteudoActivity.EXTRA_ARQUIVO_URL,  doc.getString("arquivoUrl"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_ARQUIVO_NOME, doc.getString("arquivoNome"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_ARQUIVO_TIPO, doc.getString("arquivoTipo"));
        intent.putExtra(DetalheConteudoActivity.EXTRA_ARQUIVO_ID,   doc.getString("arquivoId"));
        intent.putExtra("cidadeNome", cidadeNome);
        startActivity(intent);
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    class ConteudoCidadeAdapter extends RecyclerView.Adapter<ConteudoCidadeAdapter.VH> {

        private final List<DocumentSnapshot> listaOriginal;
        private final List<DocumentSnapshot> listaFiltrada;

        ConteudoCidadeAdapter(List<DocumentSnapshot> lista) {
            this.listaOriginal = new ArrayList<>(lista);
            this.listaFiltrada = new ArrayList<>(lista);
        }

        void filtrar(String categoria) {
            listaFiltrada.clear();
            for (DocumentSnapshot doc : listaOriginal) {
                String cat = doc.getString("categoria") != null
                        ? doc.getString("categoria").toLowerCase() : "outro";
                if ("todos".equals(categoria) || cat.equals(categoria)) {
                    listaFiltrada.add(doc);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conteudo_cidade, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = listaFiltrada.get(position);

            String titulo    = doc.getString("titulo")    != null ? doc.getString("titulo")    : "";
            String data      = doc.getString("data")      != null ? doc.getString("data")      : "";
            String categoria = doc.getString("categoria") != null ? doc.getString("categoria") : "outro";
            String capaUrl   = doc.getString("capaUrl");
            String arqTipo   = doc.getString("arquivoTipo");
            Boolean temAnexo = doc.getBoolean("temAnexo");

            holder.txtTitulo.setText(titulo);
            holder.txtData.setText(data);

            // Tempo estimado
            String desc = doc.getString("descricao");
            int chars = desc != null ? desc.length() : 0;
            holder.txtTempo.setText(Math.max(1, chars / 200) + " min");

            // Badge categoria
            String badge = badgeLabel(categoria);
            int    cor   = corCategoria(categoria);
            holder.txtBadge.setText(badge);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(4f);
            badgeBg.setColor(cor);
            holder.txtBadge.setBackground(badgeBg);

            // Thumbnail
            if (capaUrl != null && !capaUrl.isEmpty()) {
                carregarImagem(holder.ivThumb, capaUrl);
            } else {
                holder.ivThumb.setBackgroundColor(corFundo(categoria));
                holder.ivThumb.setImageResource(icone(categoria));
                holder.ivThumb.setColorFilter(cor);
            }

            // Chips de arquivo
            holder.layoutChips.removeAllViews();
            if (Boolean.TRUE.equals(temAnexo) && arqTipo != null) {
                adicionarChip(holder.layoutChips, chipLabel(arqTipo), corChip(arqTipo));
            }

            holder.itemView.setOnClickListener(v -> abrirDetalhe(doc));
        }

        @Override public int getItemCount() { return listaFiltrada.size(); }

        private String badgeLabel(String cat) {
            switch (cat.toLowerCase()) {
                case "dica":     return "DICA";
                case "video":    return "VÍDEO";
                case "noticia":  return "NOTÍCIA";
                case "material": return "MATERIAL";
                default:         return "OUTRO";
            }
        }

        private int corCategoria(String cat) {
            switch (cat.toLowerCase()) {
                case "dica":     return 0xFF2E7D32;
                case "video":    return 0xFF1565C0;
                case "noticia":  return 0xFFE65100;
                case "material": return 0xFF7B1FA2;
                default:         return 0xFF37474F;
            }
        }

        private int corFundo(String cat) {
            switch (cat.toLowerCase()) {
                case "dica":     return 0xFFE8F5E9;
                case "video":    return 0xFFE3F2FD;
                case "noticia":  return 0xFFFFF3E0;
                case "material": return 0xFFF3E5F5;
                default:         return 0xFFECEFF1;
            }
        }

        private int icone(String cat) {
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

        private void adicionarChip(LinearLayout layout, String label, int cor) {
            TextView chip = new TextView(layout.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(4);
            chip.setLayoutParams(lp);
            chip.setPadding(dp(6), dp(2), dp(6), dp(2));
            chip.setText(label);
            chip.setTextSize(9f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(4f);
            bg.setColor(cor);
            chip.setBackground(bg);
            layout.addView(chip);
        }

        class VH extends RecyclerView.ViewHolder {
            TextView     txtTitulo, txtTempo, txtData, txtBadge;
            ImageView    ivThumb;
            LinearLayout layoutChips;
            VH(View v) {
                super(v);
                txtTitulo   = v.findViewById(R.id.txtTitulo);
                txtTempo    = v.findViewById(R.id.txtTempo);
                txtData     = v.findViewById(R.id.txtData);
                txtBadge    = v.findViewById(R.id.txtBadge);
                ivThumb     = v.findViewById(R.id.ivThumb);
                layoutChips = v.findViewById(R.id.layoutChips);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void carregarImagem(ImageView iv, String url) {
        new Thread(() -> {
            try {
                java.net.URL imgUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                conn.setRequestProperty("ngrok-skip-browser-warning", "true");
                conn.connect();
                android.graphics.Bitmap bmp =
                        android.graphics.BitmapFactory.decodeStream(conn.getInputStream());
                runOnUiThread(() -> { if (bmp != null) iv.setImageBitmap(bmp); });
            } catch (Exception ignored) {}
        }).start();
    }
}
