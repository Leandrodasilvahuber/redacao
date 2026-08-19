#!/bin/bash
# Inicia o backend (Spring Boot) e o frontend (Vite) do Orquestrador IA
# e abre o navegador quando tudo estiver pronto.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$DIR/orquestrador-ia"
FRONTEND_DIR="$DIR/orquestrador-ia-web"
FRONTEND_URL="http://localhost:5180"
BACKEND_URL="http://localhost:8080"

cleanup() {
    echo "Encerrando..."
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null
    [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null
}
trap cleanup EXIT INT TERM

echo "Iniciando backend..."
(cd "$BACKEND_DIR" && ./mvnw spring-boot:run) &
BACKEND_PID=$!

echo "Iniciando frontend..."
(cd "$FRONTEND_DIR" && npm run dev) &
FRONTEND_PID=$!

echo "Aguardando backend responder em $BACKEND_URL ..."
for i in $(seq 1 60); do
    curl -s -o /dev/null "$BACKEND_URL" && break
    sleep 2
done

echo "Aguardando frontend responder em $FRONTEND_URL ..."
for i in $(seq 1 30); do
    curl -s -o /dev/null "$FRONTEND_URL" && break
    sleep 1
done

echo "Abrindo navegador em $FRONTEND_URL ..."
xdg-open "$FRONTEND_URL" >/dev/null 2>&1

echo "Sistema em execucao. Feche esta janela para encerrar backend e frontend."
wait
