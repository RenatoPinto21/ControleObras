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

---

## LLM local offline — Gemma 3 1B via MediaPipe (2026-07-09)

Pedido do utilizador: usar um modelo de linguagem local (offline) para extrair dados das faturas com muito maior precisão do que os regex.

### Modelo escolhido: Gemma 3 1B IT GPU INT4

| Critério | Valor |
|---|---|
| Tamanho do ficheiro | ~1.1 GB |
| RAM necessária | ~1.5 GB |
| Inferência num tablet médio | 10–20 segundos |
| Modo de execução | GPU (Adreno/Mali via OpenGL ES / Vulkan) |
| Conectividade | Totalmente offline após instalação |
| Biblioteca | `com.google.mediapipe:tasks-genai:0.10.22` |

### Hierarquia de extração (da mais fiável para a menos)

1. **QR code AT** (dados inseridos pelo emitente — infalível)
2. **LLM Gemma 3 1B** (compreensão semântica do texto OCR — novo)
3. **HeuristicReceiptParser** (regex — fallback sempre disponível)

O LLM só é usado se o modelo estiver instalado no dispositivo. Se não estiver, o comportamento é idêntico ao anterior (heurístico + QR).

### Instalação do modelo (admin)

O ficheiro do modelo deve ser colocado em:
```
Android/data/pt.controleobras.app/files/llm/gemma-3-1b-it-gpu-int4.bin
```
Acessível via gestor de ficheiros Android sem root, ou via ADB:
```
adb push gemma-3-1b-it-gpu-int4.bin /sdcard/Android/data/pt.controleobras.app/files/llm/
```
Download: https://huggingface.co/google/gemma-3-1b-it-litert-lc-preview

### Ficheiros criados

- `core/llm/LlmExtractionResult.kt` — `@Serializable` com campos nullable: empresa, nif, morada, data, hora, numeroFatura, total, iva, itens[], observacoes
- `core/llm/LlmExtractor.kt` — interface: `isModelReady(): Boolean` + `suspend extract(text): LlmExtractionResult?`
- `core/llm/LlmModelManager.kt` — `@Singleton`; gere o caminho `externalFilesDir/llm/model.bin`; `modelExists()` valida tamanho mínimo (500 MB)
- `core/llm/MediaPipeLlmExtractor.kt` — `@Singleton`; init lazy com `Mutex`; temperatura 0.0 (determinista); prompt Gemma instruct (`<start_of_turn>user/model`); extrai JSON da resposta (suporta blocos Markdown e JSON puro); fallback se init falhar
- `di/LlmModule.kt` — `@Binds LlmExtractor → MediaPipeLlmExtractor`

### Ficheiros alterados

- `gradle/libs.versions.toml` — `mediapipeTasks = "0.10.22"` + `mediapipe-tasks-genai`
- `app/build.gradle.kts` — `implementation(libs.mediapipe.tasks.genai)`
- `feature/receiptflow/viewmodel/ReceiptFlowUiState.kt` — novo campo `statusProcessamento: String`
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — injeta `LlmExtractor`; em `processarImagem()`: tenta LLM → se falhar usa heurístico; atualiza `statusProcessamento` ("A ler imagem (OCR)...", "A analisar com IA (Gemma)..."); `llmResultToDraft()` converte `LlmExtractionResult` para `TalaoDraft`
- `feature/home/viewmodel/HomeViewModel.kt` — injeta `LlmModelManager`; expõe `modeloIaDisponivel: StateFlow<Boolean>` e `verificarModeloIa()` (chamado em `onResume`)
- `feature/home/ui/HomeScreen.kt` — `DriveStatusBanner` generalizado para `StatusBanner` reutilizável; adicionado banner "Inteligência Artificial" (verde/amarelo); `DisposableEffect` + `LifecycleEventObserver` para re-verificar modelo ao voltar ao ecrã

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build + instalação. Sem o modelo instalado, a app funciona exatamente como antes (heurístico + QR). Com o modelo instalado, os dados das faturas deverão ser extraídos com muito maior precisão, especialmente em formatos não-standard.

---

## Download automático do modelo IA (2026-07-09)

