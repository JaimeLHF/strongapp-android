# Relatório Técnico — StrongApp Android

**Disciplina:** Desenvolvimento Mobile — Atividade Avaliativa 2
**Aluno:** Jaime Luiz Hansen Filho — 294833
**Repositório:** https://github.com/JaimeLHF/strongapp-android

---

## 1. Objetivo

Converter o StrongApp — aplicação web de acompanhamento de treinos de academia,
entregue na Atividade Avaliativa 1 — em um aplicativo móvel nativo, mantendo as
mesmas telas, as mesmas regras de negócio e a mesma API.

O resultado é um app Android em Java com 58 classes e 25 layouts, consumindo a
API REST em Laravel do projeto original.

---

## 2. Escolhas de projeto

### 2.1 Java nativo no lugar de React Native

O documento da Atividade Avaliativa 1 previa a conversão em **React Native**.
A entrega foi feita em **Java com Android Studio**, e essa mudança é deliberada:

- a disciplina é de desenvolvimento mobile nativo, e escrever em Java expõe o
  ciclo de vida de Activity e Fragment, o `RecyclerView`, o `AlarmManager` e o
  `Canvas` diretamente, sem a camada de abstração do framework;
- recursos exigidos pelo enunciado — notificação local agendada (RF15) e geração
  de imagem para compartilhamento (RF13) — em React Native dependeriam de
  bibliotecas de terceiros, enquanto no SDK nativo já existem prontos;
- não há reaproveitamento real de código com a web: a lógica de negócio mora no
  backend, então o argumento de "mesmo modelo de componentes" traria pouco.

O custo dessa escolha é que o app não compartilha código com a versão web e
precisa ser reescrito se um dia houver versão iOS.

### 2.2 Reaproveitar a API Laravel existente

O app não tem banco local nem regra de negócio duplicada. Tudo — catálogo,
treinos, marcações, progresso, diário, conquistas — vem dos 33 endpoints da API
que já servia a versão web. Isso garante que web e app mostrem sempre o mesmo
número e concentra as regras em um lugar só.

A consequência é que o app **não funciona offline**. Foi uma troca consciente:
consistência entre plataformas em vez de disponibilidade sem rede.

### 2.3 Endereço da API configurável em tempo de execução

`ApiConfig.DEFAULT_BASE_URL` aponta para `http://10.0.2.2:8000/api/`, que é como
o emulador enxerga o `localhost` da máquina. A tela de login tem um botão
**Servidor** que grava outro endereço em `SessionManager`, e `ApiClient`
reconstrói o Retrofit quando o endereço muda.

Sem isso, testar em aparelho físico exigiria recompilar o app a cada troca de IP
da rede.

### 2.4 Semana ISO-8601 com `Calendar`

O percentual de conclusão é calculado por semana ISO, igual à web. Como o
`minSdk` é 24, `java.time` não está disponível sem desugaring, então
`util/IsoWeek.java` usa `Calendar` com `setFirstDayOfWeek(MONDAY)` e
`setMinimalDaysInFirstWeek(4)`, tratando à mão a virada de ano — dias de janeiro
podem pertencer à última semana do ano anterior e vice-versa.

### 2.5 Gráfico desenhado no `Canvas`, sem biblioteca

O gráfico de evolução (RF10) é uma `View` própria, `ui/LineChartView.java`.
Nenhuma biblioteca de gráficos foi adicionada.

Motivos: o projeto precisa de exatamente um gráfico de linha com eixo fixo de 0 a
100%; uma biblioteca traria centenas de KB e um tema próprio para brigar com o
tema do app; e desenhar no `Canvas` deixa as cores saírem dos atributos do tema,
funcionando nos modos claro e escuro sem código extra.

O desenho segue regras simples de leitura: uma série só, então sem legenda —
o título acima do gráfico já a nomeia; grade e eixos discretos; e rótulo apenas
no último ponto, para o gráfico não virar uma lista de números.

### 2.6 Progresso semanal em vez de por data

A web marca um ponto por registro de progresso, usando a data. O app marca **um
ponto por semana ISO**, com a média de conclusão daquela semana.

Não é preferência estética: a tabela `workout_progress` guarda `year` e `week`,
não a data do registro. Plotar por data exigiria mudar o backend e a migração.
O formato semanal é o que os dados realmente suportam.

### 2.7 Lembretes com alarme inexato

`util/Reminders.java` agenda um `setInexactRepeating` por dia da semana marcado,
com intervalo de 7 dias, e `util/ReminderReceiver.java` publica a notificação.

O alarme **inexato** foi escolhido de propósito: a partir do Android 12 o alarme
exato exige a permissão `SCHEDULE_EXACT_ALARM`, que o usuário precisa conceder em
uma tela do sistema e que as lojas restringem. Para um lembrete de treino, alguns
minutos de atraso não têm importância, e o app evita pedir uma permissão sensível.

`util/BootReceiver.java` reagenda tudo depois do boot, porque o Android descarta
os alarmes ao reiniciar.

### 2.8 Tema escuro como padrão

