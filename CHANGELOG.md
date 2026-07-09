# CHANGELOG.md — Controle Obras

Registo cronológico de todas as alterações feitas ao projeto, por fase do `DEVELOPMENT_PLAN.md`.

---

## Fase 1 — Fundação de arquitetura (2026-07-07)

Objetivo: preparar DI (Hilt), navegação (Navigation Compose) e a estrutura de pacotes definida em `PROJECT_ARCHITECTURE.md`, sem ainda existir lógica de negócio.

**Ficheiros criados**
- `app/src/main/java/pt/controleobras/app/ControleObrasApplication.kt` — `Application` anotada `@HiltAndroidApp`
- `app/src/main/java/pt/controleobras/app/core/designsystem/theme/{Color,Theme,Type}.kt` — tema movido de `ui/theme`
- `app/src/main/java/pt/controleobras/app/core/navigation/ControleObrasDestination.kt` — rotas de navegação
- `app/src/main/java/pt/controleobras/app/core/navigation/ControleObrasNavHost.kt` — `NavHost` raiz
- `app/src/main/java/pt/controleobras/app/feature/home/ui/HomeScreen.kt` — ecrã inicial placeholder
- `PROJECT_ARCHITECTURE.md`, `DEVELOPMENT_PLAN.md` — documentação de arquitetura e roadmap
- `CHANGELOG.md` — este ficheiro

**Ficheiros alterados**
- `gradle/libs.versions.toml` — versões `ksp`, `hiltAndroid`, `hiltNavigationCompose`, `navigationCompose` adicionadas; `lifecycleRuntimeKtx` atualizada de `2.6.1` para `2.10.0` (estava desatualizada face ao resto do toolchain; passa também a ser usada por `lifecycle-viewmodel-compose`, que partilha a mesma versão por convenção do próprio Google)
- `build.gradle.kts` (raiz) — plugins `ksp` e `hilt.android` declarados (`apply false`)
- `app/build.gradle.kts` — plugins `ksp`/`hilt.android` aplicados; dependências `navigation-compose`, `lifecycle-viewmodel-compose`, `hilt-android`, `hilt-compiler` (via `ksp`), `hilt-navigation-compose` adicionadas
- `app/src/main/AndroidManifest.xml` — `android:name=".ControleObrasApplication"` registado na tag `<application>`
- `app/src/main/java/pt/controleobras/app/MainActivity.kt` — anotada `@AndroidEntryPoint`; passa a montar `ControleObrasNavHost()` em vez do `Greeting` de exemplo
- `app/src/main/res/values/strings.xml` — string `home_placeholder` adicionada

**Ficheiros removidos**
- `app/src/main/java/pt/controleobras/app/ui/theme/` (pasta completa) — substituída por `core/designsystem/theme/`

**Não foi possível compilar automaticamente neste ambiente** (sandbox Linux sem Android SDK e com JDK 11, enquanto o Gradle 9.4.1 do projeto exige JDK 17+). Todos os ficheiros foram revistos manualmente linha a linha para consistência de packages, imports e referências ao Version Catalog. **Ação necessária:** abrir o projeto no Android Studio e correr um Gradle Sync + Build para confirmar a compilação.

**Notas em aberto**
- Cores da marca ainda não personalizadas — tema mantém a paleta Material 3 gerada por omissão (ver `PROJECT_ARCHITECTURE.md`, secção 9)
- DI com Hilt confirmado como escolha (ver `PROJECT_ARCHITECTURE.md`, secção 2), mas nenhuma dependência é injetada ainda — só a infraestrutura está pronta

---

## Correções de compatibilidade AGP 9 (2026-07-07)

Três problemas de compatibilidade resolvidos sequencialmente durante a estabilização da Fase 1, todos por desalinhamento de versões de plugins/toolchain face ao AGP 9.2.1 (confirmados em documentação oficial, ver histórico da conversa):

- `gradle/libs.versions.toml`: `hiltAndroid` `2.57.1` → `2.59.2` (Hilt só suporta AGP 9 a partir da 2.59)
- `gradle/libs.versions.toml`: `ksp` `2.2.10-2.0.2` → `2.3.9` (KSP só suporta o Built-in Kotlin do AGP 9 a partir da 2.3.1)
- `gradle/libs.versions.toml`: `kotlin` `2.2.10` → `2.3.10` (alinhado com a dependência interna do Kotlin Gradle Plugin que o AGP 9.2.0 passou a exigir; arrasta consigo o plugin do Compose Compiler)
- `app/build.gradle.kts`: `compileSdk` `36.1` → `37.0` (exigido pelo metadata AAR de `androidx.core:core-ktx:1.19.0`, dependência já presente no projeto original)

