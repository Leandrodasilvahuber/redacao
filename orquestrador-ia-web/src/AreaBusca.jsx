import { useEffect, useState } from "react";
import { buscarConfiguracoes, salvarConfiguracoes } from "./api";

const CRITERIOS_BUSCA = [
  { valor: "NOVIDADES", rotulo: "Novidades" },
  { valor: "NOSTALGIA", rotulo: "Nostalgia" },
  { valor: "TEORIAS", rotulo: "Teorias" },
  { valor: "FERRAMENTAS", rotulo: "Ferramentas" },
  { valor: "TECNICAS", rotulo: "Técnicas" },
];

export default function AreaBusca({ aoBuscar, buscando }) {
  const [termo, setTermo] = useState("");
  const [criteriosBusca, setCriteriosBusca] = useState([]);
  const [carregandoCriterios, setCarregandoCriterios] = useState(true);
  const [salvandoCriterios, setSalvandoCriterios] = useState(false);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    buscarConfiguracoes()
      .then((dados) => setCriteriosBusca(dados.criteriosBusca ?? []))
      .catch((e) => setErro(e.message))
      .finally(() => setCarregandoCriterios(false));
  }, []);

  async function alternarCriterio(valor) {
    const anterior = criteriosBusca;
    const novaLista = anterior.includes(valor) ? anterior.filter((c) => c !== valor) : [...anterior, valor];
    setCriteriosBusca(novaLista);
    setSalvandoCriterios(true);
    setErro(null);
    try {
      const dadosAtuais = await buscarConfiguracoes();
      await salvarConfiguracoes({
        criteriosBusca: novaLista,
        revisarFonteVeridica: dadosAtuais.revisarFonteVeridica,
        revisarEstrutura: dadosAtuais.revisarEstrutura,
        revisarPadraoLinkedin: dadosAtuais.revisarPadraoLinkedin,
        atribuirFonte: dadosAtuais.atribuirFonte,
        blogApiUrl: dadosAtuais.blogApiUrl,
        cotaGroq: dadosAtuais.cotaGroq,
        cotaGemini: dadosAtuais.cotaGemini,
        cotaMistral: dadosAtuais.cotaMistral,
      });
    } catch (e) {
      setCriteriosBusca(anterior);
      setErro(e.message);
    } finally {
      setSalvandoCriterios(false);
    }
  }

  return (
    <div className="area-busca">
      {erro && <div className="erro">{erro}</div>}
      <div className="area-busca-linha">
        <input
          type="text"
          className="area-busca-input"
          placeholder="Filtrar por termo (opcional) — ex.: inteligência artificial"
          value={termo}
          onChange={(e) => setTermo(e.target.value)}
          disabled={buscando}
        />
        <button onClick={() => aoBuscar(termo)} disabled={buscando}>
          {buscando ? "Buscando..." : "Buscar notícias"}
        </button>
      </div>
      {!carregandoCriterios && (
        <div className="area-busca-criterios">
          <span className="area-busca-criterios-rotulo">
            Priorizar{salvandoCriterios ? " (salvando...)" : ""}:
          </span>
          {CRITERIOS_BUSCA.map(({ valor, rotulo }) => (
            <label className="config-checkbox" key={valor}>
              <input
                type="checkbox"
                checked={criteriosBusca.includes(valor)}
                onChange={() => alternarCriterio(valor)}
              />
              {rotulo}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
