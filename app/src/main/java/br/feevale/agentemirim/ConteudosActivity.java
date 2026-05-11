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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ConteudosActivity extends AppCompatActivity {

    private RecyclerView recyclerConteudos;
    private LinearLayout layoutVazio;
    private ProgressBar progressBar;
    private FloatingActionButton fabNovoConteudo;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerConteudos = findViewById(R.id.recyclerConteudos);
        layoutVazio = findViewById(R.id.layoutVazio);
        progressBar = findViewById(R.id.progressBar);
        fabNovoConteudo = findViewById(R.id.fabNovoConteudo);

        fabNovoConteudo.setVisibility(View.GONE);

        configurarNavegacao();
    }

    private void configurarNavegacao() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_conteudos);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_conteudos) {
                return true;
            } else if (id == R.id.navigation_perfil) {
                startActivity(new Intent(this, PerfilUsuarioActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarPerfilECarregar();
    }

    private void verificarPerfilECarregar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

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

    private void carregarConteudos() {
        setCarregando(true);

        db.collection("conteudos")
                .orderBy("ordem")
                .get()
                .addOnSuccessListener(query -> {
                    setCarregando(false);

                    List<DocumentSnapshot> docs = query.getDocuments();

                    if (docs.isEmpty()) {
                        layoutVazio.setVisibility(View.VISIBLE);
                        recyclerConteudos.setVisibility(View.GONE);
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
                    layoutVazio.setVisibility(View.VISIBLE);
                    recyclerConteudos.setVisibility(View.GONE);
                });
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────
    // ADAPTER FIRESTORE
    // ─────────────────────────────

    static class ConteudoAdapter extends RecyclerView.Adapter<ConteudoAdapter.VH> {

        private final List<DocumentSnapshot> lista;

        ConteudoAdapter(List<DocumentSnapshot> lista) {
            this.lista = lista;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conteudo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = lista.get(position);

            holder.txtTitulo.setText(doc.getString("titulo"));
            holder.txtDescricao.setText(doc.getString("descricao"));
            holder.txtData.setText(doc.getString("data"));

            Long cor = doc.getLong("cor");
            Long icone = doc.getLong("icone");

            holder.ivIcone.setImageResource(
                    icone != null ? icone.intValue() : android.R.drawable.ic_menu_help
            );

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(cor != null ? cor.intValue() : 0xFFE87722);

            holder.frameIcone.setBackground(shape);
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            TextView txtTitulo, txtDescricao, txtData;
            FrameLayout frameIcone;
            ImageView ivIcone;

            VH(View v) {
                super(v);
                txtTitulo = v.findViewById(R.id.txtTitulo);
                txtDescricao = v.findViewById(R.id.txtDescricao);
                txtData = v.findViewById(R.id.txtData);
                frameIcone = v.findViewById(R.id.frameIcone);
                ivIcone = v.findViewById(R.id.ivIcone);
            }
        }
    }
}