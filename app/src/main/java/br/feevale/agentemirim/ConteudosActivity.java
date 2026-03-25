package br.feevale.agentemirim;

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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ConteudosActivity extends AppCompatActivity {

    private RecyclerView recyclerConteudos;
    private LinearLayout layoutVazio;
    private ProgressBar  progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conteudos);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerConteudos = findViewById(R.id.recyclerConteudos);
        layoutVazio       = findViewById(R.id.layoutVazio);
        progressBar       = findViewById(R.id.progressBar);

        carregarConteudos();
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
                        // Exibe dados de exemplo enquanto não há conteúdo no Firestore
                        exibirConteudosExemplo();
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
                    // Em caso de falha, exibe exemplos
                    exibirConteudosExemplo();
                });
    }

    /**
     * Exibe uma lista de conteúdos estáticos de exemplo,
     * útil durante o desenvolvimento ou quando o Firestore está vazio.
     */
    private void exibirConteudosExemplo() {
        List<ConteudoItem> exemplos = new ArrayList<>();
        exemplos.add(new ConteudoItem(
                "Organização Pessoal",
                "Pensamentos e mensagens para ajudar a organizar seu dinheiro",
                "10/03/2026",
                COR_LARANJA,
                ICONE_ORGANIZAR
        ));
        exemplos.add(new ConteudoItem(
                "Educação Financeira",
                "Confira os avisos e mensagens importantes",
                "10/03/2026",
                COR_VERDE,
                ICONE_FINANCEIRO
        ));
        exemplos.add(new ConteudoItem(
                "Dicas de Estudo",
                "Deu cada tipo de nível. Chauteba batnced do ungenbs",
                "10/03/2026",
                COR_AMARELO,
                ICONE_ESTUDO
        ));
        exemplos.add(new ConteudoItem(
                "Alimentação Saudável",
                "As opções da sua house?",
                "10/03/2026",
                COR_VERMELHO,
                ICONE_SAUDE
        ));

        ConteudoExemploAdapter adapter = new ConteudoExemploAdapter(exemplos);
        recyclerConteudos.setLayoutManager(new LinearLayoutManager(this));
        recyclerConteudos.setAdapter(adapter);
        layoutVazio.setVisibility(View.GONE);
        recyclerConteudos.setVisibility(View.VISIBLE);
    }

    // ── Cores dos ícones (em linha com o design da imagem) ────────────────────

    private static final int COR_LARANJA = 0xFFE87722;
    private static final int COR_VERDE   = 0xFF4CAF50;
    private static final int COR_AMARELO = 0xFFF9A825;
    private static final int COR_VERMELHO= 0xFFE53935;

    // IDs de ícones padrão do Android (substitua por drawables próprios se desejar)
    private static final int ICONE_ORGANIZAR  = android.R.drawable.ic_menu_sort_by_size;
    private static final int ICONE_FINANCEIRO = android.R.drawable.ic_menu_send;
    private static final int ICONE_ESTUDO     = android.R.drawable.ic_menu_help;
    private static final int ICONE_SAUDE      = android.R.drawable.ic_menu_myplaces;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

    // ── Modelo simples para exemplos ──────────────────────────────────────────

    static class ConteudoItem {
        final String titulo, descricao, data;
        final int    cor, icone;

        ConteudoItem(String titulo, String descricao, String data, int cor, int icone) {
            this.titulo   = titulo;
            this.descricao= descricao;
            this.data     = data;
            this.cor      = cor;
            this.icone    = icone;
        }
    }

    // ── Adapter para dados do Firestore ───────────────────────────────────────

    static class ConteudoAdapter extends RecyclerView.Adapter<ConteudoAdapter.VH> {

        private static final int[] CORES = {
                0xFFE87722, 0xFF4CAF50, 0xFFF9A825, 0xFFE53935,
                0xFF1976D2, 0xFF7B1FA2, 0xFF00838F
        };

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

            String titulo    = doc.getString("titulo")    != null ? doc.getString("titulo")    : "(sem título)";
            String descricao = doc.getString("descricao") != null ? doc.getString("descricao") : "";
            String data      = doc.getString("data")      != null ? doc.getString("data")      : "";

            holder.txtTitulo.setText(titulo);
            holder.txtDescricao.setText(descricao);
            holder.txtData.setText(data);

            // Alterna cor ciclicamente
            int cor = CORES[position % CORES.length];
            holder.frameIcone.setBackgroundColor(0); // reset
            holder.frameIcone.getBackground(); // garante layout
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(cor);
            holder.frameIcone.setBackground(shape);
        }

        @Override
        public int getItemCount() { return lista.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView   txtTitulo, txtDescricao, txtData;
            FrameLayout frameIcone;
            VH(View v) {
                super(v);
                txtTitulo    = v.findViewById(R.id.txtTitulo);
                txtDescricao = v.findViewById(R.id.txtDescricao);
                txtData      = v.findViewById(R.id.txtData);
                frameIcone   = v.findViewById(R.id.frameIcone);
            }
        }
    }

    // ── Adapter para dados de exemplo ─────────────────────────────────────────

    static class ConteudoExemploAdapter extends RecyclerView.Adapter<ConteudoExemploAdapter.VH> {

        private final List<ConteudoItem> lista;

        ConteudoExemploAdapter(List<ConteudoItem> lista) {
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
            ConteudoItem item = lista.get(position);

            holder.txtTitulo.setText(item.titulo);
            holder.txtDescricao.setText(item.descricao);
            holder.txtData.setText(item.data);
            holder.ivIcone.setImageResource(item.icone);

            // Aplica a cor com cantos arredondados
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(32f);
            shape.setColor(item.cor);
            holder.frameIcone.setBackground(shape);
        }

        @Override
        public int getItemCount() { return lista.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView    txtTitulo, txtDescricao, txtData;
            FrameLayout frameIcone;
            ImageView   ivIcone;
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
