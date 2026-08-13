# Design — Secção de Relatórios com Calendário
**Data:** 2026-07-21  
**Estado:** Aprovado — em implementação

---

## 1. Objetivo

Adicionar uma secção "Relatórios" à app Controle Obras que permite ao utilizador:
- Navegar num calendário mensal e selecionar um dia
- Ver e exportar um **relatório de despesas** do dia selecionado
- Ver e exportar um **relatório de presenças** do dia selecionado

---

## 2. Decisões Chave

| Ponto | Decisão |
|-------|---------|
| Definição de presença | Submissão de pelo menos um talão = funcionário presente nesse dia |
| Deduplicação presenças | 1 presença por `funcn` por dia (mesmo que submeta múltiplos talões) |
| Formato exportação | PDF e CSV — utilizador escolhe no momento |
| Layout | Calendário + painel expansível abaixo (Abordagem A) |
| Posição na nav | Novo item no Rail/BottomBar: ícone calendário, label "RELAT." |
| Biblioteca PDF | `android.graphics.pdf.PdfDocument` (nativo Android, sem dependências) |

---

## 3. Alterações à Base de Dados (Room v4 → v5)

`TalaoEntity` não persiste dados do `WorkerFormData`. É necessário adicionar:

| Campo novo | Tipo | Descrição |
|------------|------|-----------|
| `funcn` | TEXT | Nº do funcionário que submeteu |
| `fref` | TEXT | Código do centro de custo |
| `nmfref` | TEXT | Nome do centro de custo |
| `agnome` | TEXT | Nome do encarregado |

**Migration:** `MIGRATION_4_5` via `ALTER TABLE talao ADD COLUMN`.  
**ReceiptFlowViewModel:** atualizar `guardarCaptura()` para incluir estes campos.

---

## 4. Arquitetura da Feature

```
feature/relatorios/
├── ui/
│   └── RelatoriosScreen.kt        ← calendário + painel despesas/presenças
└── viewmodel/
    └── RelatoriosViewModel.kt     ← lógica, queries, estado

core/relatorios/
├── model/
│   ├── DiaResumo.kt               ← indicadores por dia (para o calendário)
│   ├── RelatorioDespesas.kt       ← dados do relatório de despesas
│   └── RelatorioPresencas.kt      ← dados do relatório de presenças
└── export/
    └── RelatorioExporter.kt       ← PDF + CSV (partilha via Intent)
```

---

## 5. UI — RelatoriosScreen

### Estrutura visual

```
┌─────────────────────────────────────────────────────┐
│  HEADER: "Relatórios"  [Mês Ano]  [◀]  [▶]          │
├─────────────────────────────────────────────────────┤
│                                                      │
│   SEG  TER  QUA  QUI  SEX  SAB  DOM                  │
│    1    2    3●   4    5●   6    7                    │
│    8    9   10   11●  12   13   14                    │
│   15   16   17   18   19   20   21                    │
│   22   23   24   25   26   27   28                    │
│   29   30   31                                        │
│                                                      │
│  ● laranja = tem despesas   ● branco = tem presenças  │
├─────────────────────────────────────────────────────┤
│  DIA SELECIONADO: Terça, 3 de Julho                  │
│  ┌──────────────┐  ┌──────────────┐                  │
│  │  DESPESAS    │  │  PRESENÇAS   │  ← tabs / cards  │
│  │  3 talões    │  │  2 func.     │                  │
│  │  € 248,50    │  │              │                  │
│  └──────────────┘  └──────────────┘                  │
│                                                      │
│  [Lista do painel ativo]                             │
│                                                      │
│  [Exportar PDF]  [Exportar CSV]                      │
└─────────────────────────────────────────────────────┘
```

### Calendário custom
- Grade 7 colunas × N linhas
- Dia ativo: fundo laranja `IndustrialGlow`
- Dias com dados: ponto indicador (laranja = despesas, branco = presenças)
- Hoje: borda subtil branca
- Dias de outros meses: opacity 30%

### Painel de relatório
- **Dois cards/tabs**: DESPESAS e PRESENÇAS
- Clique num card activa o painel de detalhe abaixo
- DESPESAS: lista de talões (empresa, total, hora)
- PRESENÇAS: lista de funcionários únicos (funcn, nmfref, count talões)
- Botões exportar: `[PDF]` e `[CSV]` em baixo

---

## 6. Queries Room (TalaoDao)

```kotlin
// Datas com registos — para indicadores do calendário
@Query("SELECT DISTINCT data FROM talao WHERE data IS NOT NULL")
fun observarDatasComDados(): Flow<List<LocalDate>>

// Talões de um dia (despesas)
@Query("SELECT * FROM talao WHERE data = :data ORDER BY hora ASC")
fun observarPorData(data: LocalDate): Flow<List<TalaoEntity>>

// Presenças de um dia (funcn únicos)
@Query("SELECT DISTINCT funcn, fref, nmfref, agnome FROM talao WHERE data = :data AND funcn != ''")
suspend fun obterPresencasDia(data: LocalDate): List<PresencaRow>
```

---

## 7. Export

### PDF (`android.graphics.pdf.PdfDocument`)
- Cabeçalho: logo texto "Controle Obras" + empresa + data do relatório
- Tabela de dados desenhada com `canvas.drawText` / `drawLine`
- Partilhado via `FileProvider` + `Intent.ACTION_SEND`

### CSV
- Separador `;` — padrão já estabelecido no projeto
- Partilhado via `Intent.ACTION_SEND`

---

## 8. Ficheiros Alterados / Criados

**Criados:**
- `feature/relatorios/ui/RelatoriosScreen.kt`
- `feature/relatorios/viewmodel/RelatoriosViewModel.kt`
- `core/relatorios/model/DiaResumo.kt`
- `core/relatorios/model/RelatorioDespesas.kt`
- `core/relatorios/model/RelatorioPresencas.kt`
- `core/relatorios/export/RelatorioExporter.kt`

**Alterados:**
- `core/database/entity/TalaoEntity.kt` — 4 novos campos
- `core/database/dao/TalaoDao.kt` — 3 novas queries
- `core/database/Migrations.kt` — MIGRATION_4_5
- `core/database/AppDatabase.kt` — version 5
- `di/DatabaseModule.kt` — MIGRATION_4_5
- `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` — guardar funcn/cc
- `core/navigation/ControleObrasDestination.kt` — Relatorios destination
- `core/navigation/ControleObrasNavHost.kt` — composable + RailItem + BottomBar