## Fases 3–12 — Aplicação completa (2026-07-07)

Implementação completa do fluxo descrito no `CLAUDE_CONTEXT.md`: captura/seleção de imagem → OCR → parser → confirmação manual → persistência (Room + JSON + XML) → histórico. Feita numa única iteração a pedido explícito do utilizador, que foi avisado de que isto contraria a regra de fases pequenas e confirmadas uma a uma — decisão dele, registada aqui para memória futura.

**Novas dependências** (`gradle/libs.versions.toml` + `app/build.gradle.kts`)
- `androidx.room` `2.8.4` (`room-runtime`, `room-ktx`, `room-compiler` via KSP) — persistência local
- `androidx.camera` (CameraX) `1.6.1` (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) — captura de fotografia
- `com.google.mlkit:text-recognition` `16.0.1` (variante *bundled*, sem dependência de rede) — OCR
- `org.jetbrains.kotlinx:kotlinx-serialization-json` `1.11.0` + plugin `kotlin.plugin.serialization` — serialização JSON
- `ksp { arg("room.schemaLocation", ...) }` — exporta o schema do Room para a pasta `schemas/`, necessário para migrações futuras

**Ficheiros criados**
- `core/model/{ItemTalao,Talao,TalaoDraft,TalaoDraftMapper}.kt` — modelos de domínio e rascunho pré-confirmação
- `core/database/entity/{TalaoEntity,ItemTalaoDto}.kt`, `core/database/converter/Converters.kt`, `core/database/dao/TalaoDao.kt`, `core/database/AppDatabase.kt`, `core/database/mapper/TalaoMapper.kt` — Room (itens guardados como JSON via `TypeConverter`, não numa tabela normalizada — decisão consciente de simplicidade, documentada como possível evolução futura)
- `core/ocr/{TextRecognizer,MlKitTextRecognizer}.kt` — abstração sobre o ML Kit
- `core/parser/{ReceiptParser,HeuristicReceiptParser}.kt` — parser heurístico por regex (NIF, data, hora, total, IVA, nº fatura, produtos); **não garante 100% de acerto por design**, daí todos os campos ficarem editáveis na confirmação
- `core/export/{XmlExporter,TalaoJsonDto}.kt` — exportação XML (serializer manual, sem dependência nova) e JSON
- `data/repository/{TalaoRepository,TalaoRepositoryImpl}.kt` — persiste na Room e escreve JSON+XML ao lado da imagem, em `filesDir/receipts/`
- `di/{OcrModule,ParserModule,RepositoryModule}.kt` — bindings Hilt
- `feature/receiptflow/viewmodel/{ReceiptFlowUiState,ReceiptFlowViewModel}.kt` — orquestra OCR → parser → confirmação → guardar; partilhado entre os dois ecrãs seguintes via grafo de navegação aninhado
- `feature/receiptcapture/ui/CameraCaptureScreen.kt` — pré-visualização CameraX, captura, seleção da galeria (Photo Picker), permissão de câmara em runtime
- `feature/receiptreview/ui/ReceiptReviewScreen.kt` — formulário de confirmação/edição de todos os campos e produtos
- `feature/receiptlist/{ui/ReceiptListScreen,viewmodel/ReceiptListViewModel}.kt` — histórico de talões guardados
- `app/src/main/res/xml/file_paths.xml` — configuração do `FileProvider`

**Ficheiros alterados**
- `core/navigation/ControleObrasDestination.kt` — rotas `ReceiptFlowGraph`, `ReceiptCapture`, `ReceiptReview`, `ReceiptList`
- `core/navigation/ControleObrasNavHost.kt` — grafo aninhado `receiptFlow` com `ReceiptFlowViewModel` partilhado (`hiltViewModel(parentEntry)`)
- `feature/home/ui/HomeScreen.kt` — botões "Novo talão" e "Histórico" reais
- `app/src/main/res/values/strings.xml` — strings `home_novo_talao`, `home_historico`; `home_placeholder` reescrita
- `app/src/main/AndroidManifest.xml` — permissão `CAMERA`, `<uses-feature>` de câmara opcional, `<provider>` `FileProvider`

**Decisões técnicas a validar com o utilizador**
- Itens do talão guardados como JSON dentro da própria linha (`TypeConverter`), não numa tabela relacional própria — mais simples para a v1, migrável mais tarde sem quebrar a API pública do repositório
- Parser heurístico por regex — não usa nenhuma biblioteca de terceiros; qualidade de extração depende muito da qualidade do OCR e do formato do talão

