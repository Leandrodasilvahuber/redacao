import { useState } from "react";
import Card from "./Card";

export default function Coluna({ estado, titulo, cor, noticias, onAbrir, onMoverNoticia, onExcluir, onExcluirTodos, processando }) {
  const [emCimaDoAlvo, setEmCimaDoAlvo] = useState(false);

  const noticiasOrdenadas = [...noticias].sort(
    (a, b) => new Date(b.atualizadoEm) - new Date(a.atualizadoEm)
  );

  return (
    <div
      className={`coluna${emCimaDoAlvo ? " coluna-alvo" : ""}`}
      style={{ borderColor: emCimaDoAlvo ? cor : undefined }}
      onDragOver={(e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
        setEmCimaDoAlvo(true);
      }}
      onDragLeave={() => setEmCimaDoAlvo(false)}
      onDrop={(e) => {
        e.preventDefault();
        setEmCimaDoAlvo(false);
        const id = Number(e.dataTransfer.getData("text/plain"));
        if (id) onMoverNoticia(id, estado);
      }}
    >
      <div className="coluna-cabecalho" style={{ borderColor: cor }}>
        <span>{titulo}</span>
        <div className="coluna-cabecalho-acoes">
          {onExcluirTodos && noticias.length > 0 && (
            <button
              type="button"
              className="coluna-excluir-todos"
              title={`Excluir todas as notícias de "${titulo}"`}
              onClick={() => onExcluirTodos(estado, titulo)}
            >
              Excluir todos
            </button>
          )}
          <span className="coluna-contagem">{noticias.length}</span>
        </div>
      </div>
      <div className="coluna-cards">
        {noticiasOrdenadas.map((noticia) => (
          <Card
            key={noticia.id}
            noticia={noticia}
            onAbrir={onAbrir}
            onExcluir={onExcluir}
            processando={processando?.id === noticia.id ? processando : null}
          />
        ))}
        {noticias.length === 0 && <p className="coluna-vazia">Nada aqui</p>}
      </div>
    </div>
  );
}
