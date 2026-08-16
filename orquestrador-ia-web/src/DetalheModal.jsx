export default function DetalheModal({ noticia, aoFechar, aoAprovar, aprovando }) {
  if (!noticia) return null;

  return (
    <div className="modal-fundo" onClick={aoFechar}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-fechar" onClick={aoFechar}>×</button>
        <h2>{noticia.titulo}</h2>
        <p className="modal-meta">
          {noticia.fonte} · estado atual: <strong>{noticia.estado}</strong>
        </p>
        <a href={noticia.link} target="_blank" rel="noreferrer">Ver notícia original</a>

        <Secao titulo="Resumo original" texto={noticia.resumoOriginal} html />
        <Secao titulo="Texto redigido" texto={noticia.textoRedigido} />
        <Secao titulo="Texto revisado" texto={noticia.textoRevisado} />
        <Secao titulo="Texto final (pronto para o LinkedIn)" texto={noticia.textoFinal} />

        {noticia.estado === "PRONTA_PARA_PUBLICAR" && (
          <button className="botao-aprovar" onClick={() => aoAprovar(noticia.id)} disabled={aprovando}>
            {aprovando ? "Aprovando..." : "Aprovar e marcar como publicada"}
          </button>
        )}
        {noticia.estado === "PUBLICADA" && (
          <p className="modal-publicada">✅ Já publicada</p>
        )}
      </div>
    </div>
  );
}

function Secao({ titulo, texto, html }) {
  if (!texto) return null;
  return (
    <div className="modal-secao">
      <h3>{titulo}</h3>
      {html ? (
        <p dangerouslySetInnerHTML={{ __html: texto }} />
      ) : (
        <p className="modal-texto">{texto}</p>
      )}
    </div>
  );
}
