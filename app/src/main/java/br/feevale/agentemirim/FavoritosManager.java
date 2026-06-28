package br.feevale.agentemirim;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gerencia favoritos de cidades.
 *
 * • Usuário logado   → Firestore  (/usuarios/{uid}/favoritos/{cidadeId})
 * • Usuário anônimo  → SharedPreferences  (chave "favoritos_locais")
 *
 * Ao fazer login, chame migrarFavoritosLocaisParaFirestore() para sincronizar.
 */
public class FavoritosManager {

    private static final String PREFS_NAME  = "agente_mirim_prefs";
    private static final String PREFS_KEY   = "favoritos_locais";

    private final Context           context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;

    public interface Callback {
        void onConcluido();
    }

    public FavoritosManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db      = FirebaseFirestore.getInstance();
    }

    // =========================================================================
    // API PÚBLICA
    // =========================================================================

    /** Adiciona ou remove favorito, escolhendo o backend correto. */
    public void toggle(String cidadeId, boolean eraFavorito, Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            toggleFirestore(user.getUid(), cidadeId, eraFavorito, callback);
        } else {
            toggleLocal(cidadeId, eraFavorito);
            if (callback != null) callback.onConcluido();
        }
    }

    /** Lê os favoritos locais (SharedPreferences). */
    public Set<String> lerFavoritosLocais() {
        return new HashSet<>(prefs.getStringSet(PREFS_KEY, new HashSet<>()));
    }

    /**
     * Chamado após login bem-sucedido.
     * Copia os favoritos locais para o Firestore e limpa o cache local.
     */
    public void migrarFavoritosLocaisParaFirestore(String uid, Callback callback) {
        Set<String> locais = lerFavoritosLocais();
        if (locais.isEmpty()) {
            if (callback != null) callback.onConcluido();
            return;
        }

        // Batch write para não fazer N roundtrips
        var batch = db.batch();
        for (String cidadeId : locais) {
            var ref = db.collection("usuarios")
                    .document(uid)
                    .collection("favoritos")
                    .document(cidadeId);
            Map<String, Object> dados = new HashMap<>();
            dados.put("favoritadoEm", Timestamp.now());
            dados.put("origem", "local"); // útil para debug
            batch.set(ref, dados);
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    limparFavoritosLocais(); // migrado com sucesso → limpa local
                    if (callback != null) callback.onConcluido();
                })
                .addOnFailureListener(e -> {
                    // Falhou: mantém local para tentar de novo depois
                    if (callback != null) callback.onConcluido();
                });
    }

    /** Remove todos os favoritos locais (após logout ou migração). */
    public void limparFavoritosLocais() {
        prefs.edit().remove(PREFS_KEY).apply();
    }

    // =========================================================================
    // INTERNOS
    // =========================================================================

    private void toggleFirestore(String uid, String cidadeId,
                                 boolean eraFavorito, Callback callback) {
        var ref = db.collection("usuarios")
                .document(uid)
                .collection("favoritos")
                .document(cidadeId);

        var task = eraFavorito ? ref.delete() : ref.set(dadosFavorito());
        task.addOnCompleteListener(t -> { if (callback != null) callback.onConcluido(); });
    }

    private void toggleLocal(String cidadeId, boolean eraFavorito) {
        Set<String> atual = lerFavoritosLocais(); // cópia mutável
        if (eraFavorito) {
            atual.remove(cidadeId);
        } else {
            atual.add(cidadeId);
        }
        prefs.edit().putStringSet(PREFS_KEY, atual).apply();
    }

    private Map<String, Object> dadosFavorito() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("favoritadoEm", Timestamp.now());
        return dados;
    }
}