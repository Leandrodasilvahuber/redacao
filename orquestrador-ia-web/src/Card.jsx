import { FERRAMENTA_POR_ETAPA, TEXTO_POR_ESTADO } from "./estados";

function tirarHtml(texto) {
  if (!texto) return "";
  const elemento = document.createElement("div");
  elemento.innerHTML = texto;
  return elemento.textContent || "";
}

function formatarData(data) {
  if (!data) return "";
  return new Date(data).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function Card({ noticia, onAbrir, onExcluir, processando }) {
  const resumo = tirarHtml(TEXTO_POR_ESTADO(noticia)) || "";
  return (
    <div
      className="card"
      draggable={!processando}
      onDragStart={(e) => {
        e.dataTransfer.setData("text/plain", String(noticia.id));
        e.dataTransfer.effectAllowed = "move";
      }}
    >
      {processando && (
        <div className="card-processando">
          <div className="card-processando-spinner" />
          <span>{FERRAMENTA_POR_ETAPA[processando.etapa] || "Processando..."}</span>
        </div>
      )}
      <button
        className="card-excluir"
        title="Excluir notícia"
        onClick={(e) => {
          e.stopPropagation();
          if (window.confirm(`Excluir "${noticia.titulo}"?`)) {
            onExcluir(noticia.id);
          }
        }}
      >
        ×
      </button>
      <strong className="card-titulo">{noticia.titulo}</strong>
      <div className="card-meta">
        <span className="card-fonte">{noticia.fonte}</span>
        <span className="card-data">{formatarData(noticia.atualizadoEm)}</span>
      </div>
      <p className="card-resumo">{resumo.slice(0, 400)}{resumo.length > 400 ? "…" : ""}</p>
      <button
        className="card-detalhes"
        onClick={(e) => {
          e.stopPropagation();
          onAbrir(noticia);
        }}
      >
        Ver detalhes
      </button>
    </div>
  );
}