Pedido do utilizador: o modelo deve ser descarregado automaticamente dentro da app, sem passos manuais. O link HuggingFace anterior não funcionava.

### Solução

Substituído **Gemma 3 1B** (HuggingFace, requer login) por **Gemma 2B IT INT4** (Google Storage público, sem login, sem conta):

```
URL: https://storage.googleapis.com/mediapipe-models/llm_inference/
     gemma-2-2b-it-gpu-int4/float32/1/gemma-2-2b-it-gpu-int4.bin
Tamanho: ~1.3 GB
Login: não requerido
```

### Como funciona na app

1. Ao abrir a app, a HomeScreen mostra o banner IA **amarelo** com botão **"Descarregar"**
2. Ao tocar, o DownloadManager do Android inicia o download em background
3. A barra de progresso com percentagem e tamanho aparece no banner
4. O download continua mesmo com a app fechada (notificação no sistema)
5. Quando concluído, o banner fica **verde** automaticamente e a IA está ativa

### Ficheiros criados

- `core/llm/LlmDownloadProgress.kt` — estado do download (`IDLE`, `A_DESCARREGAR`, `CONCLUIDO`, `ERRO`) + `descricaoTamanho` formatada
- `core/llm/LlmModelDownloader.kt` — wrapper `@Singleton` sobre `DownloadManager`; `iniciarDownload()`, `queryProgress(id)`, `cancelar(id)`

### Ficheiros alterados

- `core/llm/LlmModelManager.kt` — modelo atualizado para Gemma 2B; `MODEL_URL` público; `MODEL_FILENAME = "gemma-2-2b-it-gpu-int4.bin"`
- `core/common/AppPreferences.kt` — novo campo `llmDownloadId: Long` (persiste entre sessões para retomar monitorização após reinício da app)
- `feature/home/viewmodel/HomeViewModel.kt` — injeta `LlmModelDownloader`; `iniciarDownloadModelo()`, `cancelarDownload()`; polling a cada 500ms via coroutine; retoma polling no `init` se havia download em curso
- `feature/home/ui/HomeScreen.kt` — `IaBanner()` separado do `StatusBanner`; mostra barra de progresso determinada (`LinearProgressIndicator` com `progress = percentagem/100f`) ou indeterminada; botão "Descarregar" / "Cancelar" / "Tentar de novo" conforme estado

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build + instalação. Na primeira abertura, o banner IA aparece amarelo — toca em "Descarregar" para iniciar o download automático (~1.3 GB, necessita WiFi ou dados).

---

## Prompt IA melhorado + novos campos de fatura (2026-07-09)

Pedido do utilizador: separar NIF fornecedor/cliente, extrair série do documento, data de vencimento, método de pagamento, desconto e taxa de IVA por linha de produto.

### Contexto

A estrutura de dados foi alargada em 3 camadas simultâneas: (1) modelos de domínio, (2) base de dados, (3) extração por LLM e heurístico. O XML não foi alterado (pedido explícito do utilizador).

### Novos campos

| Campo | Onde aparece | Observações |
|---|---|---|
| `nifCliente` | TalaoDraft, Talao, TalaoEntity | NIF da empresa de construção (cliente) |
| `serie` | TalaoDraft, Talao, TalaoEntity | Série do doc. AT (ex: "A" de "FT A/1234") |
| `dataVencimento` | TalaoDraft, Talao, TalaoEntity | Data limite de pagamento |
| `metodoPagamento` | TalaoDraft, Talao, TalaoEntity | Ex: "Multibanco", "MB Way", "Numerário" |
| `desconto` (por item) | ItemTalaoDraft, ItemTalao, ItemTalaoDto | Desconto na linha de produto |
| `taxaIva` (por item) | ItemTalaoDraft, ItemTalao, ItemTalaoDto | Taxa IVA em % (6, 13 ou 23) |

### Ficheiros alterados

**Modelos de domínio**
- `core/model/TalaoDraft.kt` — 4 novos campos: `nifCliente`, `serie`, `dataVencimento`, `metodoPagamento`
- `core/model/Talao.kt` — idem
- `core/model/ItemTalaoDraft.kt` — 2 novos campos: `desconto`, `taxaIva`
- `core/model/ItemTalao.kt` — idem (nullable com default null)
- `core/model/TalaoDraftMapper.kt` — mapeia todos os novos campos

