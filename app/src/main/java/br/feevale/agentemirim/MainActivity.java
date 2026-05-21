package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private DrawerLayout      drawerLayout;
    private boolean           isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout        = findViewById(R.id.drawerLayout);
        ImageView ivMenu    = findViewById(R.id.ivMenu);
        ImageView ivUsuario = findViewById(R.id.ivUsuario);
        TextView  txtBoasVindas = findViewById(R.id.txtBoasVindas);
        CardView  cardConteudos = findViewById(R.id.cardConteudos);

        // ── Carrega dados do usuário (somente se estiver logado) ──────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String nome = doc.getString("nome");
                        if (nome != null && !nome.isEmpty()) {
                            txtBoasVindas.setText("Bem-vindo, " + nome.split(" ")[0] + "!");
                        }

                        isAdmin = doc.exists() && "admin".equals(doc.getString("perfil"));
                        ivMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    });
        } else {
            // Sem login: esconde o menu admin e mantém saudação padrão
            ivMenu.setVisibility(View.GONE);
        }

        // ── ☰ Painel lateral admin ────────────────────────────────────────────
        ivMenu.setOnClickListener(v -> {
            if (isAdmin) drawerLayout.openDrawer(GravityCompat.START);
        });

        // ── Itens do painel lateral ───────────────────────────────────────────
        NavigationView navAdmin = findViewById(R.id.navAdmin);
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

        // ── Card Conteúdos ────────────────────────────────────────────────────
        cardConteudos.setOnClickListener(v ->
                startActivity(new Intent(this, ConteudosActivity.class)));

        // ── 👤 Ícone de usuário ───────────────────────────────────────────────
        ivUsuario.setOnClickListener(this::exibirMenuUsuario);
    }

    // ── onResume: atualiza ícone/saudação se o usuário logou e voltou ─────────
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivMenu       = findViewById(R.id.ivMenu);
        TextView  txtBoasVindas = findViewById(R.id.txtBoasVindas);

        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String nome = doc.getString("nome");
                        if (nome != null && !nome.isEmpty()) {
                            txtBoasVindas.setText("Bem-vindo, " + nome.split(" ")[0] + "!");
                        }
                        isAdmin = doc.exists() && "admin".equals(doc.getString("perfil"));
                        ivMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    });
        } else {
            // Usuário fez logout ou nunca logou — reseta estado
            isAdmin = false;
            ivMenu.setVisibility(View.GONE);
            txtBoasVindas.setText("Bem-vindo!");
        }
    }

    // ── Menu do ícone de usuário ──────────────────────────────────────────────
    private void exibirMenuUsuario(View anchorView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        PopupMenu popup = new PopupMenu(this, anchorView);

        if (user == null) {
            // Não logado → opção simples de login
            popup.getMenu().add(0, 1001, 0, "Entrar");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1001) {
                    startActivity(new Intent(this, LoginActivity.class));
                    return true;
                }
                return false;
            });

        } else {
            // Logado → menu normal
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

    // ── Confirmar logout ──────────────────────────────────────────────────────
    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta")
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton("Sair", (dialog, which) -> {

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void fazerLogout() {
        FirebaseAuth.getInstance().signOut();
        // Não redireciona para login — apenas desloga e atualiza a tela
        isAdmin = false;
        findViewById(R.id.ivMenu).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.txtBoasVindas)).setText("Bem-vindo!");
    }

    // ── Voltar: fecha drawer ou minimiza o app (sem logout) ───────────────────
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Minimiza o app sem matar a Activity
            moveTaskToBack(true);
        }
    }
}