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

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

        drawerLayout    = findViewById(R.id.drawerLayout);
        ImageView ivMenu    = findViewById(R.id.ivMenu);
        ImageView ivUsuario = findViewById(R.id.ivUsuario);
        TextView  txtBoasVindas = findViewById(R.id.txtBoasVindas);
        CardView  cardConteudos = findViewById(R.id.cardConteudos);

        // ── Carrega dados do usuário ──────────────────────────────────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String perfil = doc.exists() && doc.getString("perfil") != null
                                ? doc.getString("perfil") : "usuario";

                        // Boas-vindas com primeiro nome
                        String nome = doc.getString("nome");
                        if (nome != null && !nome.isEmpty()) {
                            txtBoasVindas.setText("Bem-vindo, " + nome.split(" ")[0] + "!");
                        } else {
                            txtBoasVindas.setText("Bem-vindo, Beneficiado!");
                        }

                        // Exibe o ☰ somente para admins
                        isAdmin = doc.exists() && "admin".equals(doc.getString("perfil"));
                        ivMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    });
        }

        // ── ☰ Abre o painel lateral admin ─────────────────────────────────────
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

        // ── 👤 PopupMenu do usuário (direita) ─────────────────────────────────
        ivUsuario.setOnClickListener(this::exibirMenuUsuario);

        configurarNavegacao();
    }

    private void configurarNavegacao() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // VERIFICAÇÃO PARA ESCONDER O PERFIL QUANDO FOR BENEFICIADO
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.isAnonymous()) {
            bottomNavigation.getMenu().findItem(R.id.navigation_perfil).setVisible(false);
        }

        bottomNavigation.setSelectedItemId(R.id.navigation_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                return true;
            } else if (id == R.id.navigation_conteudos) {
                startActivity(new Intent(this, ConteudosActivity.class));
                return true;
            } else if (id == R.id.navigation_perfil) {
                startActivity(new Intent(this, PerfilUsuarioActivity.class));
                return true;
            }
            return false;
        });
    }

    // ── Menu usuário ──────────────────────────────────────────────────────────

    private void exibirMenuUsuario(View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_usuario, popup.getMenu());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.isAnonymous()) {
            popup.getMenu().findItem(R.id.action_perfil).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_perfil) {
                startActivity(new Intent(this, PerfilUsuarioActivity.class));
                return true;
            }
            if (id == R.id.action_sair) {
                confirmarSaida();
                return true;
            }
            return false;
        });

        popup.show();
    }

    // ── Sair ──────────────────────────────────────────────────────────────────

    private void confirmarSaida() {
        new AlertDialog.Builder(this)
                .setTitle("Sair")
                .setMessage("Deseja realmente sair do aplicativo?")
                .setPositiveButton("Sair", (dialog, which) -> fazerLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void fazerLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            confirmarSaida();
        }
    }
}
