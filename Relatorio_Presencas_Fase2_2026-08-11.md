# Relatório — Presenças Fase 2 (SUBFUNC + SUBFUNC_REG)

**Data:** 2026-08-11
**Funcionalidade:** Sistema completo de marcação de presenças por centro de custo

---

## Resumo

Implementação completa da Fase 2 do ecrã de Presenças: leitura de funcionários da tabela SUBFUNC do MariaDB, cache local em Room, e escrita de registos de presença na tabela SUBFUNC_REG.

## Ficheiros criados

| Ficheiro | Descrição |
|---|---|
| `core/model/Funcionario.kt` | Modelo de domínio — funcionário associado a CC |
| `core/database/entity/SubFuncEntity.kt` | Entity Room — cache local da tabela SUBFUNC |
| `core/database/dao/SubFuncDao.kt` | DAO Room — CRUD por centro de custo |
| `core/database/remote/SubFuncRepository.kt` | Repositório JDBC — leitura SUBFUNC + escrita SUBFUNC_REG |

## Ficheiros alterados

| Ficheiro | Alteração |
|---|---|
| `core/database/AppDatabase.kt` | Adicionado SubFuncEntity + SubFuncDao, version 5 → 6 |
| `core/database/Migrations.kt` | Adicionada MIGRATION_5_6 (CREATE TABLE subfunc) |
| `di/DatabaseModule.kt` | Adicionado provideSubFuncDao + MIGRATION_5_6 |
| `feature/presencas/viewmodel/PresencasViewModel.kt` | Reescrito: AndroidViewModel, carregamento de funcionários por CC, estado de presenças, registo |
| `feature/presencas/ui/PresencasScreen.kt` | Reescrito: lista de funcionários com checkboxes, selecionar todos/nenhum, observações, botão registar, snackbar |

## Arquitetura

```
MariaDB (SUBFUNC) ──JDBC──→ SubFuncRepository ──Room──→ SubFuncDao ──Flow──→ PresencasViewModel ──StateFlow──→ PresencasScreen
                                    │
MariaDB (SUBFUNC_REG) ←──JDBC── registarPresencas()
```

## SQL utilizado

**Leitura (SUBFUNC):**
```sql
SELECT FREF, NMFREF, NOME, DESIGN, U_BISTAMPI, BISTAMP
FROM PHPRetailConcept.SUBFUNC
WHERE FREF = ?
ORDER BY NOME ASC
```

**Escrita (SUBFUNC_REG):**
```sql
INSERT INTO PHPRetailConcept.SUBFUNC_REG
    (ENCSERIE, REGSTAMP, U_BISTAMPI, BISTAMP, DATA, HORA, OBS)
VALUES (?, ?, ?, ?, ?, ?, ?)
```

## Base de dados Room

- **Versão:** 5 → 6
- **Nova tabela:** `subfunc` (fref, nmfref, nome, designacao, u_bistampi, bistamp)
- **Chave primária:** bistamp
- **Migração:** MIGRATION_5_6

## Funcionalidades do ecrã

1. Dropdown de centro de custo (já existia)
2. Ao selecionar CC → sincroniza funcionários via JDBC → guarda em Room
3. Lista de funcionários com checkbox presente/ausente
4. Botões selecionar todos / limpar seleção
5. Campo de observações (opcional)
6. Botão "Registar Presenças" → INSERT em batch no SUBFUNC_REG
7. Snackbar de feedback (sucesso/erro)
8. Indicadores de loading (sync + envio)

## Próximos passos

- Build e teste no tablet
- Verificar se as colunas da tabela SUBFUNC correspondem ao SQL
- Verificar se SUBFUNC_REG aceita os tipos de dados enviados
- Considerar validação de duplicados (mesma data + mesmo funcionário)