**Base de dados**
- `core/database/entity/TalaoEntity.kt` — 4 novas colunas nullable
- `core/database/entity/ItemTalaoDto.kt` — `desconto = ""`, `taxaIva = ""` (defaults garantem compatibilidade com JSON antigo)
- `core/database/mapper/TalaoMapper.kt` — mapeia todos os novos campos
- `core/database/Migrations.kt` — `MIGRATION_2_3`: `ALTER TABLE talao ADD COLUMN` para 4 colunas (só SQL, sem apagar dados)
- `core/database/AppDatabase.kt` — versão 2 → 3
- `di/DatabaseModule.kt` — `MIGRATION_2_3` registada no builder

**LLM**
- `core/llm/LlmExtractionResult.kt` — novo schema com snake_case e campos separados: `fornecedor`, `nif_fornecedor`, `nif_cliente`, `serie`, `data_emissao`, `data_vencimento`, `metodo_pagamento`, `subtotal`, `iva_total`, `total`, `linhas[]` com `desconto`/`taxa_iva`/`total_linha`; usa `@SerialName` para mapeamento JSON ↔ Kotlin
- `core/llm/MediaPipeLlmExtractor.kt` — prompt reescrito: separa fornecedor/cliente, explica extração de série, inclui "Nunca inventes valores", instrução QR AT, limita OCR a 2500 chars

**ViewModel**
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — `llmResultToDraft()` mapeado para todos os novos campos; `LlmItemResult.toItemTalaoDraft()` inclui `desconto`, `taxaIva`, `totalLinha`; chamada a `exportar()` passa `draft` em vez de apenas `fornecedor`

**Exportação CSV**
- `core/export/CapturaCsvExporter.kt` — assinatura atualizada para receber `TalaoDraft`; 5 novas colunas: `NIF_FORNECEDOR`, `NIF_CLIENTE`, `SERIE`, `DATA_VENCIMENTO`, `METODO_PAGAMENTO`

**Parser heurístico**
- `core/parser/HeuristicReceiptParser.kt` — 4 novos métodos de extração:
  - `extrairNifCliente()` — procura rótulo "NIF DO CLIENTE" / "ADQUIRENTE" / "A/C NIF"; fallback: segundo NIF válido diferente do fornecedor
  - `extrairSerie()` — extrai da parte entre tipo de doc e `/` (ex: "FT **A**/1234"); fallback: rótulo "SÉRIE:"
  - `extrairDataVencimento()` — rótulos: VENCIMENTO, PRAZO DE PAGAMENTO, DATA LIMITE, DATA VENC.
  - `extrairMetodoPagamento()` — rótulo "FORMA DE PAGAMENTO:"; fallback: palavras-chave MB WAY, MULTIBANCO, TRANSFERÊNCIA, VISA, MASTERCARD, NUMERÁRIO, CHEQUE no texto

**Não foi possível compilar neste ambiente.** Faz Gradle Sync + Build + reinstalação. A migração 2→3 corre automaticamente na primeira abertura — não apaga dados existentes. O CSV de captura agora inclui NIF do fornecedor, NIF do cliente, série, data de vencimento e método de pagamento.

---

## Crash OCR + imagem imediata (2026-07-09)

### Crash ao fotografar — maxTokens insuficiente

O app encerrava abruptamente (JNI SIGABRT) ao tirar foto. Causa raiz: `MediaPipeLlmExtractor` usava `setMaxTokens(1024)` mas o prompt enviado ao Gemma tinha 1165 tokens. O parâmetro `maxTokens` é o budget total (input + output), não só output — excedê-lo causa um crash nativo não capturável em Kotlin.

**Ficheiro alterado:** `core/llm/MediaPipeLlmExtractor.kt`
- `setMaxTokens(1024)` → `setMaxTokens(2048)` (Gemma suporta até 8192)
- `ocrText.take(2500)` → `ocrText.take(1500)` (reduz texto OCR para ≈375 tokens, prompt base ≈700 tokens)

