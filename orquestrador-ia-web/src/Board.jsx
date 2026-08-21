import Coluna from "./Coluna";
import { COLUNAS } from "./estados";

export default function Board({ noticias, onAbrir, onMoverNoticia, onExcluir, onExcluirTodos, processando }) {
  return (
    <div className="board">
      {COLUNAS.map(({ estado, titulo, cor }) => (
        <Coluna
          key={estado}
          estado={estado}
          titulo={titulo}
          cor={cor}
          noticias={noticias.filter((n) => n.estado === estado)}
          onAbrir={onAbrir}
          onMoverNoticia={onMoverNoticia}
          onExcluir={onExcluir}
          onExcluirTodos={estado === "PUBLICADA" ? null : onExcluirTodos}
          processando={processando}
        />
      ))}
    </div>
  );
}