**Não foi possível compilar neste ambiente** (mesma limitação da Fase 1: sem Android SDK, JDK 11 apenas). Revisão feita por leitura manual de todos os ficheiros e verificação cruzada de packages/imports/nomes de função. Dada a dimensão desta alteração, é expectável que surjam um ou mais erros de compilação no Android Studio — é necessário Gradle Sync + Build para confirmar.

**Atualização (2026-07-07):** `BUILD SUCCESSFUL` confirmado pelo utilizador. `app-debug.apk` gerado em `app/build/outputs/apk/debug/`, instalado com sucesso num tablet físico via `adb install`. Fase de estabilização encerrada — projeto compilável de novo. Próximo passo: teste funcional do fluxo completo no dispositivo (câmara → OCR → confirmação → guardar → histórico).

---

## Tema laranja, exportação CSV e ecrã de detalhe (2026-07-07)

Pedido do utilizador após o primeiro `BUILD SUCCESSFUL`: tema laranja, exportação visível (XML + novo CSV) e confirmação de que produtos guardam nome/quantidade/valor (já estava feito desde a Fase 8/9 — apenas confirmado, nada alterado aí).

**Ficheiros criados**
- `core/export/CsvExporter.kt` — CSV, uma linha por produto (`Empresa;NIF;Data;NumeroFatura;Produto;Quantidade;PrecoUnitario;TotalProduto;IVA;TotalTalao`), separador `;`
- `core/export/ExportFileLocator.kt` — convenção única de caminhos dos ficheiros de exportação (`filesDir/receipts/talao_<id>.{json,xml,csv}`), usada pelo repositório e pelos ecrãs
- `core/export/ExportSharing.kt` — `partilharExportacao()`, abre o menu de partilha do Android para o XML/CSV de um talão (via `FileProvider`)
- `feature/receiptdetail/viewmodel/ReceiptDetailViewModel.kt`, `feature/receiptdetail/ui/ReceiptDetailScreen.kt` — novo ecrã de detalhe (todos os campos do talão + botões "Exportar XML"/"Exportar CSV")

**Ficheiros alterados**
- `core/designsystem/theme/Color.kt` — paleta laranja de marca (`primary = #FF6D00`) substitui a paleta Purple gerada por omissão, com variantes claro/escuro
- `core/designsystem/theme/Theme.kt` — usa a nova paleta; `dynamicColor` removido (a app deixa de seguir o wallpaper do dispositivo — decisão de identidade de marca consistente)
- `data/repository/TalaoRepositoryImpl.kt` — passa também a escrever o CSV ao guardar; usa `ExportFileLocator` em vez de construir caminhos à mão
- `feature/receiptlist/ui/ReceiptListScreen.kt` — tocar num talão abre o ecrã de detalhe; ícone de partilha rápida (exporta XML) em cada cartão
- `core/navigation/ControleObrasDestination.kt` — nova rota `receiptDetail/{talaoId}`
- `core/navigation/ControleObrasNavHost.kt` — regista o ecrã de detalhe com argumento `talaoId` (`NavType.LongType`)

**Não foi possível compilar neste ambiente** (mesma limitação de sempre). Revisão manual feita, incluindo correção de um import cruzado entre features que tinha ficado mal colocado (`partilharExportacao` estava na feature `receiptdetail` e era importado por `receiptlist` — movido para `core/export` para não violar a separação por camadas). Precisa de Gradle Sync + Build + instalação para confirmar.

**Atualização:** utilizador reportou 4 erros de compilação reais neste build. Corrigidos: dependência `androidx.compose.material:material-icons-core` em falta (adicionada ao catalog + `app/build.gradle.kts`); `ControleObrasDestination.ReceiptDetail.ARG_TALAO_ID` estava mal referenciado (a constante é `ControleObrasDestination.ARG_TALAO_ID`, no companion object da classe-mãe); import supérfluo `androidx.compose.foundation.layout.weight` em `ReceiptListScreen.kt` que colidia com um símbolo interno do Compose com o mesmo nome — removido (`.weight()` resolve-se pelo `RowScope` implícito, sem import).

---

## Textos de apoio e boas-vindas na primeira abertura (2026-07-07)

Pedido do utilizador: instruções para o utilizador se orientar na app.

**Ficheiros criados**
- `core/common/AppPreferences.kt` — `SharedPreferences` simples (sem dependência nova) para guardar a flag "já viu as boas-vindas"
- `feature/home/viewmodel/HomeViewModel.kt` — expõe `mostrarBoasVindas: StateFlow<Boolean>`

