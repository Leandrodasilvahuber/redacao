import Coluna from "./Coluna";
import { COLUNAS } from "./estados";

export default function Board({ noticias, onAbrir }) {
  return (
    <div className="board">
      {COLUNAS.map(({ estado, titulo, cor }) => (
        <Coluna
          key={estado}
          titulo={titulo}
          cor={cor}
          noticias={noticias.filter((n) => n.estado === estado)}
          onAbrir={onAbrir}
        />
      ))}
    </div>
  );
}
