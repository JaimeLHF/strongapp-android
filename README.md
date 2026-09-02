# StrongApp — aplicativo Android

Aplicativo mobile nativo para acompanhamento de treinos de academia, escrito em
**Java** com **Android Studio** e **Gradle**. Consome a API REST em **Laravel**
do projeto StrongApp.

Trabalho da disciplina de Desenvolvimento Mobile — Atividade Avaliativa 2,
continuação do projeto entregue na Atividade Avaliativa 1.

## Requisitos implementados

| Código | Requisito | Onde |
|---|---|---|
| RF01 | Cadastro de usuário | `ui/LoginActivity.java` |
| RF02 | Autenticação com sessão mantida entre acessos | `ui/LoginActivity.java`, `data/SessionManager.java` |
| RF03 | Catálogo de exercícios | `ui/ExercisesFragment.java` |
| RF05 | Busca por texto e filtro por grupo muscular | `ui/ExercisesFragment.java` |
| RF06 | Montagem de treino com séries, repetições, carga e descanso | `ui/CreateWorkoutActivity.java` |
| RF08 | Progresso semanal calculado por semana ISO | `ui/WorkoutDetailActivity.java`, `util/IsoWeek.java` |
| RF09 | Cronômetro de descanso em botão flutuante | `ui/TimerBottomSheet.java` |
| RF12 | Perfil com estatísticas e edição de nome | `ui/ProfileFragment.java` |

## Como rodar

1. Suba a API Laravel do StrongApp:

```
php artisan serve --host=0.0.0.0 --port=8000
```

2. Abra este projeto no Android Studio e rode em um emulador ou aparelho.

3. Se o app não conectar, toque em **Servidor** na tela de login e informe o
   endereço da API:

   - Emulador: `10.0.2.2:8000` (é o padrão já configurado)
   - Aparelho físico: o IP da máquina na rede, por exemplo `192.168.0.10:8000`

## Estrutura

```
app/src/main/java/br/com/strongapp/
├── data/     Retrofit, interceptor de token, sessão
├── model/    Objetos de transferência da API
├── ui/       Activities, Fragments e Adapters
└── util/     Semana ISO-8601
```

## Tecnologias

- Java 11, minSdk 24, compileSdk 37
- Retrofit 2 + Gson para o consumo da API
- OkHttp com interceptor que injeta o token Bearer do Laravel Sanctum
- Material Design 3, ViewBinding, RecyclerView
- Tema claro e escuro seguindo a paleta do StrongApp web
