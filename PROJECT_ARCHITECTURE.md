# PROJECT_ARCHITECTURE.md — Controle Obras

Documento vivo. Atualizar sempre que uma decisão de arquitetura for tomada ou alterada. Complementa `CLAUDE_CONTEXT.md` (visão geral) e `DEVELOPMENT_PLAN.md` (roadmap detalhado).

Data da análise: 2026-07-07

---

## 1. Estado atual do projeto

Análise ao repositório em `C:\Users\Estagio\Projetos\ControleObras`.

**Toolchain**
- AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01
- `compileSdk` 36 (sintaxe nova do AGP 9: `release(36) { minorApiLevel = 1 }`)
- `minSdk` 26, `targetSdk` 36
- Java/Kotlin target 11
- `buildTypes.release` usa `optimization { enable = false }` (API nova do AGP 9, substitui `minifyEnabled`) — R8/ProGuard desativado no release, como é normal num projeto ainda sem build de produção

**Estrutura**
- Módulo único `:app`, package `pt.controleobras.app` (Gradle Kotlin DSL, Version Catalog já em uso)
- Ficheiros de código = exatamente o template gerado pelo Android Studio: `MainActivity.kt` com o ecrã "Hello Android", tema Compose por defeito (`Color.kt`, `Theme.kt`, `Type.kt` com paleta Purple e dynamic color ativo, sem personalização da marca)
- Nenhum ecrã, ViewModel, repositório, modelo de dados ou navegação implementados

