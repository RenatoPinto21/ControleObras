# Relatório — Consulta de Presenças Registadas nos Relatórios
**Projeto:** Controle Obras (Android)  
**Data:** 2026-08-11  
**Estado:** Implementação concluída — pronto para compilar e testar

---

## 1. Objetivo

Permitir consultar no ecrã de Relatórios as presenças registadas via SUBFUNC_REG (MariaDB), com filtro por centro de custo.

Anteriormente o painel de presenças mostrava apenas dados derivados dos talões (funcn). Com esta alteração, passa a mostrar os registos reais inseridos pelo ecrã de Presenças.

---

## 2. Ficheiros Alterados

### 2.1 SubFuncRepository.kt (core/database/remote/)
- **Adicionado:** método `consultarPresencas(data, fref?)` — consulta JDBC ao SUBFUNC_REG com LEFT JOIN a SUBFUNC
- **SQL:** `SQL_CONSULTAR_REG` (todos os CC) e `SQL_CONSULTAR_REG_CC` (filtrado por CC)
- Todos os `rs.getString()` com `.trim()` (padrão MariaDB CHAR)

### 2.2 RelatorioPresencas.kt (core/relatorios/model/)
- **Adicionado:** `RelatorioPresencasReg` — modelo com data, linhas e filtro CC
- **Adicionado:** `LinhaPresencaReg` — nome, designacao, fref, nmfref, hora, obs, bistamp

### 2.3 RelatoriosViewModel.kt (feature/relatorios/viewmodel/)
- **Injeção:** SubFuncRepository + FrefRepository adicionados ao constructor
- **UiState expandido:** novos campos `presencasReg`, `centrosCusto`, `ccFiltro`
- **Novo método:** `filtrarPresencasPorCC(fref?)` — muda filtro e recarrega
- **carregarDia():** agora também invoca `carregarPresencasReg()` via JDBC
- **init:** observa lista de CC do FrefRepository (para dropdown de filtro)

### 2.4 RelatoriosScreen.kt (feature/relatorios/ui/)
- **Substituído:** `PainelPresencas` → `PainelPresencasReg`
- **Novo composable:** `PainelPresencasReg` com:
  - Barra de filtro CC minimalista (label + botão FilterList)
  - DropdownMenu com "Todos" + lista de CCs
  - Botão fica laranja quando filtro ativo
  - Cabeçalho: Nome | Função | Hora | Obs
  - Nome em destaque (bodyMedium, Bold, branco)
- **Novo composable:** `LinhaPresencaRegItem`
- **Cards:** contador de presenças usa `presencasReg` em vez de `presencas`
- **Exportação:** PDF/CSV usam novos métodos do exporter

### 2.5 RelatorioExporter.kt (core/relatorios/export/)
- **Adicionado:** `exportarPresencasRegPdf()` — PDF com nome, função, CC, hora, obs
- **Adicionado:** `exportarPresencasRegCsv()` — CSV com todos os campos

---

## 3. Fluxo de Dados

```
Utilizador seleciona dia no calendário
  → RelatoriosViewModel.selecionarDia(dia)
    → carregarDia(dia)
      → carregarPresencasReg(dia, ccFiltro)
        → SubFuncRepository.consultarPresencas(data, fref)
          → JDBC: SELECT ... FROM SUBFUNC_REG r LEFT JOIN SUBFUNC s
        → UiState.presencasReg = RelatorioPresencasReg(...)

Utilizador clica no filtro CC
  → RelatoriosViewModel.filtrarPresencasPorCC(fref)
    → carregarPresencasReg(dia, novoFref)
      → JDBC com WHERE s.FREF = ?
```

---

## 4. Notas Técnicas

- A consulta usa LEFT JOIN para que registos com BISTAMP sem correspondência em SUBFUNC apareçam (nome "—")
- O filtro CC é aplicado na query SQL (server-side), não no cliente
- A lista de CCs vem do Room (cache local do FrefRepository), já carregada pelo ecrã de Presenças
- Os métodos antigos de exportação (`exportarPresencasPdf/Csv`) mantêm-se por compatibilidade mas não são chamados pela UI

---

## 5. Correções de Segurança e Robustez (2026-08-11)

Auditoria de correcção comparando o código com a estrutura real da BD (SUBFUNC_REG com 11 colunas).

### Ficheiros alterados:

**SubFuncRepository.kt (core/database/remote/)**
- DATA: `setString()` → `setDate()` (coluna é datetime)
- HORA: `setString()` → `setTime()` (coluna é time)
- OUSRINIS: `encserie.take(30)` — proteção contra truncatura (varchar(30))
- WHERE: `r.DATA = ?` → `DATE(r.DATA) = ?` — evita falhas se DATA tiver hora ≠ 00:00:00
- Prevenção de duplicados: nova query `SQL_EXISTE_REG` verifica se BISTAMP+DATA já existe antes de inserir
- KDoc duplicado removido (bloco órfão antes de consultarPresencas)
- consultarPresencas: agora mapeia REGSTAMP, DATAREG, HORAREG do ResultSet

**RelatorioPresencas.kt (core/relatorios/model/)**
- LinhaPresencaReg: adicionados campos `regstamp`, `dataReg`, `horaReg` (com defaults vazios para compatibilidade)

### Lógica de duplicados:
```
Para cada funcionário no lote:
  1. SELECT COUNT(*) WHERE BISTAMP = ? AND DATE(DATA) = ?
  2. Se COUNT > 0 → ignorar (já registado)
  3. Caso contrário → incluir no batch INSERT
```

---

## 6. Próximos Passos

- Testar com dados reais no tablet
- Verificar se o JOIN SUBFUNC funciona com todos os registos
- Considerar paginação se o volume de presenças por dia for muito grande
- Adicionar indicador de presenças registadas no calendário (ponto adicional)
