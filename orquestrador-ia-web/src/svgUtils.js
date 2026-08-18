export function listaDeIlustracoes(svgIlustracao) {
  if (!svgIlustracao) return [];
  try {
    const dados = JSON.parse(svgIlustracao);
    return Array.isArray(dados) ? dados.filter(Boolean) : [svgIlustracao];
  } catch {
    return [svgIlustracao];
  }
}

export function prepararSvgParaExibicao(svg) {
  if (!svg) return "";
  let limpo = svg
    .replace(/<script[\s\S]*?<\/script>/gi, "")
    .replace(/\son\w+="[^"]*"/gi, "")
    .replace(/\son\w+='[^']*'/gi, "");

  limpo = limpo.replace(/<svg([^>]*)>/i, (match, atributos) => {
    const semTamanho = atributos
      .replace(/\swidth="[^"]*"/i, "")
      .replace(/\sheight="[^"]*"/i, "")
      .replace(/\spreserveAspectRatio="[^"]*"/i, "");
    return `<svg${semTamanho} width="100%" height="100%" preserveAspectRatio="xMidYMid slice">`;
  });

  return limpo;
}

export function rasterizarSvgParaPng(svg, largura = 1200, altura = 627) {
  return new Promise((resolve, reject) => {
    if (!svg) {
      resolve(null);
      return;
    }
    const blob = new Blob([svg], { type: "image/svg+xml" });
    const url = URL.createObjectURL(blob);
    const imagem = new Image();
    imagem.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = largura;
      canvas.height = altura;
      const contexto = canvas.getContext("2d");
      contexto.fillStyle = "#ffffff";
      contexto.fillRect(0, 0, largura, altura);
      contexto.drawImage(imagem, 0, 0, largura, altura);
      URL.revokeObjectURL(url);
      resolve(canvas.toDataURL("image/png"));
    };
    imagem.onerror = (e) => {
      URL.revokeObjectURL(url);
      reject(e);
    };
    imagem.src = url;
  });
}