**Ficheiros alterados**
- `feature/home/ui/HomeScreen.kt` — `AlertDialog` com o passo a passo da app (fotografar → OCR → confirmar → guardar/exportar), mostrado automaticamente só na primeira abertura de sempre; nova legenda de apoio no ecrã
- `feature/receiptcapture/ui/CameraCaptureScreen.kt` — legenda sobreposta no topo da pré-visualização da câmara com instrução de utilização
- `feature/receiptreview/ui/ReceiptReviewScreen.kt` — legenda no topo do formulário a explicar a confirmação
- `feature/receiptlist/ui/ReceiptListScreen.kt` — legenda no estado vazio e acima da lista
- `app/src/main/res/values/strings.xml` — novas strings (`home_instrucao`, `boasvindas_*`)

**Não foi possível compilar neste ambiente** (limitação de sempre). Precisa de Gradle Sync + Build + reinstalação para testar — a caixa de boas-vindas só aparece na primeira abertura de sempre da app; para a veres outra vez depois de já teres aberto a app, terias de desinstalar e reinstalar (limpa os dados/SharedPreferences).

---

## Parser mais robusto + texto OCR sempre preservado (2026-07-07)

Pedido do utilizador: leitura de dados inconsistente (campos diferentes falham de talão para talão), "não pode faltar nada". Sem exemplo real disponível para testar, a abordagem foi (1) tornar a extração heurística mais tolerante a variações de formato e (2) garantir que o texto bruto do OCR nunca se perde, mesmo quando o parser falha — fica sempre visível e guardado para o utilizador conferir/copiar à mão.

**`core/parser/HeuristicReceiptParser.kt` — reescrito**
- Extração de produtos deixou de exigir um formato de linha fixo e ancorado; agora procura todos os valores monetários em cada linha (`findAll`), o que cobre muito mais layouts de talão
- NIF validado pelo **algoritmo de checksum oficial português** (módulo 11) — antes aceitava a primeira sequência de 9 dígitos que encontrasse, que muitas vezes era o número errado (telefone, código de produto, etc.)
- Datas: aceita `/`, `-` e `.` como separador, mais formato ISO (`aaaa-mm-dd`) e anos com 2 dígitos
- Hora: aceita `:`, `.` e `h` como separador (comum em OCR e em notação informal)
- TOTAL/IVA/Nº fatura: mais variantes de rótulo aceites (`TOTAL A PAGAR`, `VALOR TOTAL`, `IVA 23%`, `FT`/`FS`/`FR`/`FA`/`GT`, símbolo `€` opcional)
- Linhas conhecidas de cabeçalho/rodapé (TOTAL, MULTIBANCO, OBRIGADO, ...) são ignoradas na extração de produtos, para não virarem "produtos" falsos

**Texto OCR preservado permanentemente**
- `core/model/Talao.kt` — novo campo `textoOcr: String?`
- `core/database/entity/TalaoEntity.kt` — nova coluna `textoOcr`
- `core/database/Migrations.kt` (novo) — `MIGRATION_1_2`, `ALTER TABLE talao ADD COLUMN textoOcr TEXT`, **não apaga dados existentes**
- `core/database/AppDatabase.kt` — versão 1 → 2
- `di/DatabaseModule.kt` — regista a migração no `Room.databaseBuilder`
- `core/database/mapper/TalaoMapper.kt`, `core/model/TalaoDraftMapper.kt`, `core/export/TalaoJsonDto.kt`, `core/export/XmlExporter.kt` — todos passam a incluir `textoOcr` (não incluído no CSV, que é estruturado por produto — o texto bruto não faz sentido repetido em cada linha)
- `feature/receiptreview/ui/ReceiptReviewScreen.kt` e `feature/receiptdetail/ui/ReceiptDetailScreen.kt` — botão "Ver texto reconhecido (OCR)" que mostra/esconde o texto bruto

**Não foi possível compilar neste ambiente.** Esta alteração muda o esquema da base de dados — é importante testar num tablet que já tenha talões guardados, para confirmar que a migração preserva os dados existentes (não é só instalar de raiz).

---

## Compose BOM desatualizado — causa raiz de dezenas de erros (2026-07-08)

Utilizador reportou "muitos erros" no build, todos em `ReceiptDetailScreen.kt`: `Cannot access class 'androidx.compose.runtime.internal.ComposableFunctionN'`, tipos de lambda incompatíveis, `weight` e campos de `item` "unresolved". São todos sintomas em cascata de **uma única causa**: o `composeBom` estava em `2026.02.01`, de quando o projeto começou (Kotlin 2.2.10). Entretanto o Kotlin/Compose Compiler subiu para `2.3.10` (correção de compatibilidade AGP 9), mas ninguém voltou a atualizar a BOM — o Compose Runtime resolvido por essa BOM antiga não tem as classes `ComposableFunctionN` que o compilador mais recente já gera para `@Composable` lambdas.

