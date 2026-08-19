# Orquestrador IA

Pipeline automatizado que busca notícias em feeds RSS, seleciona as mais relevantes, gera um texto autoral com IA, revisa, ilustra e publica (blog externo e/ou LinkedIn). Composto por um backend em Spring Boot e um frontend em React.

## Estrutura do projeto

```
redacao/
├── orquestrador-ia/       # Backend (Spring Boot / Java 17)
├── orquestrador-ia-web/   # Frontend (React + Vite)
└── iniciar.sh             # Sobe backend + frontend e abre no navegador
```

## Pipeline

1. **Busca** — coleta notícias dos feeds RSS configurados (`rss.feeds`).
2. **Seleção** — escolhe as notícias mais relevantes do dia (limite em `selecao.limite-diario`).
3. **Redação** — gera o texto usando Groq/Gemini/Mistral, respeitando limites de uso de cada provedor.
4. **Revisão** — revisa o texto gerado.
5. **Ilustração** — gera uma imagem via Ideogram, Gemini ou Flux (Pollinations), com fallback automático entre provedores.
6. **Publicação** — publica no blog externo e/ou no LinkedIn (OAuth).

## Pré-requisitos

- Java 17+
- Node.js 18+
- Chaves de API: Groq, Gemini e Mistral (obrigatórias); Ideogram e LinkedIn (opcionais)

## Configuração

Copie o arquivo de exemplo de variáveis de ambiente e preencha com suas chaves:

```bash
cp orquestrador-ia/.env.example orquestrador-ia/.env
```

Variáveis principais (veja `orquestrador-ia/.env.example` para a lista completa):

| Variável | Obrigatória | Descrição |
|---|---|---|
| `GROQ_API_KEY` | sim | Chave da API Groq |
| `GEMINI_API_KEY` | sim | Chave da API Gemini |
| `MISTRAL_API_KEY` | sim | Chave da API Mistral |
| `IDEOGRAM_API_KEY` | não | Habilita o Ideogram como provedor de ilustração |
| `LINKEDIN_CLIENT_ID` / `LINKEDIN_CLIENT_SECRET` | não | Necessárias para publicar no LinkedIn |
| `BLOG_API_URL` / `BLOG_API_TOKEN` | não | Necessárias para publicar no blog externo |

## Como executar

### Opção 1 — script único

```bash
./iniciar.sh
```

Sobe o backend (porta `8080`), o frontend (porta `5180`) e abre o navegador automaticamente. Também há um atalho de área de trabalho ("Orquestrador IA") que executa o mesmo script em um terminal.

### Opção 2 — manualmente

Backend:

```bash
cd orquestrador-ia
./mvnw spring-boot:run
```

Frontend:

```bash
cd orquestrador-ia-web
npm install
npm run dev
```

Acesse `http://localhost:5180`.

## Tecnologias

**Backend:** Spring Boot, Spring Data JPA, H2 (arquivo local), Rome (parsing de RSS), integrações com Groq, Gemini, Mistral, Ideogram e LinkedIn.

**Frontend:** React 19, Vite.

## Dados

O backend usa um banco H2 embarcado, persistido em arquivo local (`orquestrador-ia/data/orquestrador`).
