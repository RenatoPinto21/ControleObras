# Relatório — Fase de Ligação à Base de Dados
**Projeto:** Controle Obras (Android)  
**Início da análise:** 2026-07-17  
**Estado:** Implementação concluída — pronto para compilar e testar

---

## 1. Contexto e Objetivo

A aplicação Controle Obras necessita de ligar a uma base de dados MySQL/MariaDB central (PHPRetailConcept) para obter a lista de Centros de Custo (obras) associados a cada tablet.

O objetivo desta fase é implementar:
- Ligação segura ao MariaDB via ficheiro XML de configuração encriptado
- Sincronização da tabela FREF para cache local (Room)
- Dropdown de seleção de Centro de Custo no WorkerFormScreen
- Preenchimento automático do Encarregado (AGNOME) ao selecionar a obra

---

## 2. Base de Dados Remota

**Servidor:** MariaDB 10  
**Plataforma de gestão:** phpMyAdmin  
**Base de dados:** PHPRetailConcept  
**Tabela:** FREF  

### Estrutura da tabela FREF:
| Coluna   | Tipo    | Descrição                              |
|----------|---------|----------------------------------------|
| FREF     | varchar | Código do centro de custo              |
| NMFREF   | varchar | Nome da obra / centro de custo         |
| AGNOME   | varchar | Nome do encarregado responsável        |
| ENCSERIE | varchar | Número de série do dispositivo         |
| SINCTAB  | bit     | Flag de visibilidade global (1 = todos)|

### Lógica de filtro:
```sql
SELECT FREF, NMFREF, AGNOME FROM FREF 
WHERE ENCSERIE = '[serial_dispositivo]' OR SINCTAB = 1
```
- `SINCTAB = 1` → registo visível em **todos** os tablets
- `ENCSERIE = serial` → registo exclusivo daquele tablet específico

### Exemplo de dado real:
- FREF: `25202`
- NMFREF: `Mercadona Amarante - CC e IH`
- AGNOME: `Jorge Pereira (JPP)`

---

## 3. Ficheiro de Configuração XML

O acesso à BD é configurado via ficheiro XML colocado no tablet pelo administrador.  
Formato baseado no `ZEBRA_CONFIG.xml` já em uso na app Zebra (picking).

### Estrutura do ficheiro:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ZEBRA>
  <ZEID>RC001</ZEID>
  <VERSAO>01</VERSAO>
  <EMPRESA>
    <Id>RETAIL CONCEPT SA</Id>
    <NIF>508067057</NIF>
    <BD>
      <SERVIDOR>[AES-256-CBC encriptado em Base64]</SERVIDOR>
      <LOGIN>[AES-256-CBC encriptado em Base64]</LOGIN>
      <PASSWORD>[AES-256-CBC encriptado em Base64]</PASSWORD>
      <PORTA>[AES-256-CBC encriptado em Base64]</PORTA>
      <BASEDADOS>[AES-256-CBC encriptado em Base64]</BASEDADOS>
    </BD>
    <TIPOLIGACAO>
      <ONLINE>1</ONLINE>
      <TXT>0</TXT>
    </TIPOLIGACAO>
  </EMPRESA>
</ZEBRA>
```

---

## 4. Encriptação das Credenciais

**Algoritmo:** AES-256-CBC  
**Chave:** `chaveDe32Bytes182316548325967405` (32 bytes UTF-8)  
**IV:** 16 bytes a zero (fixo)  
**Formato:** string Base64 → desencripta para UTF-8  

**Referência de implementação:**  
Ficheiro `ConfigManager.cs` em `D:\estagio\Picking\Assets\Scripts`  
(app Unity anterior — Zebra TC22 — usa o mesmo algoritmo)

**Decisão confirmada:** A mesma chave será reutilizada no projeto Controle Obras.

---

## 5. Arquitetura Planeada (Kotlin Android)

```
CONTROLE_OBRAS_CONFIG.xml
(colocado pelo admin no tablet — pasta Downloads ou filesDir)
        ↓
ConfigManager.kt
— lê o XML
— desencripta credenciais com AES-256-CBC (javax.crypto)
— expõe AppConfig com os dados prontos a usar
        ↓
