import UsoGroq from "./UsoGroq";
import UsoMistral from "./UsoMistral";

const PROVEDORES = [
  { chave: "groq", nome: "Groq", cor: "var(--dash-groq)" },
  { chave: "gemini", nome: "Gemini", cor: "var(--dash-gemini)" },
  { chave: "mistral", nome: "Mistral", cor: "var(--dash-mistral)" },
];

function formatarNumero(valor) {
  return valor.toLocaleString("pt-BR");
}

function percentualCota(uso, chave) {
  if (!uso) return null;
  if (chave === "mistral") {
    return uso.limiteSeguroUsd > 0 ? Math.min(100, (uso.custoHojeUsd / uso.limiteSeguroUsd) * 100) : 0;
  }
  return uso.limiteRequisicoesPorDia > 0
    ? Math.min(100, (uso.requisicoesHoje / uso.limiteRequisicoesPorDia) * 100)
    : 0;
}

function tokensHoje(uso, chave) {
  if (!uso) return 0;
  if (chave === "mistral") return (uso.tokensEntradaHoje ?? 0) + (uso.tokensSaidaHoje ?? 0);
  return uso.tokensHoje ?? 0;
}

function CardProvedor({ chave, nome, cor, uso, percentual }) {
  const semDados = !uso;
  const critico = percentual !== null && percentual >= 90;

  return (
    <div className="dash-card" style={{ "--dash-cor": cor }}>
      <div className="dash-card-topo">
        <span className="dash-card-ponto" />
        <strong>{nome}</strong>
      </div>
      {semDados ? (
        <p className="dash-card-vazio">Sem dados ainda</p>
      ) : (
        <>
          <div className={`dash-card-hero${critico ? " critico" : ""}`}>
            {Math.round(percentual)}%
          </div>
          <p className="dash-card-legenda">da cota diária usada hoje</p>
          <div className="dash-meter-trilho">
            <div
              className={`dash-meter-preenchido${critico ? " critico" : ""}`}
              style={{ width: `${Math.max(2, percentual)}%` }}
            />
          </div>
          <p className="dash-card-detalhe">
            {formatarNumero(uso.requisicoesHoje)} requisições · {formatarNumero(tokensHoje(uso, chave))} tokens
          </p>
        </>
      )}
    </div>
  );
}

function GraficoComparativo({ titulo, dados }) {
  const maximo = Math.max(1, ...dados.map((d) => d.valor));
  const alturaLinha = 30;
  const alturaSvg = dados.length * alturaLinha;

  return (
    <div className="dash-grafico">
      <h3>{titulo}</h3>
      <svg viewBox={`0 0 300 ${alturaSvg}`} className="dash-grafico-svg" role="img" aria-label={titulo}>
        {dados.map((d, i) => {
          const y = i * alturaLinha;
          const largura = Math.max(3, (d.valor / maximo) * 170);
          return (
            <g key={d.label}>
              <title>{`${d.label}: ${formatarNumero(d.valor)}`}</title>
              <text x="0" y={y + 15} className="dash-grafico-rotulo">
                {d.label}
              </text>
              <rect x="62" y={y + 8} width="170" height="14" rx="7" className="dash-grafico-trilho" />
              <rect x="62" y={y + 8} width={largura} height="14" rx="7" fill={d.cor} />
              <text x={62 + largura + 8} y={y + 15} className="dash-grafico-valor">
                {formatarNumero(d.valor)}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

export default function Dashboard({ usoGroq, usoGemini, usoMistral }) {
  const usoPorChave = { groq: usoGroq, gemini: usoGemini, mistral: usoMistral };
  const semDados = !usoGroq && !usoGemini && !usoMistral;

  const dadosRequisicoes = PROVEDORES.filter((p) => usoPorChave[p.chave]).map((p) => ({
    label: p.nome,
    valor: usoPorChave[p.chave].requisicoesHoje ?? 0,
    cor: p.cor,
  }));

  const dadosTokens = PROVEDORES.filter((p) => usoPorChave[p.chave]).map((p) => ({
    label: p.nome,
    valor: tokensHoje(usoPorChave[p.chave], p.chave),
    cor: p.cor,
  }));

  return (
    <div className="dashboard">
      <div className="config-secao">
        <h2>Monitoramento de IA</h2>
        <p className="config-descricao">
          Consumo de cota das IAs usadas no pipeline (Groq e Gemini pro fluxo principal, Mistral como fallback).
        </p>

        {semDados ? (
          <p className="config-descricao">Carregando métricas de uso...</p>
        ) : (
          <>
            <div className="dash-legenda">
              {PROVEDORES.map((p) => (
                <span key={p.chave} className="dash-legenda-item">
                  <span className="dash-legenda-ponto" style={{ background: p.cor }} />
                  {p.nome}
                </span>
              ))}
            </div>

            <div className="dash-cards">
              {PROVEDORES.map((p) => (
                <CardProvedor
                  key={p.chave}
                  chave={p.chave}
                  nome={p.nome}
                  cor={p.cor}
                  uso={usoPorChave[p.chave]}
                  percentual={percentualCota(usoPorChave[p.chave], p.chave)}
                />
              ))}
            </div>

            <div className="dash-graficos">
              {dadosRequisicoes.length > 0 && (
                <GraficoComparativo titulo="Requisições hoje" dados={dadosRequisicoes} />
              )}
              {dadosTokens.length > 0 && <GraficoComparativo titulo="Tokens hoje" dados={dadosTokens} />}
            </div>
          </>
        )}
      </div>

      <div className="config-secao">
        <h2>Detalhes por provedor</h2>
        <div className="dashboard-uso">
          <UsoGroq titulo="Groq" uso={usoGroq} />
          <UsoGroq titulo="Gemini" uso={usoGemini} />
          <UsoMistral uso={usoMistral} />
        </div>
      </div>
    </div>
  );
}
