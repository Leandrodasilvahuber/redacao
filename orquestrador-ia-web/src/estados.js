export const COLUNAS = [
  { estado: "BUSCADA", titulo: "Buscada", cor: "#64748b" },
  { estado: "SELECIONADA", titulo: "Selecionada", cor: "#0ea5e9" },
  { estado: "REDIGIDA", titulo: "Redigida", cor: "#8b5cf6" },
  { estado: "REVISADA", titulo: "Revisada", cor: "#f59e0b" },
  { estado: "PRONTA_PARA_PUBLICAR", titulo: "Pronta para publicar", cor: "#22c55e" },
  { estado: "PUBLICADA", titulo: "Publicada", cor: "#16a34a" },
];

export const TEXTO_POR_ESTADO = (noticia) => {
  switch (noticia.estado) {
    case "REDIGIDA":
      return noticia.textoRedigido;
    case "REVISADA":
      return noticia.textoRevisado;
    case "PRONTA_PARA_PUBLICAR":
    case "PUBLICADA":
      return noticia.textoFinal;
    default:
      return noticia.resumoOriginal;
  }
};
