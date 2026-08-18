function formatarUsd(valor) {
  if (valor > 0 && valor < 0.01) return `$${valor.toFixed(4)}`;
  return `$${valor.toFixed(2)}`;
}

export default function UsoMistral({ uso }) {
  if (!uso) return null;

  const percentualBruto = uso.limiteSeguroUsd > 0
    ? Math.min(100, (uso.custoHojeUsd / uso.limiteSeguroUsd) * 100)
    : 0;
  const percentual = percentualBruto > 0 ? Math.max(1, Math.round(percentualBruto)) : 0;
  const critico = percentualBruto >= 90;

  return (
    <div className="uso-groq uso-mistral">
      <strong className="uso-groq-titulo">Mistral (fallback)</strong>
      <div className="uso-groq-item uso-mistral-item">
        <div className="uso-groq-rotulo">
          <span>Gasto hoje (limite seguro / crédito total {formatarUsd(uso.creditoTotalUsd)})</span>
          <span>
            {formatarUsd(uso.custoHojeUsd)} / {formatarUsd(uso.limiteSeguroUsd)}
          </span>
        </div>
        <div className="uso-groq-barra-fundo">
          <div
            className={`uso-groq-barra-preenchida${critico ? " critico" : ""}`}
            style={{ width: `${percentual}%` }}
          />
        </div>
      </div>
      <span className="uso-mistral-detalhe">
        {uso.requisicoesHoje} requisições · {uso.tokensEntradaHoje.toLocaleString("pt-BR")} tokens entrada ·{" "}
        {uso.tokensSaidaHoje.toLocaleString("pt-BR")} tokens saída
      </span>
    </div>
  );
}
