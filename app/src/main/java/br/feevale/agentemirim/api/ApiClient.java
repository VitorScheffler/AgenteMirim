package br.feevale.agentemirim.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente HTTP para a API de arquivos do Agente Mirim.
 *
 * Coloque em: app/src/main/java/br/feevale/agentemirim/api/ApiClient.java
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CONFIGURAÇÃO OBRIGATÓRIA:
 *   1. Defina BASE_URL com o endereço da sua API (localhost, ngrok, VPS, etc.)
 *   2. Defina AUTH_TOKEN com o token definido no seu .env
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ApiClient {

    // ── ⚙️  CONFIGURAÇÃO — ALTERE AQUI ────────────────────────────────────────
    private static final String BASE_URL   = "https://glamorous-handwrite-speak.ngrok-free.dev"; // sem barra no final
    private static final String AUTH_TOKEN = "-R,V*ox+>K,0o76MH=XYNG9.sRz@xLLR";               // igual ao .env
    // ─────────────────────────────────────────────────────────────────────────

    private static final String  TAG      = "ApiClient";
    private static final int     TIMEOUT  = 30_000; // 30 segundos
    private static final String  BOUNDARY = "----AgenteMirimBoundary7MA4YWxkTrZu0gW";

    private static ApiClient instance;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static ApiClient getInstance() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    // ── Modelos internos ──────────────────────────────────────────────────────

    /** Representa um arquivo retornado pela API. */
    public static class ArquivoApi {
        public String id;
        public String filename;
        public String contentType;
        public long   sizeBytes;
        public String createdAt;

        /** URL completa para download do arquivo. */
        public String getDownloadUrl() {
            return BASE_URL + "/files/" + id;
        }
    }

    /** Resultado de uma operação assíncrona. */
    public interface Callback<T> {
        void onSucesso(T resultado);
        void onErro(String mensagem);
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Faz upload de um arquivo selecionado pelo usuário.
     *
     * @param context   Context para abrir o InputStream do Uri
     * @param uri       Uri do arquivo (retornado pelo file picker)
     * @param nomeArquivo Nome original do arquivo (ex: "imagem.jpg")
     * @param mimeType  Tipo MIME (ex: "image/jpeg")
     * @param callback  Retorna o ArquivoApi criado ou uma mensagem de erro
     */
    public void uploadArquivo(Context context, Uri uri, String nomeArquivo,
                              String mimeType, Callback<ArquivoApi> callback) {
        executor.execute(() -> {
            try {
                byte[] dados = lerBytes(context, uri);
                if (dados == null || dados.length == 0) {
                    callback.onErro("Arquivo vazio ou não encontrado");
                    return;
                }

                // Monta multipart/form-data manualmente (sem biblioteca externa)
                byte[] corpo = montarMultipart(dados, nomeArquivo, mimeType);

                HttpURLConnection conn = abrirConexao(BASE_URL + "/files/upload", "POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(corpo);
                }

                int status = conn.getResponseCode();
                if (status == 200 || status == 201) {
                    String resposta = lerResposta(conn);
                    ArquivoApi arquivo = parsearArquivo(new JSONObject(resposta));
                    callback.onSucesso(arquivo);
                } else {
                    String erro = lerErro(conn);
                    callback.onErro("Erro " + status + ": " + erro);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Erro no upload", e);
                callback.onErro("Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── Listar arquivos ───────────────────────────────────────────────────────

    /**
     * Lista todos os arquivos cadastrados na API.
     */
    public void listarArquivos(Callback<List<ArquivoApi>> callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = abrirConexao(BASE_URL + "/files/", "GET");
                int status = conn.getResponseCode();

                if (status == 200) {
                    String resposta = lerResposta(conn);
                    JSONArray array = new JSONArray(resposta);
                    List<ArquivoApi> lista = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        lista.add(parsearArquivo(array.getJSONObject(i)));
                    }
                    callback.onSucesso(lista);
                } else {
                    callback.onErro("Erro ao listar: " + status);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Erro ao listar", e);
                callback.onErro("Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── Deletar arquivo ───────────────────────────────────────────────────────

    /**
     * Remove um arquivo pelo ID.
     */
    public void deletarArquivo(String fileId, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = abrirConexao(BASE_URL + "/files/" + fileId, "DELETE");
                int status = conn.getResponseCode();

                if (status == 200) {
                    callback.onSucesso(null);
                } else {
                    callback.onErro("Erro ao deletar: " + status);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Erro ao deletar", e);
                callback.onErro("Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private HttpURLConnection abrirConexao(String url, String metodo) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(metodo);
        conn.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        return conn;
    }

    private byte[] montarMultipart(byte[] dados, String nomeArquivo, String mimeType) throws IOException {
        // Padrão RFC 2046 multipart/form-data
        String cabecalho = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + nomeArquivo + "\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n";
        String rodape = "\r\n--" + BOUNDARY + "--\r\n";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(cabecalho.getBytes("UTF-8"));
        bos.write(dados);
        bos.write(rodape.getBytes("UTF-8"));
        return bos.toByteArray();
    }

    private byte[] lerBytes(Context context, Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) return null;
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) bos.write(buffer, 0, n);
            return bos.toByteArray();
        }
    }

    private String lerResposta(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    private String lerErro(HttpURLConnection conn) {
        try (InputStream is = conn.getErrorStream()) {
            if (is == null) return "sem detalhe";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            JSONObject obj = new JSONObject(bos.toString("UTF-8"));
            return obj.optString("detail", bos.toString("UTF-8"));
        } catch (Exception e) {
            return "erro desconhecido";
        }
    }

    private ArquivoApi parsearArquivo(JSONObject obj) {
        ArquivoApi a   = new ArquivoApi();
        a.id          = obj.optString("id");
        a.filename    = obj.optString("filename");
        a.contentType = obj.optString("content_type");
        a.sizeBytes   = obj.optLong("size_bytes");
        a.createdAt   = obj.optString("created_at");
        return a;
    }
}
