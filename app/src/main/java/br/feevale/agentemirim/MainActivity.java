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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

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

        // Busca o nome do usuário no Firestore e exibe boas-vindas
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView txtBoasVindas = findViewById(R.id.txtBoasVindas);

        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String nome = doc.getString("nome");
                        if (nome != null && !nome.isEmpty()) {
                            String primeiroNome = nome.split(" ")[0];
                            txtBoasVindas.setText("Bem-vindo, " + primeiroNome + "!");
                        }
                    });
        }

        ImageView ivUsuario = findViewById(R.id.ivUsuario);
        ivUsuario.setOnClickListener(this::exibirMenuUsuario);
    }

    private void exibirMenuUsuario(View anchorView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = doc.exists()
                            && "admin".equals(doc.getString("perfil"));

                    PopupMenu popup = new PopupMenu(this, anchorView);
                    popup.getMenuInflater().inflate(R.menu.menu_usuario, popup.getMenu());

                    popup.getMenu()
                            .findItem(R.id.action_criar_usuario)
                            .setVisible(isAdmin);

                    popup.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_gerenciar_usuarios) {
                            startActivity(new Intent(this, GerenciarUsuariosActivity.class));
                            return true;
                        }
                        if (id == R.id.action_criar_usuario) {
                            startActivity(new Intent(this, CriarUsuarioActivity.class));
                            return true;
                        }
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
                });
    }

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
        super.onBackPressed();
        confirmarSaida();
    }
}