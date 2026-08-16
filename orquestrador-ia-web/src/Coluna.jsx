import Card from "./Card";

export default function Coluna({ titulo, cor, noticias, onAbrir }) {
  return (
    <div className="coluna">
      <div className="coluna-cabecalho" style={{ borderColor: cor }}>
        <span>{titulo}</span>
        <span className="coluna-contagem">{noticias.length}</span>
      </div>
      <div className="coluna-cards">
        {noticias.map((noticia) => (
          <Card key={noticia.id} noticia={noticia} onAbrir={onAbrir} />
        ))}
        {noticias.length === 0 && <p className="coluna-vazia">Nada aqui</p>}
      </div>
    </div>
  );
}