**Alterado**
- `gradle/libs.versions.toml`: `composeBom` `2026.02.01` → **`2026.06.01`** (confirmado oficialmente em developer.android.com — a mais recente estável; resolve Compose Runtime/UI 1.11.2, Material3 1.4.0)
- `feature/receiptdetail/ui/ReceiptDetailScreen.kt`: `Icons.Filled.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack` (o Android Studio assinalava a versão antiga como deprecated)

**Não foi possível compilar neste ambiente.** Esta é uma atualização de 4 releases de BOM de uma só vez — improvável, mas não impossível, que traga alguma mudança de API entretanto. Faz Sync + Build e envia-me o log se sobrar algum erro.

---

## Revisão OCR — leitura automática, imagem visível, avisos visuais (2026-07-08)

Pedido do utilizador: o utilizador **não deve escrever nada** — a app lê a fatura automaticamente; campos não encontrados mostram aviso visual; a imagem da fatura deve ser visível junto com os dados, tanto no ecrã de revisão como no ecrã de detalhe.

### Alterações

**`feature/receiptreview/ui/ReceiptReviewScreen.kt` — reescrito**
- Todos os `OutlinedTextField` (campos editáveis) removidos — substituídos por `Card` de leitura com rótulo + valor
- Campos sem valor (vazios/nulos) mostram um `Card` amarelo com ícone ⚠️ e texto **"Verifique na imagem esta informação"**
- A imagem da fatura capturada (path ou content URI) é exibida no topo do ecrã, acima dos dados, usando `AsyncImage` (Coil)
- Produtos igualmente em modo de leitura (`CardProduto`); se não houver produtos, aviso visual idêntico
- Botão "Guardar fatura" mantido

**`feature/receiptdetail/ui/ReceiptDetailScreen.kt` — atualizado**
- Imagem original da fatura adicionada no topo do ecrã de detalhe (mesma lógica de `AsyncImage`)

**`core/parser/HeuristicReceiptParser.kt` — melhorado**
- Empresa: passa a analisar múltiplas linhas do cabeçalho e a pontuar cada uma (proporção de maiúsculas × comprimento); escolhe a mais provável em vez de usar a primeira linha
- Morada: prioriza código postal português (`XXXX-XXX`) como âncora primária; palavras-chave de via pública como fallback
- Número de fatura: regex alargado (`NC`, `ND`, `RECIBO Nº`, `DOCUMENTO Nº`)
- Total: usa o **último** match (evita subtotais intermédios)
- IVA: regex separado do total, mais preciso
- `PALAVRAS_IGNORAR`: adicionadas `DESCONTO`, `TAXA`, `IMPOSTO`, `PAGAMENTO`, `NUMERADOR`, `DOCUMENTO`, `EMITIDO`

**`gradle/libs.versions.toml` + `app/build.gradle.kts` — nova dependência**
- `io.coil-kt:coil-compose:2.7.0` adicionada (necessária para `AsyncImage`)

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build e instala no tablet. O comportamento esperado: fotografas ou seleciona uma imagem → a app processa → abre o ecrã de revisão com a imagem no topo e os dados extraídos em baixo; campos não encontrados mostram o aviso amarelo.

---

## Leitura de QR code AT em faturas (2026-07-08)

Pedido do utilizador: a app deve também extrair dados do QR code presente nas faturas, exportando-os no XML e CSV (que já existiam).

### Contexto técnico

Desde julho de 2021, as faturas portuguesas são obrigadas a ter um QR code normalizado pela Autoridade Tributária (Portaria 195/2020). Esse QR contém campos separados por `*` no formato `CHAVE:VALOR` — entre eles NIF do emitente, data, número de documento, total de IVA e total a pagar. Estes dados são muito mais fiáveis do que a extração heurística por OCR porque foram inseridos pelo sistema de faturação do emitente. A app passa a usar ambas as fontes e a preferir o QR quando disponível.

### Arquitectura

O fluxo de processamento passa a ser:
```
Imagem → [OCR (texto)]  ─┐
         [QR (barcode)] ─┴→ merge → TalaoDraft → guardar/exportar
```
OCR e QR correm em paralelo (`async/await`). O merge aplica a regra: **QR tem prioridade** para os campos que fornece (NIF, data, nºfatura, IVA, total); OCR preenche os campos que o QR não tem (empresa, morada, hora, produtos, observações). Sem QR → comportamento idêntico ao anterior.

### Ficheiros criados

