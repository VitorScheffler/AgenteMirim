package br.feevale.agentemirim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText editUsuario = findViewById(R.id.editUsuario);
        EditText editSenha   = findViewById(R.id.editSenha);
        Button   btnLogin    = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String usuario = editUsuario.getText().toString();
            String senha   = editSenha.getText().toString();

            if (usuario.equals("admin") && senha.equals("admin")) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Usuário ou senha incorretos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}