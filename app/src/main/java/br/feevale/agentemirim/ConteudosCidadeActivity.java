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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

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
    private View                 btnMenuAdmin;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private String cidadeId, cidadeNome, cidadeDescricao, cidadeImagemUrl;
    private String categoriaAtiva = "todos";
    private boolean isAdmin = false;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerConteudos;
    private ConteudoCidadeAdapter adapter;

    // ── Categorias ────────────────────────────────────────────────────────────
    private static final String[] CATEGORIAS       = {"todos","dica","video","noticia","material"};
    private static final String[] CATEGORIAS_LABEL = {"Todos","Enchentes","Deslizamentos","Tempestades","Outros"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos_cidade);

        db = FirebaseFirestore.getInstance();

        cidadeId        = getIntent().getStringExtra("cidadeId");
        cidadeNome      = getIntent().getStringExtra("cidadeNome");
        cidadeImagemUrl = getIntent().getStringExtra("cidadeImagemUrl");
        cidadeDescricao = getIntent().getStringExtra("cidadeDescricao");

        bindViews();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        txtNomeCidadeTitulo.setText(cidadeNome     != null ? cidadeNome     : "");
        txtDescricaoCidade.setText(cidadeDescricao != null ? cidadeDescricao : "");

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
        if (listenerConteudos != null) {
            listenerConteudos.remove();
            listenerConteudos = null; // ← garante que onStart() vai recriar
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reinicia o listener sempre que a Activity volta ao primeiro plano
        iniciarListener();
    }

    private void bindViews() {
        ivCapaCidade          = findViewById(R.id.ivCapaCidade);
        txtNomeCidadeTitulo   = findViewById(R.id.txtNomeCidadeTitulo);
        txtDescricaoCidade    = findViewById(R.id.txtDescricaoCidade);
        layoutChipsCategorias = findViewById(R.id.layoutChipsCategorias);
        recyclerConteudos     = findViewById(R.id.recyclerConteudos);
        layoutVazio           = findViewById(R.id.layoutVazio);
        progressBar           = findViewById(R.id.progressBar);
        fabNovoConteudo       = findViewById(R.id.fabNovoConteudo);
        btnMenuAdmin          = findViewById(R.id.btnMenuAdmin);

        fabNovoConteudo.setVisibility(View.GONE);
        btnMenuAdmin.setVisibility(View.GONE);
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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
                    isAdmin = "admin".equals(perfil);

                    fabNovoConteudo.setVisibility(podeGerenciar ? View.VISIBLE : View.GONE);
                    fabNovoConteudo.setOnClickListener(v -> {
                        Intent intent = new Intent(this, CriarConteudoActivity.class);
                        intent.putExtra("cidadeId",   cidadeId);
                        intent.putExtra("cidadeNome", cidadeNome);
                        startActivity(intent);
                    });

                    btnMenuAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    btnMenuAdmin.setOnClickListener(this::exibirMenuAdmin);
                });
    }

    // =========================================================================
    // MENU ADMIN
    // =========================================================================

    private void exibirMenuAdmin(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Editar cidade");
        popup.getMenu().add(0, 2, 1, "Apagar cidade");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.view.MenuItem itemApagar = popup.getMenu().findItem(2);
            android.text.SpannableString span = new android.text.SpannableString(itemApagar.getTitle());
            span.setSpan(new android.text.style.ForegroundColorSpan(0xFFD32F2F), 0, span.length(), 0);
            itemApagar.setTitle(span);
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { editarCidade();          return true; }
            if (item.getItemId() == 2) { confirmarApagarCidade(); return true; }
            return false;
        });

        popup.show();
    }

    private void editarCidade() {
        Intent intent = new Intent(this, CriarCidadeActivity.class);
        intent.putExtra("cidadeId",        cidadeId);
        intent.putExtra("cidadeNome",      cidadeNome);
        intent.putExtra("cidadeDescricao", cidadeDescricao);
        intent.putExtra("cidadeImagemUrl", cidadeImagemUrl);
        intent.putExtra("modoEdicao",      true);
        startActivityForResult(intent, 100);
    }

    private void confirmarApagarCidade() {
        new AlertDialog.Builder(this)
                .setTitle("Apagar cidade")
                .setMessage("Tem certeza que deseja apagar \"" + cidadeNome + "\"?\n\nEsta ação não pode ser desfeita.")
                .setPositiveButton("Apagar", (dialog, which) -> apagarCidade())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarCidade() {
        if (cidadeId == null) return;
        setCarregando(true);
        db.collection("cidades").document(cidadeId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Cidade apagada com sucesso", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    Toast.makeText(this, "Erro ao apagar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            cidadeNome      = data.getStringExtra("cidadeNome");
            cidadeDescricao = data.getStringExtra("cidadeDescricao");
            cidadeImagemUrl = data.getStringExtra("cidadeImagemUrl");

            txtNomeCidadeTitulo.setText(cidadeNome     != null ? cidadeNome     : "");
            txtDescricaoCidade.setText(cidadeDescricao != null ? cidadeDescricao : "");
            if (cidadeImagemUrl != null && !cidadeImagemUrl.isEmpty()) {
                carregarImagem(ivCapaCidade, cidadeImagemUrl);
            }
        }
    }

    // =========================================================================
    // LISTENER DE CONTEÚDOS
    // =========================================================================

    private void iniciarListener() {
        // ← REMOVIDO: if (listenerConteudos != null) return;
        setCarregando(true);

        Query query = (cidadeId != null && !cidadeId.equals("todas"))
                ? db.collection("conteudos").whereEqualTo("cidadeId", cidadeId).orderBy("ordem")
                : db.collection("conteudos").orderBy("ordem");

        listenerConteudos = query.addSnapshotListener((snapshot, error) -> {
            setCarregando(false);
            if (error != null) { exibirVazio("Erro ao carregar"); return; }
            if (snapshot == null || snapshot.isEmpty()) { exibirVazio("Nenhum conteúdo disponível"); return; }

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
        intent.putExtra(DetalheConteudoActivity.EXTRA_CAPA_URL,     doc.getString("capaUrl")); // ← FIX
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
                if ("todos".equals(categoria) || cat.equals(categoria)) listaFiltrada.add(doc);
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

            String desc = doc.getString("descricao");
            int chars = desc != null ? desc.length() : 0;
            holder.txtTempo.setText(Math.max(1, chars / 200) + " min");

            String badge = badgeLabel(categoria);
            int    cor   = corCategoria(categoria);
            holder.txtBadge.setText(badge);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(4f);
            badgeBg.setColor(cor);
            holder.txtBadge.setBackground(badgeBg);

            if (capaUrl != null && !capaUrl.isEmpty()) {
                carregarImagem(holder.ivThumb, capaUrl);
            } else {
                holder.ivThumb.setBackgroundColor(corFundo(categoria));
                holder.ivThumb.setImageResource(icone(categoria));
                holder.ivThumb.setColorFilter(cor);
            }

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

    // =========================================================================
    // HELPERS
    // =========================================================================

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final String AUTH_TOKEN = "-R,V*ox+>K,0o76MH=XYNG9.sRz@xLLR";

    private void carregarImagem(ImageView iv, String url) {
        if (url == null || url.isEmpty()) return;
        url = url.trim();
        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build());

        Glide.with(this)
                .load(glideUrl)
                .into(iv);
    }
}