**Dependências (`libs.versions.toml`)**
- `androidx.core:core-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, `androidx.activity:activity-compose`, Compose BOM (`ui`, `material3`, `tooling`), testes por defeito (JUnit, Espresso)
- Nenhuma dependência de arquitetura: sem Navigation, sem DI, sem Room, sem CameraX, sem ML Kit, sem Kotlin Serialization, sem KSP/kapt

**Git**
- 1 commit (`Projeto inicial - Jetpack Compose`), branch `master`, remoto `github.com/RenatoPinto21/ControleObras` configurado e sincronizado
- Working tree com alterações não commitadas: vários ficheiros `.idea/*`, `gradlew.bat`, `gradle-wrapper.properties` modificados, e `CLAUDE_CONTEXT.md` novo e não rastreado
- `.gitignore` cobre apenas parte de `.idea/` (`caches`, `libraries`, `modules.xml`, `workspace.xml`, `navEditor.xml`, `assetWizardSettings.xml`); alguns destes já estavam commitados antes da regra existir, por isso continuam a aparecer como alterados

**Documentação**
- Só existe `CLAUDE_CONTEXT.md`. Os restantes ficheiros previstos na "Estrutura Esperada" (`README.md`, `PROJECT_ARCHITECTURE.md`, `DEVELOPMENT_PLAN.md`, `CLAUDE_RULES.md`, `ROADMAP.md`, `CHANGELOG.md`, `TODO.md`) ainda não existem

**Conclusão:** projeto limpo, sem dívida técnica, no estado exato em que o Android Studio o criou. Ponto de partida ideal para definir arquitetura de raiz — não há nada para migrar ou desfazer.

---

## 2. Arquitetura recomendada

**MVVM + Clean layering leve**, com fluxo unidirecional de dados (UDF):

```
UI (Compose)  →  ViewModel (StateFlow<UiState>)  →  Repository  →  DataSource (Room)
     ↑___________________ eventos/ações __________________|
```

- **UI**: composables "burros", sem lógica de negócio; observam `StateFlow` via `collectAsStateWithLifecycle`
- **ViewModel**: expõe estado imutável (`data class UiState`) e recebe eventos; não conhece Android framework além de `ViewModel`/`SavedStateHandle`
- **Domain**: começa como simples modelos (`core:model`) + classes de lógica isolada (parser de talões, validadores). Introduzir `UseCase` explícitos só quando a lógica for partilhada por mais de um ViewModel — evita camada vazia sem propósito, respeitando "nunca criar complexidade desnecessária"
- **Repository**: interface no domínio, implementação na camada de dados; esconde Room, ficheiros e (futuramente) rede
- **DataSource**: Room (local), sistema de ficheiros (imagens, JSON, XML)

**Como as tecnologias pedidas encaixam:**

| Tecnologia | Papel |
|---|---|
| CameraX | Captura a foto do talão → devolve `Uri` para a camada de dados |
| ML Kit Text Recognition (on-device) | Recebe a imagem, devolve texto bruto reconhecido — corre localmente, sem rede |
| Parser próprio (novo, na camada domain) | Interpreta o texto do OCR e extrai os campos estruturados (Empresa, NIF, Data, etc.) por heurísticas/regex |
| Room | Persiste o talão validado, os itens e a referência à imagem original |
| Kotlin Serialization | Serializa o modelo estruturado para JSON |
| XML | Sem biblioteca nativa em kotlinx.serialization; ver decisão na secção 6 |
| Navigation Compose | Navegação entre Home → Captura → Confirmação → Histórico |
| Hilt | Injeção de dependências entre as camadas acima, evitando acoplamento manual à medida que módulos crescem |

**Ponto em aberto para decisão do utilizador:** confirmar Hilt como DI. Alternativa seria DI manual (sem framework), mais simples agora mas com maior custo de refactor mais tarde, dado que o projeto vai crescer durante anos. Recomendação: Hilt desde a Fase 1.

---

## 3. Estrutura definitiva das pastas

Estrutura dentro do módulo único `:app`, organizada **por feature e por camada em simultâneo**, já preparada para ser extraída em módulos Gradle no futuro sem reescrever código (ver secção 4).

```
app/src/main/java/pt/controleobras/app/
│
├── ControleObrasApplication.kt          # @HiltAndroidApp
├── MainActivity.kt                      # NavHost root
│
├── core/
│   ├── designsystem/                    # Tema MD3, cores da marca, componentes reutilizáveis
│   │   ├── theme/ (Color.kt, Theme.kt, Type.kt)
│   │   └── components/
│   ├── common/                          # Extensions, Result<T>, dispatchers, formatação de datas/moeda
│   ├── navigation/                      # Rotas e grafo de navegação
│   ├── database/                        # Room: AppDatabase, DAOs, Entities, migrations
│   ├── model/                           # Data classes de domínio (Talao, ItemTalao, ...)
│   └── ocr/                             # Wrapper CameraX + ML Kit, isolado do resto da app
│
├── feature/
│   ├── receiptcapture/                  # Câmara + galeria + preview da imagem
│   │   ├── ui/
│   │   └── viewmodel/
│   ├── receiptreview/                   # Confirmação/edição dos campos extraídos
│   │   ├── ui/
│   │   └── viewmodel/
│   └── receiptlist/                     # Histórico de talões guardados
│       ├── ui/
│       └── viewmodel/
│
└── di/                                  # Módulos Hilt (Database, Repository, OCR)
```

Pastas `feature/obras`, `feature/clientes`, `feature/fornecedores`, `feature/materiais`, `feature/relatorios`, `feature/sync`, `feature/auth` serão adicionadas apenas quando essas fases do roadmap forem iniciadas — não criar agora (evita estrutura vazia sem propósito).

---

## 4. Módulos da aplicação

Duas opções válidas:

**Opção A — Multi-módulo Gradle desde já.** Cada pasta acima torna-se um módulo Gradle (`:core:database`, `:feature:receiptcapture`, etc.).
- Vantagens: build incremental mais rápido a longo prazo, fronteiras impostas pelo compilador, testável em isolamento desde o dia 1.
- Desvantagens: overhead de configuração Gradle desde já (build.gradle.kts por módulo, `api`/`implementation` a gerir), lentidão de configuração inicial sem benefício real enquanto o código é pouco — contraria a regra "nunca criar complexidade quando existe solução simples".

**Opção B (recomendada) — Módulo único agora, organizado exatamente com as fronteiras da secção 3; extração para módulos Gradle reais quando o projeto justificar** (referência prática: a partir da Fase 13, quando `feature/obras`, `feature/clientes`, etc. começarem a existir e o tempo de build começar a doer).
- Vantagens: simplicidade imediata, zero overhead de configuração, mesma disciplina de fronteiras (pastas = futuros módulos) sem o custo agora.
- Desvantagens: nada impede tecnicamente uma feature de aceder diretamente a outra sem passar pela camada `core` — mitigado por revisão de código disciplinada (nunca importar de `feature.x` dentro de `feature.y`).

**Recomendação: Opção B.** Migrar para módulos Gradle reais é um refactor mecânico (mover pastas, ajustar imports) porque as fronteiras já existem em pacotes — o custo de adiar é baixo, o ganho de simplicidade agora é alto.

Responsabilidade de cada módulo lógico (atual ou futuro):

- **core:designsystem** — tema Material 3 da marca, componentes Compose reutilizáveis (botões, cards, campos de formulário)
- **core:common** — utilitários sem dependência de Android framework quando possível
- **core:navigation** — grafo de rotas Navigation Compose
- **core:database** — Room: schema, DAOs, migrations. Único ponto de acesso a dados persistidos
- **core:model** — modelos de domínio partilhados entre features
- **core:ocr** — abstrai CameraX + ML Kit atrás de uma interface simples (`suspend fun recognizeText(uri): String`), para poder trocar o motor de OCR no futuro sem tocar nas features
- **feature:receiptcapture** — captura/seleção da imagem
- **feature:receiptreview** — confirmação e correção manual dos campos extraídos
- **feature:receiptlist** — histórico e (futuramente) relatórios
- **di** — módulos Hilt que ligam interfaces a implementações

---

## 6. Dependências

Nenhuma destas é adicionada agora — cada uma entra no `libs.versions.toml` apenas na fase do roadmap que a justifica (secção correspondente em `DEVELOPMENT_PLAN.md`).

| Dependência | Motivo |
|---|---|
| `androidx.navigation:navigation-compose` | Navegação entre ecrãs |
| `com.google.dagger:hilt-android` + `hilt-compiler` + `androidx.hilt:hilt-navigation-compose` | Injeção de dependências |
| `com.google.devtools.ksp` (plugin) | Processador de anotações para Room e Hilt, mais rápido que kapt |
| `androidx.room:room-runtime` + `room-ktx` + `room-compiler` (via KSP) | Persistência local, requisito explícito do projeto |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `ViewModel` + `collectAsStateWithLifecycle` em Compose |
| `androidx.camera:camera-core` + `camera-camera2` + `camera-lifecycle` + `camera-view` | Captura de fotografia (CameraX), requisito explícito |
| `com.google.mlkit:text-recognition` | OCR on-device, requisito explícito. **Nota de risco:** ver secção 7 sobre disponibilidade 100% offline |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `kotlin("plugin.serialization")` | Serialização JSON, requisito explícito |
| `androidx.compose.material3:material3` (photo picker) | Já presente; seleção de imagem da galeria usa a Photo Picker API do próprio Android, sem dependência extra |
| Testes: `kotlinx-coroutines-test`, `turbine` (opcional) | Testar `StateFlow`/`Flow` nos ViewModels e repositórios |

**XML — decisão a validar com o utilizador:** `kotlinx.serialization` não tem suporte XML oficial estável. Duas opções:
- (a) Serializer XML manual, escrito à mão dado o volume de campos ser pequeno e fixo (Empresa, NIF, Data, etc.) — sem dependência nova, controlo total
- (b) Biblioteca externa de serialização XML

Recomendação: (a), para não introduzir dependência não solicitada explicitamente pelo projeto.

---

## 7. Riscos técnicos

- **Toolchain muito recente** (AGP 9.2.1 com DSL ainda em evolução, Kotlin 2.2.10) — risco de incompatibilidades com KSP/Room/Hilt caso as versões não estejam alinhadas. Mitigação: validar compilação a cada fase antes de avançar, fixar versões testadas no Version Catalog.
- **ML Kit Text Recognition e offline real** — o modelo on-device pode precisar de ser transferido via Google Play Services na primeira utilização. Mitigação: testar em modo avião logo após a primeira instalação e documentar este requisito ao utilizador final.
- **Parsing heurístico de talões** — grande variabilidade de formatos de talões portugueses pode gerar extrações incorretas. Mitigação: parser extensível (uma estratégia por padrão de talão conhecido) e confirmação manual sempre disponível (já previsto no requisito).
- **ViewModels "gordos"** se toda a lógica ficar neles em vez de em classes de domínio dedicadas. Mitigação: isolar parsing/validação em classes próprias desde a Fase 8.
- **Ausência de testes automatizados desde o início** aumenta risco de regressão num projeto de vários anos. Mitigação: exigir testes unitários dos modelos e do parser a partir da Fase 3/8 (são as partes mais fáceis e valiosas de testar).
- **Migrations Room mal geridas** podem causar perda de dados em updates futuros. Mitigação: nunca usar `fallbackToDestructiveMigration()` depois da primeira versão em uso real; definir estratégia de migração explícita desde a Fase 4.

---

## 8. Boas práticas

- SOLID, Clean Code, MVVM, estado de UI imutável (`data class` + `StateFlow`), fluxo unidirecional
- Um ficheiro = uma responsabilidade; ViewModel sem chamadas diretas a APIs Android além de `ViewModel`/`SavedStateHandle`
- Toda a dependência nova passa pelo Version Catalog, nunca declarada diretamente no `build.gradle.kts`
- Compilar (`./gradlew assembleDebug`) no fim de cada fase antes de avançar
- Commits pequenos e descritivos por fase
- Strings sempre em `strings.xml`, nunca hardcoded no Compose
- Sem literais mágicos: constantes nomeadas para regex de parsing, formatos de data/moeda

---

## 9. Melhorias sugeridas

- Resolver já as alterações Git pendentes: decidir se `.idea/*` deve continuar versionado (recomendação: parar de rastrear `workspace.xml` e afins com `git rm --cached`, já estão listados no `.gitignore` mas foram commitados antes da regra existir)
- Criar os documentos de suporte já previstos na "Estrutura Esperada" do `CLAUDE_CONTEXT.md`: `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `TODO.md`, `CLAUDE_RULES.md`
- Personalizar o tema Material 3 (cores da marca Controle Obras) em vez de manter a paleta Purple gerada por defeito — evita retrabalho visual mais tarde
- Considerar GitHub Actions simples (`./gradlew build`) para detetar quebras de build cedo — sugerido para introduzir depois da Fase 4, não imediato
