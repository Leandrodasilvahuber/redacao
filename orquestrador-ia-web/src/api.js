export const BASE_URL = "http://localhost:8080";
const TIMEOUT_PADRAO_MS = 30_000;

async function requisitar(caminho, opcoes = {}, timeoutMs = TIMEOUT_PADRAO_MS) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  let resposta;
  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, { ...opcoes, signal: controller.signal });
  } catch (e) {
    if (e.name === "AbortError") {
      throw new Error(
        `Tempo esgotado (${Math.round(timeoutMs / 1000)}s) aguardando resposta de ${caminho}. O servidor pode estar ocupado ou fora do ar.`
      );
    }
    throw new Error(`Não foi possível conectar em ${caminho}: ${e.message}`);
  } finally {
    clearTimeout(timer);
  }

  if (!resposta.ok) {
    const texto = await resposta.text().catch(() => "");
    throw new Error(`Falha em ${caminho}: ${resposta.status} ${texto}`);
  }
  if (resposta.status === 204) return null;
  return resposta.json();
}

export function listarNoticias() {
  return requisitar("/noticias");
}

export function marcarPublicada(id, imagemPngBase64) {
  return requisitar(`/noticias/${id}/marcar-publicada`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ imagemPngBase64: imagemPngBase64 ?? null }),
  });
}

export function regerarIcone(id) {
  return requisitar(`/noticias/${id}/regerar-icone`, { method: "POST" }, 90_000);
}

export function excluirNoticia(id) {
  return requisitar(`/noticias/${id}`, { method: "DELETE" });
}

export function rodarEtapa(etapa, id) {
  const timeoutMs = etapa === "executar-tudo" ? 180_000 : 90_000;
  const caminho = id ? `/pipeline/${etapa}?id=${id}` : `/pipeline/${etapa}`;
  return requisitar(caminho, { method: "POST" }, timeoutMs);
}

export function buscarUsoGroq() {
  return requisitar("/groq/uso");
}

export function buscarUsoGemini() {
  return requisitar("/gemini/uso");
}

export function buscarUsoMistral() {
  return requisitar("/mistral/uso");
}

export function buscarConfiguracoes() {
  return requisitar("/configuracoes");
}

export function salvarConfiguracoes(config) {
  return requisitar("/configuracoes", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(config),
  });
}
