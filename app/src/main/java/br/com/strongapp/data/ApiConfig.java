package br.com.strongapp.data;

/** Endereço padrão da API Laravel. Pode ser trocado em tempo de execução na tela de login. */
public final class ApiConfig {

    /** 10.0.2.2 é como o emulador do Android enxerga o localhost da máquina. */
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/";

    private ApiConfig() {
    }
}
