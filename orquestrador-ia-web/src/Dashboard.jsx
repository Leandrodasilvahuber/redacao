import UsoGroq from "./UsoGroq";
import UsoMistral from "./UsoMistral";

export default function Dashboard({ usoGroq, usoGemini, usoMistral }) {
  const semDados = !usoGroq && !usoGemini && !usoMistral;

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
          <div className="dashboard-uso">
            <UsoGroq titulo="Groq" uso={usoGroq} />
            <UsoGroq titulo="Gemini" uso={usoGemini} />
            <UsoMistral uso={usoMistral} />
          </div>
        )}
      </div>
    </div>
  );
}
