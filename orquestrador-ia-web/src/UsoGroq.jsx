function Barra({ rotulo, usado, limite }) {
  const percentual = limite > 0 ? Math.min(100, Math.round((usado / limite) * 100)) : 0;
  const critico = percentual >= 90;

  return (
    <div className="uso-groq-item">
      <div className="uso-groq-rotulo">
        <span>{rotulo}</span>
        <span>
          {usado.toLocaleString("pt-BR")} / {limite.toLocaleString("pt-BR")}
        </span>
      </div>
      <div className="uso-groq-barra-fundo">
        <div
          className={`uso-groq-barra-preenchida${critico ? " critico" : ""}`}
          style={{ width: `${percentual}%` }}
        />
      </div>
    </div>
  );
}

export default function UsoGroq({ titulo, uso }) {
  if (!uso) return null;

  return (
    <div className="uso-groq">
      {titulo && <strong className="uso-groq-titulo">{titulo}</strong>}
      <Barra rotulo="Requisições hoje" usado={uso.requisicoesHoje} limite={uso.limiteRequisicoesPorDia} />
      <Barra rotulo="Tokens hoje" usado={uso.tokensHoje} limite={uso.limiteTokensPorDia} />
      <Barra rotulo="Requisições/min" usado={uso.requisicoesUltimoMinuto} limite={uso.limiteRequisicoesPorMinuto} />
      <Barra rotulo="Tokens/min" usado={uso.tokensUltimoMinuto} limite={uso.limiteTokensPorMinuto} />
    </div>
  );
}
