import { useState } from "react";
import { criarNoticiaManual, formatarNoticiaManual } from "./api";

const TIPOS = [
  { valor: "NOTICIA", rotulo: "Notícia" },
  { valor: "TUTORIAL", rotulo: "Breve tutorial" },
];

function estadoInicial() {
  return { texto: "", tipo: "NOTICIA" };
}

export default function CriarNoticia({ aoSalvar }) {
  const [rascunho, setRascunho] = useState(estadoInicial());
  const [formatando, setFormatando] = useState(false);
  const [gerado, setGerado] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  function cancelar() {
    setGerado(null);
    setRascunho(estadoInicial());
    setErro(null);
  }

  async function formatarComIa() {
    if (!rascunho.texto.trim()) {
      setErro("Cole um texto antes de formatar.");
      return;
    }
    setFormatando(true);
    setErro(null);
    try {
      const resultado = await formatarNoticiaManual(rascunho.texto, rascunho.tipo);
      setGerado(resultado);
    } catch (e) {
      setErro(e.message);
    } finally {
      setFormatando(false);
    }
  }

  async function salvarNoticia() {
    if (!gerado.titulo.trim() || !gerado.texto.trim()) {
      setErro("Título e texto são obrigatórios.");
      return;
    }
    setSalvando(true);
    setErro(null);
    try {
      await criarNoticiaManual(gerado.titulo, gerado.texto);
      cancelar();
      aoSalvar?.();
    } catch (e) {
      setErro(e.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="criar-noticia">
      <div className="config-secao">
        <h2>Criar notícia</h2>
        <p className="config-descricao">
          Cole um texto e a IA (Mistral) formata como notícia curta ou breve tutorial. Depois é só
          revisar e salvar — a notícia entra no pipeline já revisada.
        </p>

        {erro && <div className="erro">{erro}</div>}

        {!gerado ? (
          <>
            <div className="config-campo">
              <label htmlFor="criar-noticia-texto">Texto colado</label>
              <textarea
                id="criar-noticia-texto"
                className="criar-noticia-textarea"
                rows={10}
                placeholder="Cole aqui o texto bruto que a IA vai formatar..."
                value={rascunho.texto}
                onChange={(e) => setRascunho((atual) => ({ ...atual, texto: e.target.value }))}
                disabled={formatando}
              />
            </div>

            <div className="config-campo">
              <label>Formato</label>
              <div className="config-checkboxes">
                {TIPOS.map((t) => (
                  <label key={t.valor} className="config-checkbox">
                    <input
                      type="radio"
                      name="criar-noticia-tipo"
                      value={t.valor}
                      checked={rascunho.tipo === t.valor}
                      onChange={() => setRascunho((atual) => ({ ...atual, tipo: t.valor }))}
                      disabled={formatando}
                    />
                    {t.rotulo}
                  </label>
                ))}
              </div>
            </div>

            <button className="config-salvar" onClick={formatarComIa} disabled={formatando}>
              {formatando ? "Formatando com IA..." : "Formatar com IA"}
            </button>
          </>
        ) : (
          <>
            <div className="config-campo">
              <label htmlFor="criar-noticia-titulo">Título</label>
              <input
                id="criar-noticia-titulo"
                type="text"
                value={gerado.titulo}
                onChange={(e) => setGerado((atual) => ({ ...atual, titulo: e.target.value }))}
                disabled={salvando}
              />
            </div>

            <div className="config-campo">
              <label htmlFor="criar-noticia-corpo">Texto formatado</label>
              <textarea
                id="criar-noticia-corpo"
                className="criar-noticia-textarea"
                rows={10}
                value={gerado.texto}
                onChange={(e) => setGerado((atual) => ({ ...atual, texto: e.target.value }))}
                disabled={salvando}
              />
            </div>

            <div className="criar-noticia-acoes">
              <button className="config-salvar" onClick={salvarNoticia} disabled={salvando}>
                {salvando ? "Salvando..." : "Salvar notícia"}
              </button>
              <button className="botao-cancelar-icone" onClick={cancelar} disabled={salvando}>
                Cancelar
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