- `core/qr/QrCodeReader.kt` — interface (parallel ao `TextRecognizer`)
- `core/qr/MlKitQrCodeReader.kt` — implementação ML Kit Barcode Scanning (bundled, offline); procura o primeiro QR_CODE na imagem; devolve o `rawValue` ou null
- `core/qr/AtQrCodeParser.kt` — interpreta o formato AT: split por `*`, extrai campos `A` (NIF), `F` (data YYYYMMDD), `G` (nºfatura), `N` (totalIVA), `O` (totalComIVA); valida presença do campo obrigatório `A`; devolve `AtQrData` ou null

### Ficheiros alterados

- `di/OcrModule.kt` — adicionado binding `QrCodeReader → MlKitQrCodeReader`
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — injeta `QrCodeReader` e `AtQrCodeParser`; `processarImagem()` lança OCR e QR em paralelo; função `mergeDraft()` aplica a regra de prioridade
- `gradle/libs.versions.toml` — versão `mlkitBarcodeScanning = "17.3.0"` + biblioteca `mlkit-barcode-scanning`
- `app/build.gradle.kts` — `implementation(libs.mlkit.barcode.scanning)`

### O que não mudou

- Exportação XML e CSV: não foi necessário alterar nada — os dados já ficam no `Talao` antes de exportar; o QR apenas melhora a qualidade dos campos, não acrescenta campos novos
- `TalaoDraft` e `Talao`: modelos inalterados
- Ecrã de revisão: idêntico — continua a mostrar os dados extraídos (agora potencialmente mais completos quando há QR)

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build. Para testar: fotografa uma fatura portuguesa recente (que tenha o QR AT) — deves ver NIF, data, nºfatura, IVA e total preenchidos com os valores exatos do QR code.

---

## Coil removido — conflito de runtime Compose (2026-07-08)

Após o build com QR code, surgiram de novo os erros `ComposableFunctionN` — desta vez causados pelo `coil-compose:2.7.0`, compilado com Kotlin 1.9.x, que puxava transitivamente uma versão antiga do Compose Runtime incompatível com a BOM 2026.06.01.

**Solução:** remover Coil completamente e substituir `AsyncImage` por carregamento nativo sem dependências:
- `Image(bitmap = BitmapFactory.decodeFile(path).asImageBitmap(), ...)`
- Para URIs `content://`: `contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }`

**Ficheiros alterados**
- `gradle/libs.versions.toml` — linha `coil = "2.7.0"` e `coilCompose` removidas
- `app/build.gradle.kts` — `implementation(libs.coil.compose)` removido
- `feature/receiptreview/ui/ReceiptReviewScreen.kt` — `AsyncImage` substituído por `ImagemFatura()` com `BitmapFactory` nativo
- `feature/receiptdetail/ui/ReceiptDetailScreen.kt` — idem

---

## Formulário de funcionário + GPS + MAC + CSV de registo + Google Drive (2026-07-08)

Pedido do utilizador: ao clicar "Novo Talão" deve surgir um formulário obrigatório; ao tirar a foto, a app deve capturar GPS, MAC, gerar um ID de registo, tentar detetar QR code, gerar um CSV de registo específico e enviar a imagem + CSV para o Google Drive automaticamente.

### Fluxo novo

```
Home → [Formulário funcionário] → Câmara
  → GPS + MAC + IDREG + OCR + QR (paralelo)
  → imagem renomeada: {MAC}_{IDREG}.jpg
  → CSV gerado:       {MAC}_{IDREG}.csv
  → upload Drive (background, SAF — sem OAuth nem API keys)
  → ecrã de revisão com dados extraídos
```

### Ficheiros criados

- `core/model/WorkerFormData.kt` — `data class WorkerFormData(funcn, ccnome, funobs)`
- `core/model/CaptureMetadata.kt` — `data class CaptureMetadata(macAddress, idReg, gps, qrCodeRaw)` + propriedade `fileBaseName`
- `core/location/LocationProvider.kt` — interface
- `core/location/FusedLocationProvider.kt` — usa `FusedLocationProviderClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY)`; devolve `"lat,lon"` ou `""` sem permissão; usa `suspendCancellableCoroutine` (sem dependência extra)
- `core/device/DeviceInfo.kt` — `getMacAddress()` (tenta eth0/wlan0, filtra MACs aleatórios 02:00:00, fallback ANDROID_ID formatado como MAC); `gerarIdReg()` (AAAAMMDDHHMMSS)
- `core/export/CapturaCsvExporter.kt` — CSV de registo com colunas: `Macadress;IDREG;TIPO;GPS;FUNCN;FUNCDESC;FUNOBS;FORNECEDOR;DATA;QRCODE;TIPODOC;TOTAL`
- `core/drive/DriveUploader.kt` — interface
- `core/drive/SafDriveUploader.kt` — upload via SAF (`DocumentsContract.createDocument` + `ContentResolver.openOutputStream`); sem OAuth, sem API keys — usa o Drive app já instalado no tablet
- `di/LocationModule.kt` — binding `FusedLocationProvider → LocationProvider`
- `di/DriveModule.kt` — binding `SafDriveUploader → DriveUploader`
- `feature/workerform/ui/WorkerFormScreen.kt` — 3 campos obrigatórios (FUNCN, Centro de Custo, Observações); validação com `tentouSubmeter`; botão "Continuar para fotografar"