O protótipo da Atividade Avaliativa 1 é inteiramente escuro. O tema escuro virou
o padrão do app (`SessionManager.isDarkMode()` devolve `true` quando não há
escolha salva), com `res/values-night/themes.xml` redefinindo apenas as cores de
superfície e texto. O botão de lua na barra superior alterna e a escolha
persiste, atendendo o RF14.

### 2.9 Uma tela para criar e editar treino

`CreateWorkoutActivity` aceita um extra `EXTRA_ID`. Sem ele, cria um treino com
`POST /workouts`; com ele, carrega o treino, preenche o formulário e salva com
`PUT /workouts/{id}`.

Duplicar a tela significaria manter dois formulários idênticos com quatro campos,
a lista de exercícios escolhidos e o agrupamento em superset.

---

## 3. Arquitetura

```
app/src/main/java/br/com/strongapp/
├── data/     Retrofit, interceptor de token, sessão
├── model/    Objetos de transferência da API
├── ui/       Activities, Fragments, Adapters e o gráfico em Canvas
└── util/     Semana ISO-8601, tema, lembretes e a imagem de progresso
```

**Camada de rede.** `ApiClient` monta um `OkHttpClient` com dois interceptores:
um injeta `Accept: application/json` e o `Authorization: Bearer` do token salvo,
outro registra as chamadas no logcat. `StrongApi` é a interface Retrofit com os
endpoints. `ApiClient.errorMessage()` traduz o corpo de erro do Laravel
(`{"message": ..., "errors": {...}}`) em uma frase legível ao usuário.

**Sessão.** `SessionManager` guarda em `SharedPreferences` o token do Sanctum, o
nome, o e-mail, o endereço da API e a preferência de tema. É o que mantém o
usuário logado entre acessos (RF02).

**Telas.** Quatro abas na navegação inferior — Início, Treinos, Exercícios e
Perfil — e o resto abre a partir delas. A lista completa está no `README.md`.

---

## 4. Problemas enfrentados e soluções

### 4.1 "Não foi possível falar com o servidor"

O app não conectava, com duas causas somadas:

1. A API subia com `php artisan serve` sem argumentos, ficando presa em
   `127.0.0.1` dentro do WSL2. Um processo assim **não** é alcançável a partir do
   Windows, e portanto nem do emulador. Solução: `--host=0.0.0.0`.
2. O app tinha gravado em `SharedPreferences` um endereço apontando para a porta
   **8001**, de um teste anterior, enquanto a API rodava na **8000**.

O segundo ponto expôs um defeito de usabilidade: o botão **Servidor** só existe
na tela de login, então um endereço errado gravado deixa o usuário preso depois
de autenticado. Fica registrado como limitação conhecida.

### 4.2 Campos numéricos trocando de linha no `RecyclerView`

Na montagem do treino, cada linha tem quatro campos editáveis. O `RecyclerView`
recicla as `View`, e os `TextWatcher` da linha anterior continuavam ativos,
gravando o valor digitado no exercício errado. Solução em
`PickedExerciseAdapter`: soltar os observadores antes de reescrever os campos.

### 4.3 `String.join` não existe no minSdk 24

`String.join` só existe a partir da API 26. Trocado por concatenação manual em
`Reminders.summary()`.

### 4.4 Título da `MaterialToolbar`

As telas novas apareciam sem título. A `Toolbar` do AndroidX lê `app:title`, não
`android:title`; o atributo estava no namespace errado e era silenciosamente
ignorado.

### 4.5 Rótulos sumindo na navegação inferior

Ao passar de três para quatro abas, o Material 3 escondeu o rótulo das abas não
selecionadas. Resolvido com `app:labelVisibilityMode="labeled"`.

---

## 5. Verificação

Não há suíte automatizada. Cada requisito foi verificado à mão no emulador
(`sdk_gphone16k_x86_64`, Android 16), dirigindo o app e conferindo o efeito na
API e no banco MySQL. Exemplos do que foi checado:

| O que | Como foi confirmado |
|---|---|
| Gestão do catálogo (RF04) | exercício criado e excluído pelo app, conferido na tabela `exercises` |
| Supersets (RF07) | grupo gravado em `workout_exercise_groups` com os dois exercícios ligados |
| Gráfico de evolução (RF10) | cinco semanas de progresso semeadas via API e conferidas na tela |
| Imagem de progresso (RF13) | PNG gerado com 1080x1920 exatos, proporção 9:16 |
| Lembretes (RF15) | `dumpsys alarm` mostrando dois alarmes semanais para o `ReminderReceiver` |
| Exportação (RF13 web) | CSV aberto e conferido linha a linha |

---

## 6. Limitações conhecidas

- **Ranking da Comunidade usa dados fictícios.** A API não expõe ranking entre
  usuários. A tela reproduz a mesma lista de demonstração da versão web, e isso
  está dito no código e no `README.md`. Tornar o ranking real exige um endpoint
  novo no backend.
- **Sem funcionamento offline.** Toda tela depende de rede.
- **O endereço do servidor só é editável antes do login.**
- **Sem testes automatizados.** A verificação foi manual.
- **A imagem de progresso é gerada em tamanho fixo**, sem opção de moldura ou cor.

---

## 7. Situação dos requisitos

Os quinze requisitos funcionais do enunciado estão atendidos. A tabela com o
arquivo responsável por cada um está no `README.md`.
