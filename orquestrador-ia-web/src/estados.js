export const PROXIMA_ETAPA = {
  BUSCADA: { destino: "SELECIONADA", etapa: "selecionar" },
  SELECIONADA: { destino: "REDIGIDA", etapa: "redigir" },
  REDIGIDA: { destino: "REVISADA", etapa: "revisar" },
  REVISADA: { destino: "ILUSTRADA", etapa: "ilustrar" },
  ILUSTRADA: { destino: "PRONTA_PARA_PUBLICAR", etapa: "publicar" },
  PRONTA_PARA_PUBLICAR: { destino: "PUBLICADA", etapa: "aprovar" },
};

export const FERRAMENTA_POR_ETAPA = {
  selecionar: "Selecionando com IA (Groq/Mistral)",
  redigir: "Redigindo com IA (Groq/Mistral)",
  revisar: "Revisando com IA (Groq/Mistral)",
  ilustrar: "Ilustrando com IA (Gemini/Mistral)",
  publicar: "Formatando para publicação (Groq/Mistral)",
  aprovar: "Aprovando",
};

export const COLUNAS = [
  { estado: "BUSCADA", titulo: "Buscada", cor: "#64748b" },
  { estado: "SELECIONADA", titulo: "Selecionada", cor: "#0ea5e9" },
  { estado: "REDIGIDA", titulo: "Redigida", cor: "#8b5cf6" },
  { estado: "REVISADA", titulo: "Revisada", cor: "#f59e0b" },
  { estado: "ILUSTRADA", titulo: "Ilustrar e adequar ao LinkedIn", cor: "#ec4899" },
  { estado: "PRONTA_PARA_PUBLICAR", titulo: "Pronta para publicar", cor: "#22c55e" },
  { estado: "PUBLICADA", titulo: "Publicada", cor: "#16a34a" },
];

export const TEXTO_POR_ESTADO = (noticia) => {
  switch (noticia.estado) {
    case "REDIGIDA":
      return noticia.textoRedigido;
    case "REVISADA":
      return noticia.textoRevisado;
    case "ILUSTRADA":
      return noticia.textoIlustrado;
    case "PRONTA_PARA_PUBLICAR":
    case "PUBLICADA":
      return noticia.textoFinal;
    default:
      return noticia.resumoOriginal;
  }
};
