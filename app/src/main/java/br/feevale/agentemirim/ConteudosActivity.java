package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ConteudosActivity extends AppCompatActivity {

    private RecyclerView         recyclerConteudos;
    private LinearLayout         layoutVazio;
    private ProgressBar          progressBar;
    private FloatingActionButton fabNovoConteudo;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerConteudos = findViewById(R.id.recyclerConteudos);
        layoutVazio       = findViewById(R.id.layoutVazio);
        progressBar       = findViewById(R.id.progressBar);
        fabNovoConteudo   = findViewById(R.id.fabNovoConteudo);

        fabNovoConteudo.setVisibility(View.GONE);
<<<<<<< HEAD
    }

    @Override
    protected void onResume() {
        super.onResume();
=======

>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
        verificarPerfilECarregar();
    }

    // ── Verifica perfil do usuário logado ─────────────────────────────────────

    private void verificarPerfilECarregar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        setCarregando(true);

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String perfil = doc.exists() && doc.getString("perfil") != null
                            ? doc.getString("perfil") : "usuario";

                    boolean podeGerenciar = "projeto".equals(perfil) || "admin".equals(perfil);
                    fabNovoConteudo.setVisibility(podeGerenciar ? View.VISIBLE : View.GONE);

                    fabNovoConteudo.setOnClickListener(v ->
                            startActivity(new Intent(this, CriarConteudoActivity.class)));

                    carregarConteudos();
                })
                .addOnFailureListener(e -> carregarConteudos());
    }

    // ── Carregar conteúdos do Firestore ───────────────────────────────────────

    private void carregarConteudos() {
        setCarregando(true);

        db.collection("conteudos")
                .orderBy("ordem")
                .get()
                .addOnSuccessListener(query -> {
                    setCarregando(false);
                    List<DocumentSnapshot> docs = query.getDocuments();

                    if (docs.isEmpty()) {
<<<<<<< HEAD
                        layoutVazio.setVisibility(View.VISIBLE);
                        recyclerConteudos.setVisibility(View.GONE);
=======
                        exibirConteudosExemplo();
>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
                        return;
                    }

                    ConteudoAdapter adapter = new ConteudoAdapter(docs);
                    recyclerConteudos.setLayoutManager(new LinearLayoutManager(this));
                    recyclerConteudos.setAdapter(adapter);
                    layoutVazio.setVisibility(View.GONE);
                    recyclerConteudos.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
<<<<<<< HEAD
                    layoutVazio.setVisibility(View.VISIBLE);
                    recyclerConteudos.setVisibility(View.GONE);
                });
    }

=======
                    exibirConteudosExemplo();
                });
    }

    private void exibirConteudosExemplo() {
        List<ConteudoItem> exemplos = new ArrayList<>();
        exemplos.add(new ConteudoItem("Organização Pessoal",
                "Pensamentos e mensagens para ajudar a organizar seu dinheiro",
                "10/03/2026", COR_LARANJA, ICONE_ORGANIZAR));
        exemplos.add(new ConteudoItem("Educação Financeira",
                "Confira os avisos e mensagens importantes",
                "10/03/2026", COR_VERDE, ICONE_FINANCEIRO));
        exemplos.add(new ConteudoItem("Dicas de Estudo",
                "Conteúdo para todos os níveis de alunos.",
                "10/03/2026", COR_AMARELO, ICONE_ESTUDO));
        exemplos.add(new ConteudoItem("Alimentação Saudável",
                "As melhores opções para uma vida mais saudável.",
                "10/03/2026", COR_VERMELHO, ICONE_SAUDE));

        ConteudoExemploAdapter adapter = new ConteudoExemploAdapter(exemplos);
        recyclerConteudos.setLayoutManager(new LinearLayoutManager(this));
        recyclerConteudos.setAdapter(adapter);
        layoutVazio.setVisibility(View.GONE);
        recyclerConteudos.setVisibility(View.VISIBLE);
    }

    // ── Cores e ícones ────────────────────────────────────────────────────────

    static final int COR_LARANJA = 0xFFE87722;
    static final int COR_VERDE   = 0xFF4CAF50;
    static final int COR_AMARELO = 0xFFF9A825;
    static final int COR_VERMELHO= 0xFFE53935;

    static final int ICONE_ORGANIZAR  = android.R.drawable.ic_menu_sort_by_size;
    static final int ICONE_FINANCEIRO = android.R.drawable.ic_menu_send;
    static final int ICONE_ESTUDO     = android.R.drawable.ic_menu_help;
    static final int ICONE_SAUDE      = android.R.drawable.ic_menu_myplaces;

