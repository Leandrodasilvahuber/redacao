import { useEffect, useState } from "react";
import { BASE_URL, buscarConfiguracoes, salvarConfiguracoes } from "./api";

const CRITERIOS_BUSCA = [
  { valor: "NOVIDADES", rotulo: "Novidades" },
  { valor: "NOSTALGIA", rotulo: "Nostalgia" },
  { valor: "TEORIAS", rotulo: "Teorias" },
  { valor: "FERRAMENTAS", rotulo: "Ferramentas" },
  { valor: "TECNICAS", rotulo: "Técnicas" },
];

const ESTILOS_ILUSTRACAO = [
  { valor: "ATUAL", rotulo: "Estilo atual" },
  { valor: "BITS_8", rotulo: "8 bits" },
  { valor: "BITS_16", rotulo: "16 bits" },
  { valor: "BITS_32", rotulo: "32 bits" },
  { valor: "CARTOON", rotulo: "Desenho cartoon" },
];

const PROVEDORES_ILUSTRACAO = [
  { valor: "GEMINI", rotulo: "Gemini (desenho SVG)" },
  { valor: "FLUX", rotulo: "Flux Schnell (imagem gerada)" },
];

const CHAVES_API = [
  { chave: "groqApiKey", rotulo: "Groq" },
  { chave: "geminiApiKey", rotulo: "Gemini" },
  { chave: "mistralApiKey", rotulo: "Mistral" },
];

const ILUSTRACOES_BLOG = [
  { valor: "BRAIN", rotulo: "Cérebro" },
  { valor: "CLOUD", rotulo: "Nuvem" },
  { valor: "TERMINAL", rotulo: "Terminal" },
  { valor: "GRAPH", rotulo: "Gráfico" },
  { valor: "BRANCH", rotulo: "Branch (git)" },
  { valor: "SHIELD", rotulo: "Escudo" },
];

function estadoInicial() {
  return {
    groqApiKey: "",
    geminiApiKey: "",
    mistralApiKey: "",
    criteriosBusca: [],
    revisarFonteVeridica: true,
    revisarEstrutura: true,
    revisarPadraoLinkedin: true,
    atribuirFonte: false,
    estiloIlustracao: "ATUAL",
    provedorIlustracao: "GEMINI",
    blogApiUrl: "",
    blogApiToken: "",
    blogIlustracaoPadrao: "TERMINAL",
    linkedinClientId: "",
    linkedinClientSecret: "",
  };
}

