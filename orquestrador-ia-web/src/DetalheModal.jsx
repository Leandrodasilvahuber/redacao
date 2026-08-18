import LinkedInPreview from "./LinkedInPreview";

const ESTADOS_COM_PREVIEW = ["ILUSTRADA", "PRONTA_PARA_PUBLICAR", "PUBLICADA"];

function removerImagens(html) {
  if (!html) return "";
  return html
    .replace(/<img[^>]*>/gi, "")
    .replace(/<picture[\s\S]*?<\/picture>/gi, "")
    .replace(/<iframe[\s\S]*?<\/iframe>/gi, "");
}

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

        {ESTADOS_COM_PREVIEW.includes(noticia.estado) && (
          <div className="modal-secao">
            <h3>Simulação do post no LinkedIn</h3>
            <LinkedInPreview noticia={noticia} />
          </div>
        )}

        {noticia.resumoOriginal && (
          <div className="modal-secao">
            <h3>Resumo original</h3>
            <p
              className="modal-texto modal-texto-resumido"
              dangerouslySetInnerHTML={{ __html: removerImagens(noticia.resumoOriginal) }}
            />
          </div>
        )}

        {noticia.estado === "PRONTA_PARA_PUBLICAR" && (
          <button className="botao-aprovar" onClick={() => aoAprovar(noticia.id)} disabled={aprovando}>
            {aprovando ? "Aprovando..." : "Aprovar e marcar como publicada"}
          </button>
        )}
        {noticia.estado === "PUBLICADA" && (
          <div className="modal-secao modal-status-publicacao">
            <p className={noticia.linkedinErro ? "modal-status-erro" : "modal-status-ok"}>
              {noticia.linkedinErro
                ? `❌ LinkedIn: ${noticia.linkedinErro}`
                : "✅ LinkedIn: publicado"}
            </p>
            <p className={noticia.blogErro ? "modal-status-erro" : "modal-status-ok"}>
              {noticia.blogErro ? `❌ Blog: ${noticia.blogErro}` : "✅ Blog: publicado"}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
