function esgotadoGroqOuGemini(uso) {
  if (!uso) return false;
  return uso.requisicoesHoje >= uso.limiteRequisicoesPorDia || uso.tokensHoje >= uso.limiteTokensPorDia;
}

function esgotadoMistral(uso) {
  if (!uso) return false;
  return uso.custoHojeUsd >= uso.limiteSeguroUsd;
}

function Pill({ etapa, provedor, ativo }) {
  return (
    <span className={`indicador-ia-pill${ativo === "Mistral" ? " indicador-ia-mistral" : ""}`}>
      <span className="indicador-ia-etapa">{etapa}</span>
      <span className="indicador-ia-ponto" />
      {provedor}
    </span>
  );
}

export default function IndicadorIA({ usoGroq, usoGemini, usoMistral }) {
  if (!usoGroq && !usoGemini && !usoMistral) return null;

  const mistralEsgotado = esgotadoMistral(usoMistral);
  const provedorTexto = esgotadoGroqOuGemini(usoGroq) ? (mistralEsgotado ? "Indisponível" : "Mistral") : "Groq";
  const provedorCapa = esgotadoGroqOuGemini(usoGemini) ? (mistralEsgotado ? "Indisponível" : "Mistral") : "Gemini";

  return (
    <div className="indicador-ia">
      <Pill etapa="Texto" provedor={provedorTexto} ativo={provedorTexto} />
      <Pill etapa="Capa" provedor={provedorCapa} ativo={provedorCapa} />
    </div>
  );
}
