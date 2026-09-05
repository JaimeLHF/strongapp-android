# StrongApp — aplicativo Android

Aplicativo mobile nativo para acompanhamento de treinos de academia, escrito em
**Java** com **Android Studio** e **Gradle**. Consome a API REST em **Laravel**
do projeto StrongApp.

Trabalho da disciplina de Desenvolvimento Mobile — Atividade Avaliativa 2,
continuação do projeto entregue na Atividade Avaliativa 1.

## Requisitos funcionais

Os quinze requisitos são os do enunciado da Atividade Avaliativa 1. Todos atendidos.

| Código | Requisito | Situação | Onde |
|---|---|---|---|
| RF01 | Cadastro de usuário | Atendido | `ui/LoginActivity.java` |
| RF02 | Autenticação com sessão mantida entre acessos | Atendido | `ui/LoginActivity.java`, `data/SessionManager.java` |
| RF03 | Catálogo de exercícios | Atendido | `ui/ExercisesFragment.java` |
| RF04 | Gestão do catálogo: criar, editar e excluir exercícios | Atendido | `ui/ExercisesFragment.java` |
| RF05 | Busca por texto e filtro por grupo muscular | Atendido | `ui/ExercisesFragment.java` |
| RF06 | Montagem de treino com séries, repetições, carga e descanso | Atendido | `ui/CreateWorkoutActivity.java` |
| RF07 | Supersets | Atendido | `ui/CreateWorkoutActivity.java`, `ui/WorkoutDetailActivity.java` |
| RF08 | Progresso semanal por semana ISO | Atendido | `ui/WorkoutDetailActivity.java`, `util/IsoWeek.java` |
| RF09 | Cronômetro de descanso em botão flutuante | Atendido | `ui/TimerBottomSheet.java` |
| RF10 | Histórico e evolução em gráfico | Atendido | `ui/DashboardFragment.java`, `ui/LineChartView.java` |
| RF11 | Conquistas com barra de progresso | Atendido | `ui/AchievementsActivity.java`, `ui/Achievement.java` |
| RF12 | Perfil com nome, sobrenome, e-mail e data de cadastro | Atendido | `ui/ProfileFragment.java` |
| RF13 | Compartilhamento de progresso em imagem 9:16 | Atendido | `util/ProgressImage.java`, `ui/WorkoutDetailActivity.java` |
| RF14 | Tema claro e escuro preservando a escolha | Atendido | `util/ThemeMode.java`, `res/values-night/` |
| RF15 | Notificação de lembrete nos dias de treino | Atendido | `util/Reminders.java`, `util/ReminderReceiver.java` |

Fora do enunciado, o app também traz o diário do treino
(`ui/DiaryActivity.java`) e a exportação de dados (`ui/ExportActivity.java`),
que existem na versão web.

## Telas do aplicativo

Todas as telas da versão web têm equivalente aqui. A navegação inferior tem
quatro abas; o resto abre a partir delas.

| Tela | Onde | Equivalente na web |
|---|---|---|
| Login e cadastro | `ui/LoginActivity.java` | `/auth` |
| Início (dashboard) | `ui/DashboardFragment.java` | `/dashboard` |
| Treinos | `ui/WorkoutsFragment.java` | lista de treinos do dashboard |
| Exercícios | `ui/ExercisesFragment.java` | `/exercicios` |
| Perfil | `ui/ProfileFragment.java` | `/perfil` |
| Detalhe do treino | `ui/WorkoutDetailActivity.java` | `/treino/:id` |
| Criar e editar treino | `ui/CreateWorkoutActivity.java` | `/criar-treino`, `/treino/:id/editar` |
| Diário do treino | `ui/DiaryActivity.java` | `WorkoutDiary` no detalhe |
| Conquistas | `ui/AchievementsActivity.java` | `/conquistas` |
| Ranking da Comunidade | `ui/RankingActivity.java` | `UserRankingChart` no dashboard |
| Exportar Dados | `ui/ExportActivity.java` | `ExportData` no perfil |
| Compartilhar treino | `ui/WorkoutDetailActivity.java` | `/share/:token` |

Duas observações sobre fidelidade à versão web:

- O **Ranking da Comunidade** usa a mesma lista fictícia de demonstração da web.
  A API não expõe ranking entre usuários; nenhum número ali vem do banco.
- O **gráfico de evolução** marca um ponto por semana ISO, com a média de
  conclusão da semana. A web marca um ponto por registro, usando a data — mas
  `GET /progress` guarda ano e semana, não a data.

A tela 404 da web não tem equivalente: a navegação do app é fechada e não existe
rota inválida para cair.

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
├── ui/       Activities, Fragments, Adapters e o gráfico em Canvas
└── util/     Semana ISO-8601, tema, lembretes e a imagem de progresso
```

## Tecnologias

- Java 11, minSdk 24, compileSdk 37
- Retrofit 2 + Gson para o consumo da API
- OkHttp com interceptor que injeta o token Bearer do Laravel Sanctum
- Material Design 3, ViewBinding, RecyclerView
- Gráfico de progresso desenhado em `Canvas` numa View própria, sem biblioteca
- FileProvider para entregar relatório, CSV e a imagem de progresso pelo compartilhamento
- AlarmManager e notificação local para os lembretes nos dias de treino
- Tema escuro por padrão, seguindo o protótipo, com alternância na barra superior