### Imagem visível imediatamente (sem esperar OCR)

O app demorava a mostrar qualquer coisa após a fotografia. O utilizador via um ecrã em branco durante o processamento (20–60s). Solução: navegar para o ecrã de revisão logo que a imagem é capturada; mostrar a imagem imediatamente; spinner enquanto o OCR/LLM corre em background; campos preenchidos quando o processamento termina.

**`feature/receiptflow/viewmodel/ReceiptFlowUiState.kt`** — novo campo `imagemCapturadaPath: String?`

**`feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt`** — `processarImagem()` define `imagemCapturadaPath = imagemPath` **antes** de qualquer processamento

**`feature/receiptcapture/ui/CameraCaptureScreen.kt`** — navegação disparada por `imagemCapturadaPath != null` (era `draft != null`)

**`feature/receiptreview/ui/ReceiptReviewScreen.kt`** — mostra imagem sempre; spinner quando `draft == null`; campos quando `draft != null`

---

## Nova arquitetura de extração — extrator posicional + validação (2026-07-09)

### Contexto

O pipeline OCR → LLM (Gemma 2B) apresentava dois problemas críticos em uso real: (1) velocidade — 30 a 60 segundos por fatura é inaceitável em uso diário intenso; (2) fiabilidade — o modelo gerava campos inventados ou falhava a interpretar formatos de fatura não-standard.

Foi feita análise das abordagens de concorrentes (Contabify, Parseur, Klippa) e decidiu-se implementar uma solução própria com 4 camadas, 100% offline, com velocidade alvo de 1–2 segundos:

```
QR code AT (Portaria 195/2020)          ← mais fiável (dados do emitente)
  ↓ (quando sem QR ou campos em falta)
PositionAwareReceiptExtractor            ← usa layout visual (X, Y) da fatura
  ↓ (fallback)
HeuristicReceiptParser                   ← regex sobre texto concatenado
  ↓ (sempre, sobre o resultado final)
InvoiceFieldValidator                    ← valida NIF, datas, IVA, cruzamentos
```

### Ficheiros criados

**`core/ocr/StructuredOcrResult.kt`**
- `StructuredOcrResult(elements, fullText, imageWidth, imageHeight)`
- `OcrElement(text, left, top, right, bottom, confidence)` — coordenadas normalizadas (0.0–1.0)
- Propriedades calculadas: `centerX`, `centerY`, `height`, `width`

**`core/validation/FieldState.kt`**
- `enum class FieldState { VALID, SUSPECT, MISSING }`
- `data class FieldValidation(state, hint)` com `companion` para `valid()`, `suspect(hint)`, `missing(hint?)`

**`core/validation/NifValidator.kt`** — `@Singleton`
- `isValid(nif)` — 4 regras: 9 dígitos, sem letras, primeiro ∈ {1,2,3,5,6,7,8,9}, checksum módulo 11
- `motivoInvalido(nif)` — mensagem específica por tipo de falha (comprimento, letra, primeiro dígito, checksum)

**`core/validation/InvoiceFieldValidator.kt`** — `@Singleton`
- Valida todos os campos de um `TalaoDraft`, devolve `Map<String, FieldValidation>`
- Cross-validações: IVA < Total; total da linha ≈ qtd × preço (±0.02€ por arredondamento)
- Taxa IVA: só 6%, 13%, 23% são legais em Portugal
- Data: não anterior a 2000, não no futuro (+3 dias de tolerância para datas de emissão tarde)

**`core/extractor/PositionAwareReceiptExtractor.kt`** — `@Singleton`
- Divide a imagem em 3 zonas por Y normalizado: cabeçalho (0.00–0.42), corpo (0.33–0.78), rodapé (0.63–1.00)
- Agrupa elementos em linhas por proximidade Y (`|centerY_a - centerY_b| < 0.018`)
- Extrai empresa pela linha com maior fonte (height) ou sufixo legal (Lda., S.A., etc.)
- Extrai NIF fornecedor e NIF cliente por rótulo explícito e por checksum
- Extrai produtos pela posição X: descrição (X < 0.52), qtd (0.52–0.66), preço (0.66–0.82), total (X > 0.82)
- Extrai total, IVA, método de pagamento do rodapé

