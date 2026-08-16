import { useCallback, useEffect, useState } from "react";
import Board from "./Board";
import DetalheModal from "./DetalheModal";
import { listarNoticias, marcarPublicada, rodarEtapa } from "./api";
import "./App.css";

const ETAPAS = [
  { chave: "buscar", rotulo: "Buscar" },
  { chave: "selecionar", rotulo: "Selecionar" },
  { chave: "redigir", rotulo: "Redigir" },
  { chave: "revisar", rotulo: "Revisar" },
  { chave: "publicar", rotulo: "Publicar" },
];

export default function App() {
  const [noticias, setNoticias] = useState([]);
  const [selecionada, setSelecionada] = useState(null);
  const [etapaRodando, setEtapaRodando] = useState(null);
  const [aprovando, setAprovando] = useState(false);
  const [erro, setErro] = useState(null);

  const carregar = useCallback(async () => {
    try {
      const dados = await listarNoticias();
      setNoticias(dados);
      setErro(null);
    } catch (e) {
      setErro(e.message);
    }
  }, []);

  useEffect(() => {
    carregar();
    const intervalo = setInterval(carregar, 5000);
    return () => clearInterval(intervalo);
  }, [carregar]);

  async function executarEtapa(chave) {
    setEtapaRodando(chave);
    setErro(null);
    try {
      await rodarEtapa(chave);
      await carregar();
    } catch (e) {
      setErro(e.message);
    } finally {
      setEtapaRodando(null);
    }
  }

  async function executarTudo() {
    setEtapaRodando("executar-tudo");
    setErro(null);
    try {
      await rodarEtapa("executar-tudo");
      await carregar();
    } catch (e) {
      setErro(e.message);
    } finally {
      setEtapaRodando(null);
    }
  }

  async function aprovar(id) {
    setAprovando(true);
    try {
      await marcarPublicada(id);
      await carregar();
      setSelecionada(null);
    } catch (e) {
      setErro(e.message);
    } finally {
      setAprovando(false);
    }
  }

  return (
    <div className="app">
      <header className="topo">
        <h1>Orquestrador de IAs — Notícias para LinkedIn</h1>
        <div className="botoes-etapas">
          {ETAPAS.map(({ chave, rotulo }) => (
            <button
              key={chave}
              onClick={() => executarEtapa(chave)}
              disabled={etapaRodando !== null}
            >
              {etapaRodando === chave ? "Rodando..." : rotulo}
            </button>
          ))}
          <button
            className="botao-tudo"
            onClick={executarTudo}
            disabled={etapaRodando !== null}
          >
            {etapaRodando === "executar-tudo" ? "Rodando tudo..." : "Rodar tudo"}
          </button>
        </div>
      </header>

      {erro && <div className="erro">{erro}</div>}

      <Board noticias={noticias} onAbrir={setSelecionada} />

      <DetalheModal
        noticia={selecionada}
        aoFechar={() => setSelecionada(null)}
        aoAprovar={aprovar}
        aprovando={aprovando}
      />
    </div>
  );
}
