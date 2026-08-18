import { useState } from "react";
import { ehImagemRaster, listaDeIlustracoes, prepararSvgParaExibicao } from "./svgUtils";

const LIMITE_TEXTO_VISIVEL = 220;

function TextoComHashtags({ texto }) {
  const partes = texto.split(/(#[\p{L}0-9_]+)/gu);
  return (
    <>
      {partes.map((parte, i) =>
        parte.startsWith("#") ? (
          <span key={i} className="li-hashtag">{parte}</span>
        ) : (
          <span key={i}>{parte}</span>
        )
      )}
    </>
  );
}

function IconeAcao({ path, rotulo }) {
  return (
    <button className="li-acao" type="button">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d={path} />
      </svg>
      <span>{rotulo}</span>
    </button>
  );
}

export default function LinkedInPreview({ noticia }) {
  const [expandido, setExpandido] = useState(false);
  const [indice, setIndice] = useState(0);
  const texto = noticia.textoFinal || noticia.textoIlustrado;
  const ilustracoes = listaDeIlustracoes(noticia.svgIlustracao);
  if (!texto && ilustracoes.length === 0) return null;

  const precisaTruncar = texto && texto.length > LIMITE_TEXTO_VISIVEL;
  const textoExibido = precisaTruncar && !expandido
    ? texto.slice(0, LIMITE_TEXTO_VISIVEL).trimEnd()
    : texto;
  const indiceSeguro = Math.min(indice, ilustracoes.length - 1);

  return (
    <div className="li-card">
      <div className="li-cabecalho">
        <div className="li-avatar">Você</div>
        <div className="li-identidade">
          <strong>Sua conta</strong>
          <span className="li-subtitulo">Publicando via Orquestrador de IAs</span>
          <span className="li-meta">agora · 🌐</span>
        </div>
        <button className="li-menu" type="button">•••</button>
      </div>

      {texto && (
        <p className="li-texto">
          <TextoComHashtags texto={textoExibido} />
          {precisaTruncar && !expandido && (
            <>
              {"… "}
              <button className="li-ver-mais" type="button" onClick={() => setExpandido(true)}>
                ver mais
              </button>
            </>
          )}
        </p>
      )}

      {ilustracoes.length > 0 && (
        <div className="li-galeria">
          {ehImagemRaster(ilustracoes[indiceSeguro]) ? (
            <img
              className="li-imagem li-imagem-raster"
              src={ilustracoes[indiceSeguro]}
              alt="Ilustração gerada por IA"
            />
          ) : (
            <div
              className="li-imagem"
              dangerouslySetInnerHTML={{ __html: prepararSvgParaExibicao(ilustracoes[indiceSeguro]) }}
            />
          )}
          {ilustracoes.length > 1 && (
            <>
              <button
                className="li-galeria-seta li-galeria-anterior"
                type="button"
                onClick={() => setIndice((i) => (i - 1 + ilustracoes.length) % ilustracoes.length)}
              >
                ‹
              </button>
              <button
                className="li-galeria-seta li-galeria-proxima"
                type="button"
                onClick={() => setIndice((i) => (i + 1) % ilustracoes.length)}
              >
                ›
              </button>
              <div className="li-galeria-pontos">
                {ilustracoes.map((_, i) => (
                  <button
                    key={i}
                    className={`li-galeria-ponto${i === indiceSeguro ? " ativo" : ""}`}
                    type="button"
                    onClick={() => setIndice(i)}
                  />
                ))}
              </div>
            </>
          )}
        </div>
      )}

      <div className="li-reacoes">
        <span className="li-reacoes-icones">👍❤️👏</span>
        <span>Você e outras pessoas</span>
      </div>

      <div className="li-acoes">
        <IconeAcao
          rotulo="Gostei"
          path="M7 22h-3a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2h3m4 -8l4 -3a2 2 0 0 1 3 2v6h4a2 2 0 0 1 2 2l-3 9a3 3 0 0 1 -3 2h-7a3 3 0 0 1 -3 -3v-9a3 3 0 0 1 1 -2z"
        />
        <IconeAcao rotulo="Comentar" path="M21 11.5a8.38 8.38 0 0 1 -.9 3.8 8.5 8.5 0 0 1 -7.6 4.7 8.38 8.38 0 0 1 -3.8 -.9L3 21l1.9 -5.7a8.38 8.38 0 0 1 -.9 -3.8 8.5 8.5 0 0 1 4.7 -7.6 8.38 8.38 0 0 1 3.8 -.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
        <IconeAcao rotulo="Compartilhar" path="M17 1l4 4-4 4M3 11V9a4 4 0 0 1 4-4h14M7 23l-4-4 4-4M21 13v2a4 4 0 0 1-4 4H3" />
        <IconeAcao rotulo="Enviar" path="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7z" />
      </div>
    </div>
  );
}