### Ficheiros alterados

- `core/common/AppPreferences.kt` — novo campo `driveFolderUri: String?`
- `core/navigation/ControleObrasDestination.kt` — nova rota `WorkerForm`
- `core/navigation/ControleObrasNavHost.kt` — grafo aninhado inicia em `WorkerForm` (era `ReceiptCapture`); os três ecrãs partilham o mesmo `ReceiptFlowViewModel` via `hiltViewModel(parentEntry)`; `Home.onNovoTalao` navega para `ReceiptFlowGraph.route`
- `feature/receiptflow/viewmodel/ReceiptFlowUiState.kt` — novos campos: `workerFormData`, `captureMetadata`, `qrDetectado`, `driveStatus`; novo enum `DriveStatus { IDLE, A_ENVIAR, ENVIADO, ERRO }`
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — reescrito: injeta `LocationProvider`, `DeviceInfo`, `CapturaCsvExporter`, `DriveUploader`; `processarImagem()` corre OCR + QR + GPS em paralelo; renomeia imagem; gera CSV; lança upload Drive em background
- `di/OcrModule.kt` — binding `QrCodeReader → MlKitQrCodeReader` (adicionado neste sprint junto com o QR)
- `gradle/libs.versions.toml` — `mlkitBarcodeScanning = "17.3.0"`, `playServicesLocation = "21.3.0"`
- `app/build.gradle.kts` — `implementation(libs.mlkit.barcode.scanning)`, `implementation(libs.play.services.location)`
- `app/src/main/AndroidManifest.xml` — permissões `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; feature GPS (required=false)

### Ecrã Home — configuração Google Drive

- `feature/home/viewmodel/HomeViewModel.kt` — novo `driveConfigurado: StateFlow<Boolean>` + `guardarDriveFolderUri()`
- `feature/home/ui/HomeScreen.kt` — banner colorido (verde/amarelo) com estado do Drive; botão "Configurar"/"Alterar" que abre o picker SAF (`ActivityResultContracts.OpenDocumentTree()`); ao selecionar: `takePersistableUriPermission()` + guarda em `AppPreferences`

### Ecrã de Revisão — estado Drive + aviso sem QR

- `feature/receiptreview/ui/ReceiptReviewScreen.kt` — `DriveStatusIndicator` (barra de progresso quando `A_ENVIAR`, cartão verde quando `ENVIADO`, cartão vermelho quando `ERRO`); `SnackbarHost` + `LaunchedEffect(draft)` para mostrar "Sem QR code detetado" quando `qrDetectado == false`

### Como configurar o Google Drive no tablet

1. Abrir a app → ecrã Home mostra "Sem pasta configurada"
2. Tocar em **Configurar** → picker do sistema Android → navegar até ao Google Drive → selecionar a pasta destino → confirmar
3. A app guarda a URI com permissão persistente — não precisas de repetir este passo entre sessões
4. Da próxima captura em diante, a imagem e o CSV são enviados automaticamente logo após a fotografia

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build + instalação. Se a pasta Drive ainda não estiver configurada, o upload é ignorado silenciosamente (os ficheiros ficam guardados localmente em `filesDir/receipts/`).

---

## GPS no CSV + HeuristicReceiptParser super-inteligente (2026-07-08)

Dois problemas reportados pelo utilizador após confirmação de que o Drive estava a funcionar:

1. **GPS aparecia sempre vazio no CSV** — permissão declarada no manifesto mas nunca pedida em runtime (Android 6+). `FusedLocationProvider` devolve `""` sem permissão concedida.
2. **Campos do parser extraídos de forma inconsistente** — pedido explícito de "algoritmo super-inteligente para interpretar toda e qualquer fatura no mercado".

### Fix GPS — permissão em runtime

**Ficheiro alterado:** `feature/workerform/ui/WorkerFormScreen.kt`

Adicionado `rememberLauncherForActivityResult(RequestMultiplePermissions)` com pedido de `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` num `LaunchedEffect(Unit)`, assim que o ecrã abre. Estratégia intencional: o GPS tem tempo de aquecer enquanto o funcionário preenche o formulário (FUNCN, Centro de Custo, Observações), antes de tirar a fotografia.

### HeuristicReceiptParser — reescrita completa

**Ficheiro alterado:** `core/parser/HeuristicReceiptParser.kt`

Reescrito de raiz com uma estratégia em **duas camadas para cada campo**:

**Camada 1 — Label-first:** procura o rótulo explícito na mesma linha ("NIF:", "DATA:", "TOTAL:", etc.) e extrai o valor imediatamente após. Muito fiável quando o documento está bem formatado.

**Camada 2 — Pattern fallback:** expressão regular sobre todo o texto, para quando o rótulo não existe ou o OCR o deformou.

#### Melhorias por campo

**Empresa**
- Prioridade 1: linha com sufixo legal (Lda., S.A., Unipessoal, SGPS, EIRELI, SRL…)
- Prioridade 2: rótulo explícito "EMPRESA:", "RAZÃO SOCIAL:", "NOME COMERCIAL:"
- Prioridade 3: scoring por proporção de maiúsculas × comprimento, exclui moradas e linhas de ruído

**NIF**
- Rótulo alargado: `NIF`, `NIPC`, `Contribuinte`, `N.I.F.`, `N.I.P.C.`
- Fallback: primeiro número de 9 dígitos com checksum módulo-11 válido (sem rótulo)

**Morada**
- Combinação de via pública (Rua, Av., Largo, Travessa, Praceta, Estrada, Urbanização, Bairro, Quinta, Alameda, Parque, Bloco, Lote, Apt.) + código postal (XXXX-XXX) numa morada completa

**Data**
- Rótulo: DATA, DATE, DT, EMISSÃO, EMITIDO EM, PROCESSADO EM
- Formato texto: "15 de Janeiro de 2025" (meses PT por extenso)
- Formatos numéricos: dd/mm/aaaa, dd-mm-aaaa, dd.mm.aaaa, aaaa-mm-dd, ano com 2 dígitos

**Hora**
- Rótulo: HORA, HR, HOUR, TIME, H.
- Separadores: `:`, `.`, `h`; com ou sem segundos

**Número de fatura**
- Tipo AT: FT, FS, FR, FA, FD, GT, NC, ND, TD, DC, RP, RE, CS, LD, RA + série/número
- Rótulo genérico: FATURA Nº, RECIBO Nº, DOCUMENTO Nº, Nº:
- Fallback: padrão SÉRIE/NÚMERO (ex: A/0001, 2024/0001)

**Total**
- Prioridade 1: "TOTAL A PAGAR", "MONTANTE A PAGAR", "VALOR A PAGAR", "A PAGAR"
- Prioridade 2: "TOTAL C/IVA", "TOTAL INCL. IVA", "TOTAL COM IVA", "TOTAL GERAL"
- Prioridade 3: linha com rótulo TOTAL (último match — evita subtotais)
- Prioridade 4: "SOMA", "MONTANTE", "VALOR TOTAL"

**IVA**
- Prioridade 1: "TOTAL IVA", "IVA TOTAL", "IVA A PAGAR"
- Prioridade 2: linhas de IVA por taxa (6%, 13%, 23%) somadas automaticamente
- Prioridade 3: padrão genérico IVA + valor

**Produtos**
- Delimitação da zona de produtos: entre linha com ARTIGO/PRODUTO/DESCRIÇÃO/ITEM/CÓDIGO e linha com SUBTOTAL/TOTAL/DESCONTO/IVA X/TROCO/PAGO
- Quantidade extraída do prefixo da linha (3x, 3×, 3 un., 3,000 kg, 3 lt)
- Lista de ruído muito alargada: TOTAL, SUBTOTAL, IVA, TROCO, PAGO, CONTRIBUINTE, NIF, NIPC, OBRIGADO, CAIXA, OPERADOR, MULTIBANCO, MB WAY, VISA, MASTERCARD, ATM, DESCONTO, TAXA, IMPOSTO, PAGAMENTO, NUMERADOR, TEL, TELEFONE, FAX, WWW, HTTP, EMAIL, @, CERTIFICADO, PROCESSADO, SISTEMA, SOFTWARE, PROGRAMA, TALÃO, DUPLICADO, ORIGINAL, CÓPIA, VIA DO CLIENTE, VIA DO COMERCIANTE, DOCUMENTO PROCESSADO, VALIDADE, AUTORIZA

**Observações**
- Rótulo: OBS, OBSERVAÇÕES, NOTAS, NOTA: — extrai o texto que se segue

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build + instala no tablet. O parser melhorado deve cobrir muito mais formatos de fatura (talões de supermercado, restaurante, posto de combustível, faturas de obra, faturas de material, recibos de serviços).
