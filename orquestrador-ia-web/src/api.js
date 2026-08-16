const BASE_URL = "http://localhost:8080";

async function requisitar(caminho, opcoes) {
  const resposta = await fetch(`${BASE_URL}${caminho}`, opcoes);
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

export function marcarPublicada(id) {
  return requisitar(`/noticias/${id}/marcar-publicada`, { method: "POST" });
}

export function rodarEtapa(etapa) {
  return requisitar(`/pipeline/${etapa}`, { method: "POST" });
}