RemoteDatabaseManager.kt
— ligação JDBC direta ao MariaDB
— executa em background (coroutine / Dispatchers.IO)
        ↓
FrefRepository.kt
— SELECT FREF, NMFREF, AGNOME FROM FREF
  WHERE ENCSERIE = serial OR SINCTAB = 1
— guarda resultado em Room (cache offline)
        ↓
Room — tabela CentroCusto local
        ↓
WorkerFormScreen
— dropdown Centro de Custo (FREF + NMFREF)
— preenchimento automático do Encarregado (AGNOME)
        ↓
CaptureMetadata.centroCusto
— exportado no CSV como coluna CENTRO_CUSTO
```

### Ficheiros novos previstos:
- `core/config/ConfigManager.kt`
- `core/config/AppConfig.kt`
- `core/database/remote/RemoteDatabaseManager.kt`
- `core/database/remote/FrefRepository.kt`
- `core/model/CentroCusto.kt`

### Ficheiros a alterar:
- `feature/workerform/ui/WorkerFormScreen.kt` — campo dropdown + encarregado auto
- `core/model/CaptureMetadata.kt` — novo campo `centroCusto`
- `core/export/CapturaCsvExporter.kt` — nova coluna `CENTRO_CUSTO`
- `libs.versions.toml` — dependência `mysql-connector-java`

---

## 6. Decisões UX Confirmadas

| Ponto | Decisão |
|-------|---------|
| Onde aparece o Centro de Custo | WorkerFormScreen (antes de tirar fotografias) |
| Encarregado (AGNOME) | Preenchido automaticamente ao selecionar CC — campo só de leitura |
| Dropdown mostra | `FREF — NMFREF` (ex: `25202 — Mercadona Amarante - CC e IH`) |
| Sincronização | Automática ao abrir a app (background) |
| Local do XML no tablet | Assets do APK → cópia automática para filesDir na 1ª instalação |

---

## 7. Pontos Ainda em Aberto

- [x] O dropdown mostra `FREF — NMFREF` (ex: `25202 — Mercadona Amarante - CC e IH`) ✓
- [x] Sincronização automática ao abrir a app (background, sem intervenção do operador) ✓
- [x] Ficheiro XML empacotado no APK (assets), copiado automaticamente na 1ª instalação. File picker como fallback para atualização ✓
- [x] Indicador de estado da BD no HomeScreen — chip discreto (🟢 ligado / 🔴 offline) com data da última sincronização ✓

---

## 8. Histórico de Sessões

### 2026-07-17
- Análise inicial da necessidade de Centro de Custo
- Identificação da tabela FREF no PHPRetailConcept
- Confirmação do algoritmo de encriptação AES-256-CBC (mesma chave da app Zebra)
- Análise do `ConfigManager.cs` da app Unity antiga como referência
- Confirmação: Centro de Custo no WorkerFormScreen, antes da fotografia
- Confirmação: AGNOME preenchido automaticamente ao selecionar CC
- **Estado:** Brainstorming em curso — sem código escrito

### 2026-07-21 — Sessão de diagnóstico e correções runtime

**Problema:** App mostrava "BD Offline" após build bem-sucedido.

**Diagnóstico (Logcat):**
```
FrefRepository: Falha na sincronização: Failed resolution of: Ljava/sql/DriverAction;
```

**Causa raiz:** `mariadb-java-client:2.7.9` (e toda a série 2.x) usa `java.sql.DriverAction` — interface JDBC 4.2 (Java 8) **não disponível** no runtime Android. O JVM do Android marca a classe `Driver` como falhada, e chamadas subsequentes lançam `NoClassDefFoundError: org.mariadb.jdbc.Driver`.

**Tentativa 1:** Downgrade para `2.7.4` → falhou (série 2.x inteira usa `DriverAction` desde v2.3.0).

**Solução definitiva:** Downgrade para `mariadb-java-client:1.8.0` (última da série 1.x — sem `DriverAction`, JDBC padrão puro).
- Ficheiro alterado: `gradle/libs.versions.toml` → `mariadb = "1.8.0"`
- **Resultado: BD Online ✅**

**Outras correções desta sessão:**

| Alteração | Ficheiro | Motivo |
|-----------|----------|--------|
| Log de erro melhorado com classe + stacktrace | `FrefRepository.kt`, `RemoteDatabaseManager.kt` | Diagnóstico mais rápido em produção |
| Chip "BD Offline !" clicável → dialog de diagnóstico | `HomeScreen.kt` | Utilizador vê o erro técnico sem Logcat |
| Botão "Tentar novamente" no dialog | `HomeScreen.kt` | Relança sync sem reiniciar a app |
| `erroBd: StateFlow<String?>` exposto | `HomeViewModel.kt` | Propaga mensagem de erro para a UI |
| `fun tentarLigarBd()` público | `HomeViewModel.kt` | Chamado pelo botão do dialog |
| `killProcess(myPid())` no botão Sair | `ControleObrasNavHost.kt` | Mata o processo completamente (não só a Activity) |
| Android ID sem prefixo `AID_` | `DeviceInfo.kt` | Backend usa o Android ID puro em `ENCSERIE` |

**Confirmação de funcionamento:**
- Tabela FREF lida corretamente: `FREF`, `NMFREF`, `AGNOME`, `ENCSERIE`, `SINCTAB`
- Dropdown mostra `"4 — escritório barroco - geral"` (dados reais da BD)
- Campo Encarregado vazio → `AGNOME` está vazio na BD (pendente de preenchimento pelo backend)
- Filtro `WHERE ENCSERIE = ? OR SINCTAB = 1` confirmado correto

**Estado:** ✅ BD Online e funcional — checkpoint criado (ver secção 9)

---

### 2026-07-18 → 2026-07-20 — Implementação completa

**Ficheiros criados:**
- `core/config/AppConfig.kt` — data class com credenciais desencriptadas
- `core/config/ConfigManager.kt` — lê XML de assets, desencripta AES-256-CBC (javax.crypto)
- `core/database/remote/RemoteDatabaseManager.kt` — JDBC via mariadb-java-client:2.7.9
- `core/database/remote/FrefRepository.kt` — SQL filtrado por ENCSERIE/SINCTAB, cache Room
- `core/model/CentroCusto.kt` — modelo de domínio com `labelDropdown`
- `core/database/entity/CentroCustoEntity.kt` — entidade Room tabela `centro_custo`
- `core/database/dao/CentroCustoDao.kt` — Flow<List>, limpar, inserir, substituir
- `app/src/main/assets/CONTROLE_OBRAS_CONFIG.xml` — config com credenciais AES-256-CBC

**Ficheiros alterados:**
- `gradle/libs.versions.toml` — `mariadb-java-client:2.7.9`
- `app/build.gradle.kts` — dependência JDBC + packaging excludes META-INF
- `core/database/AppDatabase.kt` — versão 4, entidade CentroCusto
- `core/database/Migrations.kt` — MIGRATION_3_4
- `di/DatabaseModule.kt` — MIGRATION_3_4 + provideCentroCustoDao
- `ControleObrasApplication.kt` — companion object `appContext` (Context estático)
- `core/model/WorkerFormData.kt` — campo `ccnome: String` → `centroCusto: CentroCusto?`
- `core/export/CapturaCsvExporter.kt` — colunas FREF, FUNCDESC, ENCARREGADO
- `feature/home/viewmodel/HomeViewModel.kt` — inject FrefRepository + DeviceInfo, sincronizarBd(), estadoBd StateFlow
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — inject FrefRepository, centroCustos StateFlow
- `feature/workerform/ui/WorkerFormScreen.kt` — substituído campo texto por ExposedDropdownMenuBox + campo Encarregado read-only
- `feature/home/ui/HomeScreen.kt` — chip discreto BD (🟢 Online / 🔴 Offline / spinner) em HomeHeader

**Decisões técnicas relevantes:**
- Android ID como serial (`AID_${ANDROID_ID.uppercase()}`) — sem necessidade de permissões especiais
- `Class.forName("org.mariadb.jdbc.Driver")` obrigatório no Android (sem ServiceLoader)
- SQL usa `PHPRetailConcept.FREF` (cross-database) porque BASEDADOS=PHPGRUPO
- `ControleObrasApplication.appContext` para uso de Context em coroutines não-Hilt

**Estado:** ✅ Implementação completa — aguarda build e teste no tablet

---

---

---

### 2026-07-21 (sessão 2) — Feature Relatórios completa

**Objetivo:** Implementar secção de Relatórios com calendário, relatório diário de despesas, relatório diário de presenças e exportação PDF/CSV.

**Ficheiros criados:**
- `core/database/entity/TalaoEntity.kt` — 4 novos campos: `funcn`, `fref`, `nmfref`, `agnome`
- `core/database/Migrations.kt` — `MIGRATION_4_5` (ALTER TABLE talao ADD COLUMN ×4)
- `core/database/AppDatabase.kt` — versão 5
- `core/model/Talao.kt` — 4 campos adicionados ao modelo de domínio
- `core/database/mapper/TalaoMapper.kt` — mapeamento dos 4 novos campos
- `core/database/dao/TalaoDao.kt` — queries: `observarDatasComDados`, `observarPorData`, `obterPresencasDia`, `observarResumoPorDia`, data classes `PresencaRow`, `DiaResumoRow`
- `core/relatorios/model/DiaResumo.kt`
- `core/relatorios/model/RelatorioDespesas.kt`
- `core/relatorios/model/RelatorioPresencas.kt`
- `core/relatorios/export/RelatorioExporter.kt` — PDF (PdfDocument nativo, tema dark industrial) + CSV; partilha via FileProvider
- `feature/relatorios/viewmodel/RelatoriosViewModel.kt` — `PainelAtivo` enum, `RelatoriosUiState`, lógica de calendário + carregamento por dia
- `feature/relatorios/ui/RelatoriosScreen.kt` — layout split (42/58), `CalendarioIndustrial` custom, `CardPainel`, `PainelDespesas`, `PainelPresencas`, `BotaoExportar`
- `core/navigation/ControleObrasDestination.kt` — destino `Relatorios`
- `docs/superpowers/specs/2026-07-21-relatorios-design.md` — spec de design completa

**Ficheiros alterados:**
- `gradle/libs.versions.toml` — `mariadb-java-client:1.8.0` (fix crítico: 2.x usa `DriverAction` ausente no Android)
- `di/DatabaseModule.kt` — `MIGRATION_4_5`
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — enriquece Talao com dados do worker antes de guardar
- `core/device/DeviceInfo.kt` — removido prefixo `AID_` do Android ID
- `feature/home/viewmodel/HomeViewModel.kt` — `erroBd` StateFlow + `tentarLigarBd()`
- `feature/home/ui/HomeScreen.kt` — dialog de diagnóstico BD (motivo do erro + checklist + retry)
- `core/navigation/ControleObrasNavHost.kt` — `onSair` mata processo; Rail + BottomBar com RELAT.; `BarraNavegacaoIndustrial` expandido para 5 itens (VOLTAR, INÍCIO, SCAN, REGISTO, RELAT.)
- `app/src/main/res/xml/file_paths.xml` — `cache-path` adicionado para PDF/CSV

**Decisões técnicas:**
- Presença = submissão de talão (1 talão = 1 presença, agrupado por `funcn` por dia via `GROUP BY`)
- Exportação: PDF com `android.graphics.pdf.PdfDocument` (sem libs externas) + CSV UTF-8 com separador `;`
- Authority FileProvider: `${packageName}.fileprovider` (alinhado com AndroidManifest)
- `mariadb-java-client:2.x` incompatível com Android (toda a série 2.x usa `java.sql.DriverAction`, interface JDBC 4.2 ausente no Android runtime); solução definitiva: versão 1.8.0

**Estado:** ✅ Código completo — aguarda build no Windows e teste no tablet

---

## 9. Checkpoints / Tags Git

| Tag | Data | Estado | Descrição |
|-----|------|--------|-----------|
| `v1.0-bd-ligada` | 2026-07-21 | ✅ Estável | BD Online, FREF sync OK, diagnóstico UI, kill process, Android ID correto |

**Como voltar a este ponto:**
```bash
# Ver todos os checkpoints
git tag -l

# Voltar ao checkpoint (apenas para ver — não altera código)
git checkout v1.0-bd-ligada

# Voltar ao checkpoint e criar branch a partir dele
git checkout -b recovery/bd-ligada v1.0-bd-ligada

# Voltar ao estado actual (main)
git checkout main
```

---

*Este relatório é atualizado automaticamente ao longo das sessões de trabalho.*
