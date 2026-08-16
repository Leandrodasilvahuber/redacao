import { TEXTO_POR_ESTADO } from "./estados";

function tirarHtml(texto) {
  if (!texto) return "";
  const elemento = document.createElement("div");
  elemento.innerHTML = texto;
  return elemento.textContent || "";
}

export default function Card({ noticia, onAbrir }) {
  const resumo = tirarHtml(TEXTO_POR_ESTADO(noticia)) || "";
  return (
    <button className="card" onClick={() => onAbrir(noticia)}>
      <strong className="card-titulo">{noticia.titulo}</strong>
      <span className="card-fonte">{noticia.fonte}</span>
      <p className="card-resumo">{resumo.slice(0, 140)}{resumo.length > 140 ? "…" : ""}</p>
    </button>
  );
}