>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

<<<<<<< HEAD
=======
    // ── Modelo ────────────────────────────────────────────────────────────────

    static class ConteudoItem {
        final String titulo, descricao, data;
        final int    cor, icone;
        ConteudoItem(String t, String d, String dt, int c, int i) {
            titulo = t; descricao = d; data = dt; cor = c; icone = i;
        }
    }

>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
    // ── Adapter Firestore ─────────────────────────────────────────────────────

    static class ConteudoAdapter extends RecyclerView.Adapter<ConteudoAdapter.VH> {

        private static final int[] CORES = {
                0xFFE87722, 0xFF4CAF50, 0xFFF9A825, 0xFFE53935,
                0xFF1976D2, 0xFF7B1FA2, 0xFF00838F
        };
<<<<<<< HEAD

        private final List<DocumentSnapshot> lista;

        ConteudoAdapter(List<DocumentSnapshot> lista) {
            this.lista = lista;
        }
=======
        private final List<DocumentSnapshot> lista;
        ConteudoAdapter(List<DocumentSnapshot> lista) { this.lista = lista; }
>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conteudo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = lista.get(position);
<<<<<<< HEAD

=======
>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
            holder.txtTitulo.setText(doc.getString("titulo") != null ? doc.getString("titulo") : "");
            holder.txtDescricao.setText(doc.getString("descricao") != null ? doc.getString("descricao") : "");
            holder.txtData.setText(doc.getString("data") != null ? doc.getString("data") : "");

<<<<<<< HEAD
            Long cor = doc.getLong("cor");
            Long icone = doc.getLong("icone");

            holder.ivIcone.setImageResource(
                    icone != null ? icone.intValue() : android.R.drawable.ic_menu_help
            );

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(cor != null ? cor.intValue() : CORES[position % CORES.length]);

            holder.frameIcone.setBackground(shape);
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }
=======
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(CORES[position % CORES.length]);
            holder.frameIcone.setBackground(shape);
        }

        @Override public int getItemCount() { return lista.size(); }
>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55

        static class VH extends RecyclerView.ViewHolder {
            TextView txtTitulo, txtDescricao, txtData;
            FrameLayout frameIcone;
<<<<<<< HEAD
            ImageView ivIcone;

=======
            VH(View v) {
                super(v);
                txtTitulo    = v.findViewById(R.id.txtTitulo);
                txtDescricao = v.findViewById(R.id.txtDescricao);
                txtData      = v.findViewById(R.id.txtData);
                frameIcone   = v.findViewById(R.id.frameIcone);
            }
        }
    }

    // ── Adapter Exemplo ───────────────────────────────────────────────────────

    static class ConteudoExemploAdapter extends RecyclerView.Adapter<ConteudoExemploAdapter.VH> {

        private final List<ConteudoItem> lista;
        ConteudoExemploAdapter(List<ConteudoItem> lista) { this.lista = lista; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conteudo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ConteudoItem item = lista.get(position);
            holder.txtTitulo.setText(item.titulo);
            holder.txtDescricao.setText(item.descricao);
            holder.txtData.setText(item.data);
            holder.ivIcone.setImageResource(item.icone);

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(item.cor);
            holder.frameIcone.setBackground(shape);
        }

        @Override public int getItemCount() { return lista.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView txtTitulo, txtDescricao, txtData;
            FrameLayout frameIcone;
            ImageView   ivIcone;
>>>>>>> 9d4cec7867b26353fe308d7a55546f6726106f55
            VH(View v) {
                super(v);
                txtTitulo    = v.findViewById(R.id.txtTitulo);
                txtDescricao = v.findViewById(R.id.txtDescricao);
                txtData      = v.findViewById(R.id.txtData);
                frameIcone   = v.findViewById(R.id.frameIcone);
                ivIcone      = v.findViewById(R.id.ivIcone);
            }
        }
    }
}
