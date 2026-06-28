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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class ConteudosActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView         recyclerCidades;
    private LinearLayout         layoutVazio;
    private ProgressBar          progressBar;
    private FloatingActionButton fabAdicionarCidade;
    private ImageView            ivMenu;
    private DrawerLayout         drawerLayout;
    private NavigationView       navAdmin;
    private TextView             txtBoasVindas; // ← NOVO

    // ── Dados ─────────────────────────────────────────────────────────────────
    private boolean      isAdmin          = false;
    private boolean      modoFavoritos    = false;
    private Set<String>  favoritoIds      = new HashSet<>();

    // ── Firebase / Manager ────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private FavoritosManager     favoritosManager;
    private ListenerRegistration listenerCidades;
    private ListenerRegistration listenerFavoritos;
    private CidadeAdapter        adapter;

    private static final String AUTH_TOKEN = "-R,V*ox+>K,0o76MH=XYNG9.sRz@xLLR";

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos);

        db               = FirebaseFirestore.getInstance();
        favoritosManager = new FavoritosManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        bindViews();
        configurarAcoes();
        verificarPerfil();
        iniciarListenerFavoritos();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (listenerCidades == null) iniciarListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza saudação e visibilidade do menu admin ao voltar de outra tela
        atualizarBoasVindas();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerCidades   != null) { listenerCidades.remove();   listenerCidades   = null; }
        if (listenerFavoritos != null) { listenerFavoritos.remove(); listenerFavoritos = null; }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Tela raiz — minimiza o app em vez de fechar
            moveTaskToBack(true);
        }
    }

    // =========================================================================
    // SETUP
    // =========================================================================

    private void bindViews() {
        recyclerCidades    = findViewById(R.id.recyclerCidades);
        layoutVazio        = findViewById(R.id.layoutVazio);
        progressBar        = findViewById(R.id.progressBar);
        fabAdicionarCidade = findViewById(R.id.fabAdicionarCidade);
        ivMenu             = findViewById(R.id.ivMenu);
        drawerLayout       = findViewById(R.id.drawerLayout);
        navAdmin           = findViewById(R.id.navAdmin);
        txtBoasVindas      = findViewById(R.id.txtBoasVindas); // ← NOVO

        recyclerCidades.setLayoutManager(new LinearLayoutManager(this));
        fabAdicionarCidade.setVisibility(View.GONE);
        ivMenu.setVisibility(View.GONE);
    }

    private void configurarAcoes() {
        findViewById(R.id.ivUsuario).setOnClickListener(this::exibirMenuUsuario);

        findViewById(R.id.cardTodasCidades).setOnClickListener(v -> {
            modoFavoritos = false;
            atualizarEstadoCards();
            if (adapter != null) adapter.setFiltroFavoritos(false, favoritoIds);
        });

        findViewById(R.id.cardMinhasCidades).setOnClickListener(v -> {
            modoFavoritos = true;
            atualizarEstadoCards();
            if (adapter != null) adapter.setFiltroFavoritos(true, favoritoIds);
        });

        fabAdicionarCidade.setOnClickListener(v ->
                startActivity(new Intent(this, CriarCidadeActivity.class)));

        ivMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navAdmin.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_gerenciar_usuarios)
                startActivity(new Intent(this, GerenciarUsuariosActivity.class));
            else if (id == R.id.action_criar_usuario)
                startActivity(new Intent(this, CriarUsuarioActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void atualizarEstadoCards() {
        findViewById(R.id.cardTodasCidades).setAlpha(modoFavoritos ? 0.55f : 1f);
        findViewById(R.id.cardMinhasCidades).setAlpha(modoFavoritos ? 1f : 0.55f);
    }

    // =========================================================================
    // BOAS-VINDAS  (absorvido da MainActivity)
    // =========================================================================

    private void atualizarBoasVindas() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (txtBoasVindas != null) {
                            String nome = doc.getString("nome");
                            txtBoasVindas.setText((nome != null && !nome.isEmpty())
                                    ? "Olá, " + nome.split(" ")[0] + "! 👋"
                                    : "Olá, Agente Mirim! 👋");
                        }
                        isAdmin = doc.exists() && "admin".equals(doc.getString("perfil"));
                        fabAdicionarCidade.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        ivMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    });
        } else {
            if (txtBoasVindas != null) txtBoasVindas.setText("Olá, Agente Mirim! 👋");
            isAdmin = false;
            fabAdicionarCidade.setVisibility(View.GONE);
            ivMenu.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // PERFIL  (mantido para configurar FAB/menu no onCreate)
    // =========================================================================

    private void verificarPerfil() {
        // Delega para atualizarBoasVindas que já faz tudo
        atualizarBoasVindas();
    }

    // =========================================================================
    // FAVORITOS
    // =========================================================================

    private void iniciarListenerFavoritos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            listenerFavoritos = db.collection("usuarios")
                    .document(user.getUid())
                    .collection("favoritos")
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null) return;
                        favoritoIds.clear();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            favoritoIds.add(doc.getId());
                        }
                        if (adapter != null) {
                            adapter.atualizarFavoritos(favoritoIds);
                            if (modoFavoritos) adapter.setFiltroFavoritos(true, favoritoIds);
                        }
                    });
        } else {
            favoritoIds.clear();
            favoritoIds.addAll(favoritosManager.lerFavoritosLocais());
            if (adapter != null) adapter.atualizarFavoritos(favoritoIds);
        }
    }

    private void toggleFavorito(String cidadeId, boolean eraFavorito) {
        favoritosManager.toggle(cidadeId, eraFavorito, null);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (eraFavorito) favoritoIds.remove(cidadeId);
            else             favoritoIds.add(cidadeId);
            if (adapter != null) {
                adapter.atualizarFavoritos(favoritoIds);
                if (modoFavoritos) adapter.setFiltroFavoritos(true, favoritoIds);
            }
        }
    }

    // =========================================================================
    // MENU USUÁRIO
    // =========================================================================

    private void exibirMenuUsuario(View anchorView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        PopupMenu popup = new PopupMenu(this, anchorView);

        if (user == null) {
            popup.getMenu().add(0, 1001, 0, "Entrar");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1001) {
                    startActivity(new Intent(this, LoginActivity.class));
                    return true;
                }
                return false;
            });
        } else {
            popup.getMenuInflater().inflate(R.menu.menu_usuario, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_perfil) {
                    startActivity(new Intent(this, PerfilUsuarioActivity.class));
                    return true;
                }
                if (id == R.id.action_sair) {
                    confirmarLogout();
                    return true;
                }
                return false;
            });
        }
        popup.show();
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta")
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton("Sair", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();

                    // Limpa estado
                    favoritoIds.clear();
                    modoFavoritos = false;
                    isAdmin = false;
                    atualizarEstadoCards();

                    // Recarrega favoritos locais
                    favoritoIds.addAll(favoritosManager.lerFavoritosLocais());
                    if (adapter != null) adapter.atualizarFavoritos(favoritoIds);

                    // Remove listener de favoritos do Firestore
                    if (listenerFavoritos != null) {
                        listenerFavoritos.remove();
                        listenerFavoritos = null;
                    }

                    // Atualiza boas-vindas e visibilidade do menu
                    atualizarBoasVindas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // LISTENER DE CIDADES
    // =========================================================================

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

                    adapter.atualizarFavoritos(favoritoIds);
                    if (modoFavoritos) adapter.setFiltroFavoritos(true, favoritoIds);
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

        private final List<DocumentSnapshot> listaOriginal;
        private final List<DocumentSnapshot> listaExibida;
        private Set<String> favs               = new HashSet<>();
        private boolean     filtrandoFavoritos = false;

        CidadeAdapter(List<DocumentSnapshot> lista) {
            this.listaOriginal = new ArrayList<>(lista);
            this.listaExibida  = new ArrayList<>(lista);
        }

        void atualizarFavoritos(Set<String> novosFavs) {
            this.favs = new HashSet<>(novosFavs);
            notifyDataSetChanged();
        }

        void setFiltroFavoritos(boolean ativo, Set<String> novosFavs) {
            this.filtrandoFavoritos = ativo;
            this.favs = new HashSet<>(novosFavs);
            aplicarFiltro();
        }

        private void aplicarFiltro() {
            listaExibida.clear();

            if (filtrandoFavoritos) {
                for (DocumentSnapshot doc : listaOriginal) {
                    if (favs.contains(doc.getId())) listaExibida.add(doc);
                }
            } else {
                listaExibida.addAll(listaOriginal);
            }

            if (listaExibida.isEmpty() && filtrandoFavoritos) {
                recyclerCidades.setVisibility(View.GONE);
                layoutVazio.setVisibility(View.VISIBLE);
                if (layoutVazio.findViewWithTag("txtVazioFav") == null) {
                    TextView tv = new TextView(ConteudosActivity.this);
                    tv.setTag("txtVazioFav");
                    tv.setText("Você ainda não tem cidades favoritas.\nToque na ⭐ em qualquer cidade para salvar.");
                    tv.setTextSize(13f);
                    tv.setTextColor(0xFFBDBDBD);
                    tv.setGravity(android.view.Gravity.CENTER);
                    tv.setPadding(48, 16, 48, 0);
                    ((LinearLayout) layoutVazio).addView(tv);
                }
            } else {
                View tvFav = layoutVazio.findViewWithTag("txtVazioFav");
                if (tvFav != null) ((LinearLayout) layoutVazio).removeView(tvFav);
                layoutVazio.setVisibility(View.GONE);
                recyclerCidades.setVisibility(View.VISIBLE);
            }

            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cidade, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = listaExibida.get(position);

            String nome      = doc.getString("nome")      != null ? doc.getString("nome")      : "";
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

            boolean isFav = favs.contains(doc.getId());
            holder.ivFavorito.setImageResource(
                    isFav ? android.R.drawable.btn_star_big_on
                            : android.R.drawable.btn_star_big_off);

            holder.ivFavorito.setOnClickListener(v ->
                    toggleFavorito(doc.getId(), favs.contains(doc.getId())));

            holder.itemView.setOnClickListener(v -> abrirCidade(doc));
        }

        @Override public int getItemCount() { return listaExibida.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView  txtNome, txtQtd;
            ImageView ivThumb, ivFavorito;
            VH(View v) {
                super(v);
                txtNome    = v.findViewById(R.id.txtNomeCidade);
                txtQtd     = v.findViewById(R.id.txtQtdConteudos);
                ivThumb    = v.findViewById(R.id.ivCidadeThumb);
                ivFavorito = v.findViewById(R.id.ivFavorito);
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void carregarImagemUrl(ImageView iv, String url) {
        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build());
        Glide.with(this).load(glideUrl).into(iv);
    }
}