### Ficheiros alterados

**`core/ocr/TextRecognizer.kt`** — interface alargada:
- `recognizeText()` mantido (compatibilidade retroativa)
- `recognizeStructured()` adicionado — devolve `StructuredOcrResult`

**`core/ocr/MlKitTextRecognizer.kt`** — implementa `recognizeStructured()`:
- Itera `TextBlock → TextLine → TextElement`, cada um com `boundingBox: Rect?`
- Normaliza coordenadas: `left / imageWidth`, `top / imageHeight`, etc.
- `element.confidence` não existe em ML Kit 16.x — usa `1f` como default (sem erro)

**`core/qr/AtQrCodeParser.kt`** — campo B adicionado:
- Campo AT `B` = NIF do adquirente (comprador / vosso NIF) → `nifCliente`
- `AtQrData` tem novo campo `nifCliente: String?`
- NIF "999999990" (consumidor final) é filtrado no merge (não sobrepõe NIF do cliente real)

**`feature/receiptflow/viewmodel/ReceiptFlowUiState.kt`** — novo campo:
- `validacoes: Map<String, FieldValidation> = emptyMap()` — resultado da validação por campo

**`feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt`** — pipeline reescrito:
- Injeta `PositionAwareReceiptExtractor` e `InvoiceFieldValidator`
- `processarImagem()` usa `recognizeStructured()` em vez de `recognizeText()`
- Pipeline: OCR estruturado + QR + GPS paralelo → extrator posicional → merge QR → validação → estado
- LLM Gemma removido do pipeline primário (mantido injetado para uso futuro)
- `mergeDraft()` inclui `nifCliente` do QR (campo B), filtrando "999999990"

**`feature/receiptreview/ui/ReceiptReviewScreen.kt`** — UI com 3 estados visuais:
- `CampoValidado(rotulo, valor, validacao)` substitui `CampoLeitura()` e `AvisoCampoNaoEncontrado()`
  - VALID → fundo verde claro `#E8F5E9` + ícone CheckCircle `#2E7D32`
  - SUSPECT → fundo amarelo `#FFF8E1` + ícone Warning `#F57F17` + hint específico
  - MISSING → fundo cinzento `#ECEFF1` + ícone Help `#78909C` + mensagem
- `LegendaEstados()` explica o código de cores + indica se QR AT foi detetado
- `CardProduto()` recebe validações de total de linha e taxa IVA — fica amarelo se SUSPECT
- Secções separadas: Fornecedor / Vosso NIF / Documento / Valores / Produtos
- Campo "NIF do cliente (vosso)" visível no ecrã de revisão

**Não foi possível compilar neste ambiente.** Para build e instalação, usar os comandos descritos abaixo.

### Comandos de build (copiar e colar)

```
cd C:\Users\Estagio\Projetos\ControleObras
gradlew.bat assembleDebug
```

