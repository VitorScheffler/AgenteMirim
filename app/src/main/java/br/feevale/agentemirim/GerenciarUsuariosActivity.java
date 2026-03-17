package br.feevale.agentemirim;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GerenciarUsuariosActivity extends AppCompatActivity {

    private RecyclerView      recyclerUsuarios;
    private LinearLayout      layoutVazio;
    private ProgressBar       progressBar;
    private TextInputEditText editBusca;
    private TextView          txtContador;

    private FirebaseFirestore db;
    private UsuarioAdapter    adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_usuarios);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);
        layoutVazio      = findViewById(R.id.layoutVazio);
        progressBar      = findViewById(R.id.progressBar);
        editBusca        = findViewById(R.id.editBusca);
        txtContador      = findViewById(R.id.txtContador);

        // Verifica se é admin antes de carregar
        verificarAcessoECarregar();

        // Filtro de busca em tempo real
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                if (adapter != null) adapter.filtrar(s.toString());
            }
        });
    }

    // ── Verificação de acesso ─────────────────────────────────────────────────

    private void verificarAcessoECarregar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        setCarregando(true);

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = doc.exists()
                            && "admin".equals(doc.getString("perfil"));

                    if (!isAdmin) {
                        setCarregando(false);
                        new AlertDialog.Builder(this)
                                .setTitle("Acesso negado")
                                .setMessage("Somente administradores podem gerenciar usuários.")
                                .setPositiveButton("OK", (d, w) -> finish())
                                .setCancelable(false)
                                .show();
                    } else {
                        carregarUsuarios();
                    }
                })
                .addOnFailureListener(e -> { setCarregando(false); finish(); });
    }

    // ── Carregar usuários do Firestore ────────────────────────────────────────

    private void carregarUsuarios() {
        setCarregando(true);

        db.collection("usuarios")
                .get()
                .addOnSuccessListener(query -> {
                    setCarregando(false);

                    List<DocumentSnapshot> docs = query.getDocuments();

                    if (docs.isEmpty()) {
                        layoutVazio.setVisibility(View.VISIBLE);
                        recyclerUsuarios.setVisibility(View.GONE);
                        txtContador.setText("0 usuários");
                        return;
                    }

                    txtContador.setText(docs.size() + " usuário(s) cadastrado(s)");

                    adapter = new UsuarioAdapter(docs, this::abrirDialogEditar);
                    recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));
                    recyclerUsuarios.setAdapter(adapter);
                    layoutVazio.setVisibility(View.GONE);
                    recyclerUsuarios.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> setCarregando(false));
    }

    // ── Dialog editar usuário ─────────────────────────────────────────────────

    private void abrirDialogEditar(DocumentSnapshot doc) {
        String uid   = doc.getId();
        String nome  = doc.getString("nome")  != null ? doc.getString("nome")  : "";
        String email = doc.getString("email") != null ? doc.getString("email") : "";
        String perfil= doc.getString("perfil")!= null ? doc.getString("perfil"): "usuario";

        // Monta o dialog com campos editáveis
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_usuario, null);

        TextInputEditText editNome   = view.findViewById(R.id.editNomeDialog);
        TextInputEditText editEmail  = view.findViewById(R.id.editEmailDialog);
        TextView          txtPerfil  = view.findViewById(R.id.txtPerfilAtual);
        TextView          btnUsuario = view.findViewById(R.id.btnDialogUsuario);
        TextView          btnAdmin   = view.findViewById(R.id.btnDialogAdmin);

        editNome.setText(nome);
        editEmail.setText(email);
        txtPerfil.setText("Perfil atual: " + perfil.toUpperCase());

        // Controle de seleção de perfil dentro do dialog
        final String[] perfilSelecionado = {perfil};
        atualizarBotoesPerfil(btnUsuario, btnAdmin, perfilSelecionado[0]);
        btnUsuario.setOnClickListener(v -> {
            perfilSelecionado[0] = "usuario";
            atualizarBotoesPerfil(btnUsuario, btnAdmin, "usuario");
        });
        btnAdmin.setOnClickListener(v -> {
            perfilSelecionado[0] = "admin";
            atualizarBotoesPerfil(btnUsuario, btnAdmin, "admin");
        });

        new AlertDialog.Builder(this)
                .setTitle("Editar usuário")
                .setView(view)
                .setPositiveButton("Salvar", (d, w) -> {
                    String novoNome  = editNome.getText()  != null ? editNome.getText().toString().trim()  : "";
                    String novoEmail = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    salvarEdicao(uid, novoNome, novoEmail, perfilSelecionado[0]);
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Excluir", (d, w) -> confirmarExclusao(uid, nome))
                .show();
    }

    private void atualizarBotoesPerfil(TextView btnUsuario, TextView btnAdmin, String selecionado) {
        if ("usuario".equals(selecionado)) {
            btnUsuario.setBackgroundColor(0xFF2E7D32);
            btnUsuario.setTextColor(0xFFFFFFFF);
            btnAdmin.setBackgroundColor(0xFFE8F5E9);
            btnAdmin.setTextColor(0xFF2E7D32);
        } else {
            btnAdmin.setBackgroundColor(0xFF2E7D32);
            btnAdmin.setTextColor(0xFFFFFFFF);
            btnUsuario.setBackgroundColor(0xFFE8F5E9);
            btnUsuario.setTextColor(0xFF2E7D32);
        }
    }

    // ── Salvar edição ─────────────────────────────────────────────────────────

    private void salvarEdicao(String uid, String nome, String email, String perfil) {
        setCarregando(true);

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome",   nome);
        dados.put("email",  email);
        dados.put("perfil", perfil);

        db.collection("usuarios").document(uid)
                .update(dados)
                .addOnSuccessListener(v -> {
                    setCarregando(false);
                    mostrarSucesso("Usuário atualizado!");
                    carregarUsuarios(); // recarrega a lista
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    mostrarErro("Erro ao salvar: " + e.getMessage());
                });
    }

    // ── Excluir usuário ───────────────────────────────────────────────────────

    private void confirmarExclusao(String uid, String nome) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir usuário")
                .setMessage("Deseja excluir o usuário \"" + nome + "\"?\n\nEsta ação remove os dados do Firestore. A conta do Firebase Auth deve ser removida manualmente pelo console.")
                .setPositiveButton("Excluir", (d, w) -> excluirUsuario(uid))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirUsuario(String uid) {
        setCarregando(true);
        db.collection("usuarios").document(uid)
                .delete()
                .addOnSuccessListener(v -> {
                    setCarregando(false);
                    mostrarSucesso("Usuário removido!");
                    carregarUsuarios();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    mostrarErro("Erro ao excluir: " + e.getMessage());
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
    }

    private void mostrarSucesso(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("✅ Sucesso").setMessage(msg)
                .setPositiveButton("OK", null).show();
    }

    private void mostrarErro(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Erro").setMessage(msg)
                .setPositiveButton("OK", null).show();
    }

    // ── Adapter interno ───────────────────────────────────────────────────────

    interface OnEditarClick { void onClick(DocumentSnapshot doc); }

    static class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.VH> {

        private final List<DocumentSnapshot> listaOriginal;
        private final List<DocumentSnapshot> listaFiltrada;
        private final OnEditarClick          listener;

        UsuarioAdapter(List<DocumentSnapshot> lista, OnEditarClick listener) {
            this.listaOriginal = new ArrayList<>(lista);
            this.listaFiltrada = new ArrayList<>(lista);
            this.listener      = listener;
        }

        void filtrar(String query) {
            listaFiltrada.clear();
            if (query == null || query.trim().isEmpty()) {
                listaFiltrada.addAll(listaOriginal);
            } else {
                String q = query.toLowerCase().trim();
                for (DocumentSnapshot doc : listaOriginal) {
                    String nome  = doc.getString("nome")  != null ? doc.getString("nome").toLowerCase()  : "";
                    String email = doc.getString("email") != null ? doc.getString("email").toLowerCase() : "";
                    if (nome.contains(q) || email.contains(q)) listaFiltrada.add(doc);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_usuario, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = listaFiltrada.get(position);

            String nome   = doc.getString("nome")   != null ? doc.getString("nome")   : "(sem nome)";
            String email  = doc.getString("email")  != null ? doc.getString("email")  : "";
            String perfil = doc.getString("perfil") != null ? doc.getString("perfil") : "usuario";

            // Inicial do nome no avatar
            holder.txtAvatar.setText(nome.isEmpty() ? "?" :
                    String.valueOf(nome.charAt(0)).toUpperCase());

            holder.txtNome.setText(nome);
            holder.txtEmail.setText(email);
            holder.txtBadge.setText(perfil.toUpperCase());

            // Cor do badge: admin = verde escuro, usuario = verde claro
            holder.txtBadge.setBackgroundColor(
                    "admin".equals(perfil) ? 0xFF1B5E20 : 0xFF66BB6A);

            holder.btnEditar.setOnClickListener(v -> listener.onClick(doc));
        }

        @Override public int getItemCount() { return listaFiltrada.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  txtAvatar, txtNome, txtEmail, txtBadge;
            ImageView btnEditar;
            VH(View v) {
                super(v);
                txtAvatar = v.findViewById(R.id.txtAvatar);
                txtNome   = v.findViewById(R.id.txtNomeUsuario);
                txtEmail  = v.findViewById(R.id.txtEmailUsuario);
                txtBadge  = v.findViewById(R.id.txtBadgePerfil);
                btnEditar = v.findViewById(R.id.btnEditar);
            }
        }
    }
}