export default function Configuracoes() {
  const [form, setForm] = useState(estadoInicial());
  const [status, setStatus] = useState(estadoInicial());
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);
  const [mensagem, setMensagem] = useState(null);

  useEffect(() => {
    carregar();
  }, []);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const dados = await buscarConfiguracoes();
      setStatus(dados);
      setForm({
        groqApiKey: "",
        geminiApiKey: "",
        mistralApiKey: "",
        criteriosBusca: dados.criteriosBusca ?? [],
        revisarFonteVeridica: dados.revisarFonteVeridica,
        revisarEstrutura: dados.revisarEstrutura,
        revisarPadraoLinkedin: dados.revisarPadraoLinkedin,
        atribuirFonte: dados.atribuirFonte,
        estiloIlustracao: dados.estiloIlustracao ?? "ATUAL",
        provedorIlustracao: dados.provedorIlustracao ?? "GEMINI",
        blogApiUrl: dados.blogApiUrl ?? "",
        blogApiToken: "",
        blogIlustracaoPadrao: dados.blogIlustracaoPadrao ?? "TERMINAL",
        linkedinClientId: "",
        linkedinClientSecret: "",
      });
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  function alternarCriterio(valor) {
    setForm((atual) => ({
      ...atual,
      criteriosBusca: atual.criteriosBusca.includes(valor)
        ? atual.criteriosBusca.filter((c) => c !== valor)
        : [...atual.criteriosBusca, valor],
    }));
  }

  async function salvar() {
    setSalvando(true);
    setErro(null);
    setMensagem(null);
    try {
      const atualizado = await salvarConfiguracoes(form);
      setStatus(atualizado);
      setForm((atual) => ({
        ...atual,
        groqApiKey: "",
        geminiApiKey: "",
        mistralApiKey: "",
        blogApiToken: "",
        linkedinClientId: "",
        linkedinClientSecret: "",
      }));
      setMensagem("Configurações salvas com sucesso.");
    } catch (e) {
      setErro(e.message);
    } finally {
      setSalvando(false);
    }
  }

  const mascaraPorChave = {
    groqApiKey: status.groqApiKeyMascarada,
    geminiApiKey: status.geminiApiKeyMascarada,
    mistralApiKey: status.mistralApiKeyMascarada,
  };
  const configuradaPorChave = {
    groqApiKey: status.groqApiKeyConfigurada,
    geminiApiKey: status.geminiApiKeyConfigurada,
    mistralApiKey: status.mistralApiKeyConfigurada,
  };

  if (carregando) {
    return <div className="configuracoes">Carregando configurações...</div>;
  }

  return (
    <div className="configuracoes">
      {erro && <div className="erro">{erro}</div>}
      {mensagem && <div className="mensagem-sucesso">{mensagem}</div>}

      <section className="config-secao">
        <h2>Chaves de API</h2>
        {CHAVES_API.map(({ chave, rotulo }) => (
          <div className="config-campo" key={chave}>
            <label htmlFor={chave}>
              {rotulo}
              {configuradaPorChave[chave] && (
                <span className="config-chave-status"> — configurada ({mascaraPorChave[chave]})</span>
              )}
            </label>
            <input
              id={chave}
              type="password"
              placeholder={configuradaPorChave[chave] ? "Deixe em branco para manter a atual" : "Cole a chave aqui"}
              value={form[chave]}
              onChange={(e) => setForm((atual) => ({ ...atual, [chave]: e.target.value }))}
              autoComplete="off"
            />
          </div>
        ))}
      </section>

      <section className="config-secao">
        <h2>Busca</h2>
        <p className="config-descricao">Tipos de notícia a priorizar na seleção.</p>
        <div className="config-checkboxes">
          {CRITERIOS_BUSCA.map(({ valor, rotulo }) => (
            <label className="config-checkbox" key={valor}>
              <input
                type="checkbox"
                checked={form.criteriosBusca.includes(valor)}
                onChange={() => alternarCriterio(valor)}
              />
              {rotulo}
            </label>
          ))}
        </div>
      </section>

      <section className="config-secao">
        <h2>Revisão</h2>
        <div className="config-checkboxes">
          <label className="config-checkbox">
            <input
              type="checkbox"
              checked={form.revisarFonteVeridica}
              onChange={(e) => setForm((atual) => ({ ...atual, revisarFonteVeridica: e.target.checked }))}
            />
            Tem fonte verídica (com rechecagem)
          </label>
          <label className="config-checkbox">
            <input
              type="checkbox"
              checked={form.revisarEstrutura}
              onChange={(e) => setForm((atual) => ({ ...atual, revisarEstrutura: e.target.checked }))}
            />
            Está bem estruturada
          </label>
          <label className="config-checkbox">
            <input
              type="checkbox"
              checked={form.revisarPadraoLinkedin}
              onChange={(e) => setForm((atual) => ({ ...atual, revisarPadraoLinkedin: e.target.checked }))}
            />
            Está no padrão LinkedIn
          </label>
        </div>
      </section>

      <section className="config-secao">
        <h2>Redação</h2>
        <label className="config-checkbox">
          <input
            type="checkbox"
            checked={form.atribuirFonte}
            onChange={(e) => setForm((atual) => ({ ...atual, atribuirFonte: e.target.checked }))}
          />
          Atribuir fonte da notícia no texto final
        </label>
      </section>

      <section className="config-secao">
        <h2>Ilustrar</h2>
        <p className="config-descricao">Quem desenha a ilustração de capa dos posts.</p>
        <div className="config-checkboxes">
          {PROVEDORES_ILUSTRACAO.map(({ valor, rotulo }) => (
            <label className="config-radio" key={valor}>
              <input
                type="radio"
                name="provedorIlustracao"
                checked={form.provedorIlustracao === valor}
                onChange={() => setForm((atual) => ({ ...atual, provedorIlustracao: valor }))}
              />
              {rotulo}
            </label>
          ))}
        </div>
        <p className="config-descricao">
          Se o Gemini estiver selecionado e estourar a cota do dia, o sistema cai automaticamente para o
          Flux Schnell (sem chave, praticamente ilimitado).
        </p>
        <p className="config-descricao">Estilo visual usado no desenho, com os dois provedores.</p>
        <div className="config-checkboxes">
          {ESTILOS_ILUSTRACAO.map(({ valor, rotulo }) => (
            <label className="config-radio" key={valor}>
              <input
                type="radio"
                name="estiloIlustracao"
                checked={form.estiloIlustracao === valor}
                onChange={() => setForm((atual) => ({ ...atual, estiloIlustracao: valor }))}
              />
              {rotulo}
            </label>
          ))}
        </div>
      </section>

      <section className="config-secao">
        <h2>Blog</h2>
        <div className="config-campo">
          <label htmlFor="blogApiUrl">URL do blog</label>
          <input
            id="blogApiUrl"
            type="text"
            placeholder="https://leandrohuber.duckdns.org"
            value={form.blogApiUrl}
            onChange={(e) => setForm((atual) => ({ ...atual, blogApiUrl: e.target.value }))}
          />
        </div>
        <div className="config-campo">
          <label htmlFor="blogApiToken">
            Token de acesso
            {status.blogApiTokenConfigurado && (
              <span className="config-chave-status"> — configurado ({status.blogApiTokenMascarado})</span>
            )}
          </label>
          <input
            id="blogApiToken"
            type="password"
            placeholder={status.blogApiTokenConfigurado ? "Deixe em branco para manter o atual" : "Cole o token aqui"}
            value={form.blogApiToken}
            onChange={(e) => setForm((atual) => ({ ...atual, blogApiToken: e.target.value }))}
            autoComplete="off"
          />
        </div>
        <p className="config-descricao">Ícone padrão usado nos posts publicados no blog.</p>
        <div className="config-checkboxes">
          {ILUSTRACOES_BLOG.map(({ valor, rotulo }) => (
            <label className="config-radio" key={valor}>
              <input
                type="radio"
                name="blogIlustracaoPadrao"
                checked={form.blogIlustracaoPadrao === valor}
                onChange={() => setForm((atual) => ({ ...atual, blogIlustracaoPadrao: valor }))}
              />
              {rotulo}
            </label>
          ))}
        </div>
      </section>

      <section className="config-secao">
        <h2>LinkedIn</h2>
        {status.linkedinConectado ? (
          <p className="config-descricao">
            ✅ Conectado como <strong>{status.linkedinPersonUrn}</strong>
            {status.linkedinTokenExpiraEm && (
              <> — acesso válido até {new Date(status.linkedinTokenExpiraEm).toLocaleString()}</>
            )}
          </p>
        ) : (
          <p className="config-descricao">LinkedIn ainda não conectado.</p>
        )}
        <div className="config-campo">
          <label htmlFor="linkedinClientId">
            Client ID
            {status.linkedinClientIdConfigurado && (
              <span className="config-chave-status"> — configurado ({status.linkedinClientIdMascarado})</span>
            )}
          </label>
          <input
            id="linkedinClientId"
            type="password"
            placeholder={status.linkedinClientIdConfigurado ? "Deixe em branco para manter o atual" : "Client ID do app LinkedIn"}
            value={form.linkedinClientId}
            onChange={(e) => setForm((atual) => ({ ...atual, linkedinClientId: e.target.value }))}
            autoComplete="off"
          />
        </div>
        <div className="config-campo">
          <label htmlFor="linkedinClientSecret">
            Client Secret
            {status.linkedinClientSecretConfigurado && (
              <span className="config-chave-status"> — configurado ({status.linkedinClientSecretMascarado})</span>
            )}
          </label>
          <input
            id="linkedinClientSecret"
            type="password"
            placeholder={status.linkedinClientSecretConfigurado ? "Deixe em branco para manter o atual" : "Client Secret do app LinkedIn"}
            value={form.linkedinClientSecret}
            onChange={(e) => setForm((atual) => ({ ...atual, linkedinClientSecret: e.target.value }))}
            autoComplete="off"
          />
        </div>
        <p className="config-descricao">Salve o Client ID e Secret antes de conectar.</p>
        <a className="config-botao-linkedin" href={`${BASE_URL}/linkedin/conectar`}>
          {status.linkedinConectado ? "Reconectar LinkedIn" : "Conectar LinkedIn"}
        </a>
      </section>

      <button className="config-salvar" onClick={salvar} disabled={salvando}>
        {salvando ? "Salvando..." : "Salvar configurações"}
      </button>
    </div>
  );
}