Depois de compilar com sucesso:
```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### O que testar

1. Tirar foto de uma fatura com QR AT → revisão abre **imediatamente** com a imagem → em 1–3s os campos aparecem
2. Verificar que o **NIF do cliente (vosso)** aparece preenchido quando o QR contém o campo B
3. Verificar os **3 estados visuais** nos campos: verde = encontrado e válido; amarelo = suspeito (com mensagem explicativa); cinzento = não encontrado
4. Testar com uma fatura **sem QR** → snackbar "Sem QR code AT" → campos extraídos por OCR posicional
5. Verificar que o botão "Guardar fatura" funciona e os dados ficam no histórico

---

## Morada robusta + filtro sanitizar + botão re-escanear QR + diálogo pré-guardar (2026-07-09)

Quatro melhorias pedidas após testes com talões reais (ALDI e outros supermercados):

### 1. Morada com lixo de OCR — fix

**Problema:** em talões de supermercado, a morada mostrava centenas de carateres de texto OCR misturado porque `VIA_REGEX` encontrava "Rua" dentro de uma linha longa com todo o talão concatenado.

**Fix em `core/extractor/PositionAwareReceiptExtractor.kt`:**
- `extrairMorada()` agora exige `linha.length <= 90` **e** `!EH_RUIDO_MORADA_REGEX.containsMatchIn(linha)` em cada linha candidata
- `EH_RUIDO_MORADA_REGEX` adicionado ao `companion object` — filtra palavras de pagamento/produto que nunca fazem parte de uma morada

### 2. Filtro pós-extração `sanitizar()`

Adicionado ao final de `extract()` em `PositionAwareReceiptExtractor.kt`. Anula campos que claramente contêm lixo:
- `empresa` — rejeitado se length > 60, proporção letras < 35%, ou contém ruído de rodapé
- `morada` — rejeitado se length > 100 ou contém palavras de ruído
- `nif` / `nifCliente` — apenas aceite se passa o checksum módulo 11
- `iva` / `total` — apenas aceite se o formato é exatamente `\d+[.,]\d{2}` (novo `VALOR_EXATO_REGEX`)

### 3. Botão "Tentar ler QR code AT"

**Problema:** quando o QR não é detetado automaticamente, o utilizador não tinha como tentar de novo sem tirar nova fotografia.

**Ficheiro alterado: `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt`**
- Novo método `reescanearQr(context)` — reutiliza a imagem já guardada (`imagemCapturadaPath`), corre só o QR reader, faz merge com o draft atual, re-valida todos os campos

**Ficheiro alterado: `feature/receiptreview/ui/ReceiptReviewScreen.kt`**
- `BotaoReescanearQr(isLoading, onClick)` — aparece apenas quando `!uiState.qrDetectado && !uiState.isProcessing`
- Spinner e texto "A ler QR code..." durante o processo

### 4. Diálogo de resumo antes de guardar

**Pedido:** nenhum campo é obrigatório, mas antes de guardar deve aparecer uma mensagem clara a listar o que está em falta.

**Ficheiro alterado: `feature/receiptreview/ui/ReceiptReviewScreen.kt`**
- `DialogoResumoProblemas(validacoes, onGuardar, onCancelar)` — `AlertDialog` que lista:
  - "Não encontrado:" — ícone Info + nome do campo, para cada MISSING
  - "Verificar na fatura:" — ícone Warning + nome + hint, para cada SUSPECT (exceto itens individuais)
  - Nota: "Pode guardar assim mesmo — pode sempre corrigir mais tarde no histórico"
  - Botões: "Guardar mesmo assim" (primary) e "Voltar a verificar" (text)
- Botão "Guardar fatura" verifica `validacoes.values.any { it.state != VALID }` — se houver problemas e `validacoes` não estiver vazio, abre o diálogo; caso contrário guarda diretamente

### Ficheiros alterados neste sprint

| Ficheiro | Alteração |
|---|---|
| `core/extractor/PositionAwareReceiptExtractor.kt` | `sanitizar()`, `EH_RUIDO_MORADA_REGEX`, `VALOR_EXATO_REGEX`, `extrairMorada()` com length/noise filter |
| `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` | `reescanearQr()` |
| `feature/receiptreview/ui/ReceiptReviewScreen.kt` | `BotaoReescanearQr()`, `DialogoResumoProblemas()`, lógica botão guardar |

**Não foi possível compilar neste ambiente.** Comandos de build:

```
cd C:\Users\Estagio\Projetos\ControleObras
gradlew.bat assembleDebug
"C:\Users\Estagio\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
```

### O que testar

1. Talão com morada — confirmar que o campo mostra só a rua/CP, não linhas longas com lixo
2. Talão ALDI ou supermercado — campos "lixo" devem aparecer cinzentos (MISSING) em vez de texto incorreto
3. Fatura sem QR → botão "Tentar ler QR code AT" visível → ao tocar aparece spinner → se encontrar QR, campos ficam verdes; se não encontrar, aparece snackbar de erro
4. Tocar "Guardar fatura" quando há campos em falta → diálogo lista os problemas → "Guardar mesmo assim" guarda; "Voltar a verificar" fecha o diálogo

---

## Validador com aviso QR + redesenho layout profissional (2026-07-09)

Dois grupos de alterações pedidos pelo utilizador neste sprint:

### Grupo 1 — Aviso "informação pouco segura — falta de QR code"

**Problema:** quando o QR AT não era detetado, os campos críticos apareciam a verde (VALID) mesmo vindo apenas do OCR, que tem muito menor fiabilidade para NIF, Total, IVA, Data e Nº Fatura.

**Solução:** `InvoiceFieldValidator.validate()` recebe agora `temQr: Boolean`. Quando `false`, os campos que o QR normalmente garante (nif, nifCliente, data, numeroFatura, iva, total) são rebaixados de VALID para SUSPECT com hint: `"Informação pouco segura — falta de QR code AT. Confirme na fatura original."`.

**Ficheiros alterados:**
- `core/validation/InvoiceFieldValidator.kt` — `validate(draft, temQr: Boolean = false)`; bloco de downgrade VALID→SUSPECT no fim do `buildMap`
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — `processarImagem()` passa `temQr = atQrData != null`; `processarQrEscaneado()` passa `temQr = true`

### Grupo 2 — Redesenho de layout (cabeçalhos laranja, visual profissional)

Todos os ecrãs foram redesenhados com a paleta de marca (`primary = #FF6D00`), tornando a app mais profissional e menos genérica.

