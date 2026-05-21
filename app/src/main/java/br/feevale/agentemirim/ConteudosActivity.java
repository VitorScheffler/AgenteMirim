package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ConteudosActivity extends AppCompatActivity {

    private RecyclerView         recyclerCidades;
    private LinearLayout         layoutVazio;
    private ProgressBar          progressBar;
    private FloatingActionButton fabAdicionarCidade;
    private ImageView            ivMenu;
    private DrawerLayout         drawerLayout;   // ← novo
    private NavigationView       navAdmin;        // ← novo

    private FirebaseFirestore    db;
    private ListenerRegistration listenerCidades;
    private CidadeAdapter        adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        bindViews();
        configurarAcoes();
        verificarPerfil();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (listenerCidades == null) iniciarListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerCidades != null) { listenerCidades.remove(); listenerCidades = null; }
    }

    @Override
    public void onBackPressed() {
        // Fecha o drawer ao apertar voltar, se estiver aberto
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void bindViews() {
        recyclerCidades    = findViewById(R.id.recyclerCidades);
        layoutVazio        = findViewById(R.id.layoutVazio);
        progressBar        = findViewById(R.id.progressBar);
        fabAdicionarCidade = findViewById(R.id.fabAdicionarCidade);
        ivMenu             = findViewById(R.id.ivMenu);
        drawerLayout       = findViewById(R.id.drawerLayout);   // ← novo
        navAdmin           = findViewById(R.id.navAdmin);        // ← novo

        recyclerCidades.setLayoutManager(new LinearLayoutManager(this));
        fabAdicionarCidade.setVisibility(View.GONE);
        ivMenu.setVisibility(View.GONE);
    }

    private void configurarAcoes() {
        // Card "Todas as cidades"
        findViewById(R.id.cardTodasCidades).setOnClickListener(v -> {
            Intent intent = new Intent(this, ConteudosCidadeActivity.class);
            intent.putExtra("cidadeId",        "todas");
            intent.putExtra("cidadeNome",      "Todos os conteúdos");
            intent.putExtra("cidadeDescricao", "Conteúdos de todas as cidades disponíveis.");
            startActivity(intent);
        });

        // Card "Minhas cidades"
        findViewById(R.id.cardMinhasCidades).setOnClickListener(v -> {
            // TODO: implementar seleção de cidades favoritas
        });

        // FAB admin → adicionar cidade
        fabAdicionarCidade.setOnClickListener(v ->
                startActivity(new Intent(this, CriarCidadeActivity.class)));

        // ── Hambúrguer → abre o drawer ────────────────────────────────────────
        ivMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        // ── Itens do menu lateral ─────────────────────────────────────────────
        navAdmin.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_gerenciar_usuarios) {
                startActivity(new Intent(this, GerenciarUsuariosActivity.class));
            } else if (id == R.id.action_criar_usuario) {
                startActivity(new Intent(this, CriarUsuarioActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void verificarPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String perfil = doc.exists() && doc.getString("perfil") != null
                            ? doc.getString("perfil") : "usuario";
                    boolean isAdmin = "admin".equals(perfil);
                    fabAdicionarCidade.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    ivMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                });
    }

    private void iniciarListener() {
        setCarregando(true);

        listenerCidades = db.collection("cidades")
                .orderBy("nome")
                .addSnapshotListener((snapshot, error) -> {
                    setCarregando(false);
                    if (error != null || snapshot == null) { exibirVazio(); return; }
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) { exibirVazio(); return; }

                    layoutVazio.setVisibility(View.GONE);
                    recyclerCidades.setVisibility(View.VISIBLE);
                    adapter = new CidadeAdapter(docs);
                    recyclerCidades.setAdapter(adapter);
                });
    }

    private void exibirVazio() {
        recyclerCidades.setVisibility(View.GONE);
        layoutVazio.setVisibility(View.VISIBLE);
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

    private void abrirCidade(DocumentSnapshot doc) {
        Intent intent = new Intent(this, ConteudosCidadeActivity.class);
        intent.putExtra("cidadeId",        doc.getId());
        intent.putExtra("cidadeNome",      doc.getString("nome"));
        intent.putExtra("cidadeDescricao", doc.getString("descricao"));
        intent.putExtra("cidadeImagemUrl", doc.getString("imagemUrl"));
        startActivity(intent);
    }

    // =========================================================================
    // ADAPTER DE CIDADES
    // =========================================================================

    class CidadeAdapter extends RecyclerView.Adapter<CidadeAdapter.VH> {

        private final List<DocumentSnapshot> lista;

        CidadeAdapter(List<DocumentSnapshot> lista) { this.lista = lista; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cidade, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = lista.get(position);

            String nome      = doc.getString("nome")     != null ? doc.getString("nome")     : "";
            String imagemUrl = doc.getString("imagemUrl");
            Long   qtd       = doc.getLong("qtdConteudos");

            holder.txtNome.setText(nome);
            holder.txtQtd.setText((qtd != null ? qtd : 0) + " conteúdos disponíveis");

            if (imagemUrl != null && !imagemUrl.isEmpty()) {
                carregarImagemUrl(holder.ivThumb, imagemUrl);
            } else {
                int[] cores = {0xFF2E7D32, 0xFF1565C0, 0xFFE65100, 0xFF7B1FA2, 0xFF37474F};
                holder.ivThumb.setBackgroundColor(cores[position % cores.length]);
                holder.ivThumb.setImageResource(android.R.drawable.ic_dialog_map);
                holder.ivThumb.setColorFilter(0x44FFFFFF);
            }

            holder.itemView.setOnClickListener(v -> abrirCidade(doc));
        }

        @Override public int getItemCount() { return lista.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView  txtNome, txtQtd;
            ImageView ivThumb;
            VH(View v) {
                super(v);
                txtNome = v.findViewById(R.id.txtNomeCidade);
                txtQtd  = v.findViewById(R.id.txtQtdConteudos);
                ivThumb = v.findViewById(R.id.ivCidadeThumb);
            }
        }
    }

    private void carregarImagemUrl(ImageView iv, String url) {
        new Thread(() -> {
            try {
                java.net.URL imgUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                conn.setRequestProperty("ngrok-skip-browser-warning", "true");
                conn.connect();
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(conn.getInputStream());
                runOnUiThread(() -> { if (bmp != null) iv.setImageBitmap(bmp); });
            } catch (Exception ignored) {}
        }).start();
    }
}