# DEVELOPMENT_PLAN.md — Controle Obras

Roadmap detalhado. Cada fase termina com o projeto **compilável** e deve ser confirmada pelo utilizador antes de avançar para a seguinte. Ver `PROJECT_ARCHITECTURE.md` para as decisões de arquitetura por trás de cada fase.

Data: 2026-07-07

---

**Fase 0 — Higiene e documentação**
Resolver alterações Git pendentes, criar `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `TODO.md`, `CLAUDE_RULES.md`. Sem código de aplicação.

**Fase 1 — Fundação de arquitetura**
Estrutura de pacotes (secção 3), tema Material 3 próprio da marca, Hilt configurado (`ControleObrasApplication`), Navigation Compose com um único ecrã placeholder.

**Fase 2 — Ecrã Inicial (Home)**
`Scaffold` com topo/navegação principal e ponto de entrada "Novo Talão" (ainda sem ação real).

**Fase 3 — Modelo de dados**
`core/model`: `Talao`, `ItemTalao` e campos associados (Empresa, NIF, Morada, Data, Hora, Número da Fatura, Quantidade, Preço Unitário, IVA, Total, Observações). Sem persistência ainda. Testes unitários dos modelos.

**Fase 4 — Room Database**
Entities, DAOs, `AppDatabase`, estratégia de migração definida desde já. Repositório com dados de teste em memória.

**Fase 5 — CameraX**
Captura de fotografia, permissões em runtime, guardar imagem localmente.

**Fase 6 — Seleção da galeria**
Photo Picker API do Android (sem dependência extra).

**Fase 7 — Integração ML Kit OCR**
Reconhecimento de texto bruto a partir da imagem capturada/selecionada. Validar comportamento offline (modo avião).

**Fase 8 — Parser de talões**
Heurísticas/regex para extrair os campos estruturados a partir do texto OCR, isoladas em classes de domínio testáveis.

**Fase 9 — Ecrã de confirmação/edição**
Apresentar os campos extraídos, permitir correção manual, validação de campos obrigatórios.

**Fase 10 — Persistência real**
Gravar `Talao` completo + imagem + JSON (Kotlin Serialization) na Room DB.

**Fase 11 — Exportação XML**
Serializer XML próprio (ver decisão na secção 6 do `PROJECT_ARCHITECTURE.md`).

**Fase 12 — Histórico de talões**
Lista dos talões guardados, pesquisa/filtro simples.

**Fase 13 — Avaliação de modularização Gradle**
Só se o código já justificar (ver Opção A/B na secção 4 do `PROJECT_ARCHITECTURE.md`).

**Fase 14+ — Fora do escopo imediato**
Gestão de obras, clientes, fornecedores, materiais, relatórios, sincronização com servidor, autenticação de utilizadores. Cada uma será planeada em detalhe quando for iniciada.