**Princípios aplicados:**
- Cabeçalho com gradiente laranja em todos os ecrãs (sem `TopAppBar` branco básico)
- Botões principais com altura 52–56dp, `shape = RoundedCornerShape(12.dp)`, cor primária
- Cards com `RoundedCornerShape(12.dp)` e `elevation = 0.dp` (flat, limpo)
- Secções no `ReceiptReviewScreen` com traço laranja lateral (4×16dp)
- Botão QR ausente → card de alerta laranja proeminente (não mais `OutlinedButton` discreto)
- `CameraCaptureScreen` → botão captura circular em laranja, painel base com `RoundedCornerShape` no topo

**Ficheiros alterados:**
- `feature/home/ui/HomeScreen.kt` — cabeçalho com `Brush.verticalGradient`, botão principal FilledButton, botão secundário FilledTonalButton, Cards substituem Surface nos banners
- `feature/workerform/ui/WorkerFormScreen.kt` — cabeçalho laranja, card azul-claro de contexto, `OutlinedTextField` com `shape = RoundedCornerShape(12.dp)`, limpeza de imports não usados
- `feature/receiptcapture/ui/CameraCaptureScreen.kt` — botão captura circular `76dp` em laranja com `clickable`, painel base com cantos arredondados, botão galeria `FilledTonalIconButton`, instrução no topo mais clara
- `feature/receiptreview/ui/ReceiptReviewScreen.kt` — `TopAppBar` laranja com estado QR em subtítulo; `BotaoReescanearQr` → card de alerta laranja com `Button`; `SecaoTitulo` com traço lateral; botão "Guardar fatura" laranja 52dp; import `FilledTonalButton` desnecessário removido
- `feature/qrscan/ui/QrScanScreen.kt` — `TopAppBar` laranja, painel base com gradiente escuro, ícone `QrCodeScanner` proeminente

**Nova dependência:**
- `gradle/libs.versions.toml` — `androidx-compose-material-icons-extended` adicionada
- `app/build.gradle.kts` — `implementation(libs.androidx.compose.material.icons.extended)`

(Os ícones `QrCodeScanner`, `Badge`, `CameraAlt`, `PhotoLibrary`, `History`, `AddCircle` são da lib extended — não estavam disponíveis com `material-icons-core` apenas.)

**Não foi possível compilar neste ambiente.** Comandos de build:

```
cd C:\Users\Estagio\Projetos\ControleObras
gradlew.bat assembleDebug
"C:\Users\Estagio\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
```

**O que verificar:**
1. Todos os ecrãs abrem com cabeçalho laranja
2. Fatura sem QR → campos NIF, Total, IVA, Data, Nº Fatura mostram fundo amarelo com hint "Informação pouco segura — falta de QR code AT..."
3. Fatura com QR → campos críticos ficam verdes (VALID) como antes
4. Botão QR ausente → card laranja visível, botão "Ler QR code AT com a câmara" funcional
5. Botão "Guardar fatura" laranja em destaque, 52dp de altura
