package br.feevale.agentemirim;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CompletarPerfilActivity extends AppCompatActivity {

    private TextInputLayout   layoutNome, layoutTelefone, layoutCpf, layoutDataNascimento;
    private TextInputEditText editNome, editTelefone, editCpf, editDataNascimento;
    private MaterialButton    btnConfirmar;
    private ProgressBar       progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completar_perfil);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bindViews();
        configurarMascaras();
        configurarDatePicker();

        btnConfirmar.setOnClickListener(v -> confirmar());

        // Bloqueia voltar — preenchimento obrigatório no primeiro acesso
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // bloqueado
            }
        });
    }

    private void bindViews() {
        layoutNome           = findViewById(R.id.layoutNome);
        layoutTelefone       = findViewById(R.id.layoutTelefone);
        layoutCpf            = findViewById(R.id.layoutCpf);
        layoutDataNascimento = findViewById(R.id.layoutDataNascimento);
        editNome             = findViewById(R.id.editNome);
        editTelefone         = findViewById(R.id.editTelefone);
        editCpf              = findViewById(R.id.editCpf);
        editDataNascimento   = findViewById(R.id.editDataNascimento);
        btnConfirmar         = findViewById(R.id.btnConfirmar);
        progressBar          = findViewById(R.id.progressBar);
    }

    private void confirmar() {
        layoutNome.setError(null);
        layoutTelefone.setError(null);
        layoutCpf.setError(null);
        layoutDataNascimento.setError(null);

        String nome     = editNome.getText()           != null ? editNome.getText().toString().trim()           : "";
        String telefone = editTelefone.getText()       != null ? editTelefone.getText().toString().trim()       : "";
        String cpf      = editCpf.getText()            != null ? editCpf.getText().toString().trim()            : "";
        String dataNasc = editDataNascimento.getText() != null ? editDataNascimento.getText().toString().trim() : "";

        // Validações
        if (TextUtils.isEmpty(nome)) {
            layoutNome.setError("Informe seu nome completo"); return;
        }
        if (nome.split("\\s+").length < 2) {
            layoutNome.setError("Informe nome e sobrenome"); return;
        }
        if (TextUtils.isEmpty(telefone) || telefone.length() < 14) {
            layoutTelefone.setError("Informe um telefone válido"); return;
        }
        if (TextUtils.isEmpty(cpf) || !validarCpfFormato(cpf)) {
            layoutCpf.setError("Informe um CPF válido"); return;
        }
        if (TextUtils.isEmpty(dataNasc)) {
            layoutDataNascimento.setError("Informe a data de nascimento"); return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        setCarregando(true);

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome",           nome);
        dados.put("telefone",       telefone);
        dados.put("cpf",            cpf);
        dados.put("dataNascimento", dataNasc);
        dados.put("email",          user.getEmail());
        dados.put("perfilCompleto", true);
        // SetOptions.merge() preserva o campo "perfil" (usuario/admin)
        // que o administrador já definiu ao criar a conta

        db.collection("usuarios")
                .document(user.getUid())
                .set(dados, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    setCarregando(false);
                    Intent intent = new Intent(this, ConteudosActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    layoutNome.setError("Erro ao salvar: " + e.getMessage());
                });
    }

    // ── Máscaras ──────────────────────────────────────────────────────────────

    private void configurarMascaras() {
        // Telefone: (00) 00000-0000
        editTelefone.addTextChangedListener(new TextWatcher() {
            boolean editando = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (editando) return;
                editando = true;
                String raw = s.toString().replaceAll("[^\\d]", "");
                if (raw.length() > 11) raw = raw.substring(0, 11);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i == 0) sb.append('(');
                    if (i == 2) sb.append(") ");
                    if (i == 7) sb.append('-');
                    sb.append(raw.charAt(i));
                }
                s.replace(0, s.length(), sb.toString());
                editando = false;
            }
        });

        // CPF: 000.000.000-00
        editCpf.addTextChangedListener(new TextWatcher() {
            boolean editando = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (editando) return;
                editando = true;
                String raw = s.toString().replaceAll("[^\\d]", "");
                if (raw.length() > 11) raw = raw.substring(0, 11);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i == 3 || i == 6) sb.append('.');
                    if (i == 9) sb.append('-');
                    sb.append(raw.charAt(i));
                }
                s.replace(0, s.length(), sb.toString());
                editando = false;
            }
        });
    }

    // ── DatePicker ────────────────────────────────────────────────────────────

    private void configurarDatePicker() {
        View.OnClickListener abrir = v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, ano, mes, dia) ->
                            editDataNascimento.setText(
                                    String.format("%02d/%02d/%04d", dia, mes + 1, ano)),
                    cal.get(Calendar.YEAR) - 18,
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                    .show();
        };
        editDataNascimento.setOnClickListener(abrir);
        layoutDataNascimento.setEndIconOnClickListener(abrir);
    }

    private boolean validarCpfFormato(String cpf) {
        return Pattern.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", cpf);
    }

    private void setCarregando(boolean c) {
        progressBar.setVisibility(c ? View.VISIBLE : View.GONE);
        btnConfirmar.setEnabled(!c);
        btnConfirmar.setText(c ? "Salvando..." : "CONFIRMAR E CONTINUAR");
    }
}