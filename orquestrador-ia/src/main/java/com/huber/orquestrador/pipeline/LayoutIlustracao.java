package com.huber.orquestrador.pipeline;

/**
 * Os 4 layouts fixos usados para montar a capa dos posts. A IA só escolhe qual usar e o
 * "acabamento" (cores e fonte); a composição em si é sempre um destes templates, garantindo que o
 * resultado nunca fique quebrado ou fora do padrão como acontecia com o desenho livre.
 */
enum LayoutIlustracao {

    CENTRAL(600, 210, 260) {
        @Override
        String montar(String icone, String tituloTspans) {
            return """
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 627">
                      <defs>
                        <linearGradient id="bg" x1="0" y1="0" x2="1200" y2="627" gradientUnits="userSpaceOnUse">
                          <stop offset="0" stop-color="{{BG1}}"/>
                          <stop offset="1" stop-color="{{BG2}}"/>
                        </linearGradient>
                      </defs>
                      <rect width="1200" height="627" fill="url(#bg)"/>
                      <circle cx="600" cy="210" r="190" fill="{{ACCENT}}" opacity="0.16"/>
                      %s
                      <text x="600" y="470" text-anchor="middle" font-family="{{FONT}}" font-size="46" font-weight="700" fill="{{TEXT}}">%s</text>
                    </svg>
                    """.formatted(icone, tituloTspans);
        }
    },
    LATERAL(210, 313, 220) {
        @Override
        String montar(String icone, String tituloTspans) {
            return """
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 627">
                      <rect width="1200" height="627" fill="{{BG1}}"/>
                      <rect x="0" y="0" width="440" height="627" fill="{{BG2}}"/>
                      <circle cx="210" cy="313" r="160" fill="{{ACCENT}}" opacity="0.22"/>
                      %s
                      <text x="500" y="290" text-anchor="start" font-family="{{FONT}}" font-size="44" font-weight="700" fill="{{TEXT}}">%s</text>
                    </svg>
                    """.formatted(icone, tituloTspans);
        }
    },
    DIAGONAL(940, 190, 210) {
        @Override
        String montar(String icone, String tituloTspans) {
            return """
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 627">
                      <rect width="1200" height="627" fill="{{BG1}}"/>
                      <polygon points="0,627 1200,150 1200,627" fill="{{BG2}}" opacity="0.9"/>
                      <rect x="800" y="60" width="260" height="260" rx="36" fill="{{ACCENT}}" opacity="0.24"/>
                      %s
                      <text x="90" y="480" text-anchor="start" font-family="{{FONT}}" font-size="44" font-weight="700" fill="{{TEXT}}">%s</text>
                    </svg>
                    """.formatted(icone, tituloTspans);
        }
    },
    GRADE(600, 220, 230) {
        @Override
        String montar(String icone, String tituloTspans) {
            return """
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 627">
                      <defs>
                        <pattern id="pontos" width="40" height="40" patternUnits="userSpaceOnUse">
                          <circle cx="2" cy="2" r="2" fill="{{BG2}}" opacity="0.35"/>
                        </pattern>
                      </defs>
                      <rect width="1200" height="627" fill="{{BG1}}"/>
                      <rect width="1200" height="627" fill="url(#pontos)"/>
                      <rect x="450" y="80" width="300" height="300" rx="48" fill="{{ACCENT}}" opacity="0.2"/>
                      %s
                      <text x="600" y="490" text-anchor="middle" font-family="{{FONT}}" font-size="44" font-weight="700" fill="{{TEXT}}">%s</text>
                    </svg>
                    """.formatted(icone, tituloTspans);
        }
    };

    final double iconeCx;
    final double iconeCy;
    final double iconeTamanho;

    LayoutIlustracao(double iconeCx, double iconeCy, double iconeTamanho) {
        this.iconeCx = iconeCx;
        this.iconeCy = iconeCy;
        this.iconeTamanho = iconeTamanho;
    }

    abstract String montar(String icone, String tituloTspans);

    static LayoutIlustracao doIndice(int indice1a4) {
        LayoutIlustracao[] valores = values();
        int i = Math.max(1, Math.min(valores.length, indice1a4)) - 1;
        return valores[i];
    }
}
