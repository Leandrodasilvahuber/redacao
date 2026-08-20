import { useCallback, useEffect, useState } from "react";
import AreaBusca from "./AreaBusca";
import Board from "./Board";
import Configuracoes from "./Configuracoes";
import CriarNoticia from "./CriarNoticia";
import Dashboard from "./Dashboard";
import DetalheModal from "./DetalheModal";
import IndicadorIA from "./IndicadorIA";
import { buscarUsoGemini, buscarUsoGroq, buscarUsoMistral, excluirNoticia, listarNoticias, marcarPublicada, regerarIcone, rodarEtapa } from "./api";
import { CONTADOR_POR_ETAPA, PROXIMA_ETAPA } from "./estados";
import { listaDeIlustracoes, rasterizarSvgParaPng } from "./svgUtils";
import "./App.css";

async function rasterizarIlustracao(noticia) {
  try {
    const [primeiraIlustracao] = listaDeIlustracoes(noticia?.svgIlustracao);
    return await rasterizarSvgParaPng(primeiraIlustracao);
  } catch {
    return null;
  }
}

export default function App() {
  const [noticias, setNoticias] = useState([]);
  const [selecionada, setSelecionada] = useState(null);
  const [etapaRodando, setEtapaRodando] = useState(null);
  const [processando, setProcessando] = useState(null);
  const [aprovando, setAprovando] = useState(false);
  const [regerandoIcone, setRegerandoIcone] = useState(false);
  const [erro, setErro] = useState(null);
  const [aviso, setAviso] = useState(null);

  function avisarTemporario(mensagem) {
    setAviso(mensagem);
    setTimeout(() => setAviso((atual) => (atual === mensagem ? null : atual)), 4000);
  }
  const [usoGroq, setUsoGroq] = useState(null);
  const [usoGemini, setUsoGemini] = useState(null);
  const [usoMistral, setUsoMistral] = useState(null);
  const [abaAtiva, setAbaAtiva] = useState("board");

  const carregar = useCallback(async () => {
    try {
      const dados = await listarNoticias();
      setNoticias(dados);
      setErro(null);
    } catch (e) {
      setErro(e.message);
    }
  }, []);

  const carregarUsoGroq = useCallback(async () => {
    try {
      const dados = await buscarUsoGroq();
      setUsoGroq(dados);
    } catch {
      // silencioso: métricas de uso não são essenciais para o funcionamento da tela
    }
  }, []);

  const carregarUsoGemini = useCallback(async () => {
    try {
      const dados = await buscarUsoGemini();
      setUsoGemini(dados);
    } catch {
      // silencioso: métricas de uso não são essenciais para o funcionamento da tela
    }
  }, []);

  const carregarUsoMistral = useCallback(async () => {
    try {
      const dados = await buscarUsoMistral();
      setUsoMistral(dados);
    } catch {
      // silencioso: métricas de uso não são essenciais para o funcionamento da tela
    }
  }, []);

  useEffect(() => {
    carregar();
    carregarUsoGroq();
    carregarUsoGemini();
    carregarUsoMistral();
    const intervalo = setInterval(() => {
      carregar();
      carregarUsoGroq();
      carregarUsoGemini();
      carregarUsoMistral();
    }, 5000);
    return () => clearInterval(intervalo);
  }, [carregar, carregarUsoGroq, carregarUsoGemini, carregarUsoMistral]);

  async function buscarNoticias(termo) {
    setEtapaRodando("buscar");
    setErro(null);
    try {
      await rodarEtapa("buscar", null, { termo });
      await carregar();
    } catch (e) {
      setErro(e.message);
    } finally {
      setEtapaRodando(null);
    }
  }

  async function moverNoticia(id, estadoDestino) {
    const noticia = noticias.find((n) => n.id === id);
    if (!noticia) return;
    const proxima = PROXIMA_ETAPA[noticia.estado];
    if (!proxima || proxima.destino !== estadoDestino) {
      setErro(`Não é possível mover de "${noticia.estado}" direto para "${estadoDestino}".`);
      return;
    }
    setEtapaRodando(`mover-${id}`);
    setProcessando({ id, etapa: proxima.etapa });
    setErro(null);
    try {
      if (proxima.etapa === "aprovar") {
        const imagemPngBase64 = await rasterizarIlustracao(noticia);
        await marcarPublicada(id, imagemPngBase64);
      } else {
        const resultado = await rodarEtapa(proxima.etapa, id);
        const chave = CONTADOR_POR_ETAPA[proxima.etapa];
        if (chave && resultado?.[chave] === 0) {
          const atualizada = (await listarNoticias()).find((n) => n.id === id);
          if (atualizada?.estado === "DESCARTADA") {
            avisarTemporario(`Descartada: "${noticia.titulo}" não passou nos critérios da IA.`);
          } else {
            avisarTemporario(`Não foi possível mover "${noticia.titulo}" agora. Tente de novo em instantes.`);
          }
        }
      }
      await carregar();
      await carregarUsoGroq();
      await carregarUsoGemini();
      await carregarUsoMistral();
    } catch (e) {
      setErro(e.message);
    } finally {
      setEtapaRodando(null);
      setProcessando(null);
    }
  }

  async function excluir(id) {
    setErro(null);
    try {
      await excluirNoticia(id);
      setNoticias((atuais) => atuais.filter((n) => n.id !== id));
      if (selecionada?.id === id) setSelecionada(null);
    } catch (e) {
      setErro(e.message);
    }
  }

  async function aprovar(id) {
    setAprovando(true);
    try {
      const noticia = noticias.find((n) => n.id === id);
      const imagemPngBase64 = await rasterizarIlustracao(noticia);
      await marcarPublicada(id, imagemPngBase64);
      await carregar();
      setSelecionada(null);
    } catch (e) {
      setErro(e.message);
    } finally {
      setAprovando(false);
    }
  }

  async function regerarIconeDaNoticia(id, descricao) {
    setRegerandoIcone(true);
    setErro(null);
    try {
      const { svgIlustracao } = await regerarIcone(id, descricao);
      setNoticias((atuais) => atuais.map((n) => (n.id === id ? { ...n, svgIlustracao: JSON.stringify([svgIlustracao]) } : n)));
      setSelecionada((atual) => (atual?.id === id ? { ...atual, svgIlustracao: JSON.stringify([svgIlustracao]) } : atual));
    } catch (e) {
      setErro(e.message);
    } finally {
      setRegerandoIcone(false);
    }
  }

  return (
    <div className="app">
      <header className="topo">
        <div className="topo-titulo">
          <h1>Orquestrador de IAs — Notícias para LinkedIn</h1>
          <IndicadorIA usoGroq={usoGroq} usoGemini={usoGemini} usoMistral={usoMistral} />
        </div>
        <div className="botoes-abas">
          <button
            className={abaAtiva === "board" ? "aba-ativa" : ""}
            onClick={() => setAbaAtiva("board")}
          >
            Pipeline
          </button>
          <button
            className={abaAtiva === "criar-noticia" ? "aba-ativa" : ""}
            onClick={() => setAbaAtiva("criar-noticia")}
          >
            Criar Notícia
          </button>
          <button
            className={abaAtiva === "dashboard" ? "aba-ativa" : ""}
            onClick={() => setAbaAtiva("dashboard")}
          >
            Dashboard
          </button>
          <button
            className={abaAtiva === "configuracoes" ? "aba-ativa" : ""}
            onClick={() => setAbaAtiva("configuracoes")}
          >
            Configurações
          </button>
        </div>

        {abaAtiva === "board" && (
          <AreaBusca aoBuscar={buscarNoticias} buscando={etapaRodando !== null} />
        )}
      </header>

      {abaAtiva === "board" && (
        <>
          {etapaRodando && (
            <div className="progresso">
              <div className="progresso-barra" />
            </div>
          )}

          {erro && <div className="erro">{erro}</div>}
          {aviso && <div className="aviso">{aviso}</div>}

          <Board
            noticias={noticias}
            onAbrir={setSelecionada}
            onMoverNoticia={moverNoticia}
            onExcluir={excluir}
            processando={processando}
          />

          <DetalheModal
            noticia={selecionada}
            aoFechar={() => setSelecionada(null)}
            aoAprovar={aprovar}
            aprovando={aprovando}
            aoRegerarIcone={regerarIconeDaNoticia}
            regerandoIcone={regerandoIcone}
          />
        </>
      )}

      {abaAtiva === "criar-noticia" && (
        <CriarNoticia
          aoSalvar={async () => {
            await carregar();
            setAbaAtiva("board");
          }}
        />
      )}

      {abaAtiva === "dashboard" && (
        <Dashboard usoGroq={usoGroq} usoGemini={usoGemini} usoMistral={usoMistral} />
      )}

      {abaAtiva === "configuracoes" && <Configuracoes />}
    </div>
  );
}
