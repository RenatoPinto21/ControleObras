# Design: Extrator Posicional de Faturas com Validação
**Data:** 2026-07-09  
**Estado:** Aprovado pelo utilizador  
**Objetivo:** Substituir o pipeline OCR→texto→Gemma LLM por um extrator posicional rápido (~1-2s) com camada de validação por campo, mantendo funcionamento 100% offline.

---

## Contexto e Problema

### Situação atual
```
Imagem → ML Kit OCR → texto bruto → Gemma 3 1B (LLM) → JSON
```
- Velocidade: 30-60 segundos por fatura
- Perde toda a informação visual (colunas, tabelas, alinhamentos)
- O LLM interpreta texto desordenado e erra com frequência
- Risco de "alucinação" — o modelo pode inventar valores

### Situação alvo
```
Imagem → ML Kit OCR estruturado → extrator posicional → validação → JSON
                                          ↑
                                   QR code AT (merge)
```
- Velocidade: 1-2 segundos por fatura
- Preserva layout visual (coordenadas x, y de cada palavra)
- Nunca inventa valores — encontra ou marca como em falta
- Validação por campo antes de preencher

### Contexto de utilização
- Tablets Android com 8 GB RAM + 4 GB extended RAM
- Uso intensivo: várias fotografias por dia por tablet
- 100% offline (estaleiros de construção civil)
- Maioria das faturas tem QR code AT (Portaria 195/2020)
- Faturas portuguesas (formato SAF-T PT)

---

## Arquitetura — 4 Camadas

### Camada 1: StructuredMlKitTextRecognizer
**Ficheiro:** `core/ocr/MlKitTextRecognizer.kt` (alterado)  
**Novo modelo:** `core/ocr/StructuredOcrResult.kt`

O ML Kit já devolve `TextBlock → TextLine → TextElement`, cada um com `boundingBox: Rect`. Actualmente o `recognizeText()` concatena tudo numa `String` e descarta as posições.

A alteração devolve `StructuredOcrResult` que preserva a hierarquia completa:

```kotlin
data class StructuredOcrResult(
    val elements: List<OcrElement>,
    val imageWidth: Int,
    val imageHeight: Int
)

data class OcrElement(
    val text: String,
    val left: Float,    // posição normalizada 0.0-1.0
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float
)
```

As coordenadas são normalizadas (0.0 a 1.0) para independência da resolução da imagem.

---

### Camada 2: PositionAwareReceiptExtractor
**Ficheiro novo:** `core/extractor/PositionAwareReceiptExtractor.kt`

Divide a imagem em **3 zonas verticais** baseadas em percentagem da altura:

| Zona | Y normalizado | Conteúdo esperado |
|---|---|---|
| Cabeçalho | 0.0 – 0.40 | Empresa, NIF, morada, nº fatura, data, hora |
| Corpo | 0.35 – 0.75 | Tabela de produtos (linhas × colunas) |
| Rodapé | 0.65 – 1.00 | Subtotal, IVA, total, método pagamento |

As zonas têm sobreposição intencional para faturas com layouts atípicos.

#### Extração do cabeçalho
Aplica expressões regulares a elementos da zona de cabeçalho:
- **NIF:** `\b[1-9]\d{8}\b` — 9 dígitos, começa por 1-9
- **Data:** `\b\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{2,4}\b`
- **Hora:** `\b\d{1,2}:\d{2}\b`
- **Nº fatura:** `\b(FT|FR|FS|NC|ND|OR|GT|AA|DA)\s*[A-Z0-9]+[/\-]\d+\b`
- **Empresa:** linha de maior tamanho de fonte no topo (maior bounding box de altura)
- **Morada:** linhas abaixo da empresa que contêm padrões de morada PT (código postal `\d{4}-\d{3}`)

#### Extração da tabela de produtos
1. Filtra elementos da zona do corpo
2. Agrupa elementos por proximidade vertical (mesma linha: `|top_a - top_b| < 0.015`)
3. Para cada linha, identifica colunas pela posição X:
   - X < 0.50 → descrição do produto
   - 0.50 ≤ X < 0.65 → quantidade
   - 0.65 ≤ X < 0.80 → preço unitário
   - X ≥ 0.80 → total da linha
4. Filtra linhas que não têm pelo menos descrição + um valor numérico

#### Extração do rodapé
Procura padrões específicos na zona inferior:
- **Total:** linha com "TOTAL" (case-insensitive) + valor monetário
- **IVA:** linha com "IVA" ou "Taxa" + valor monetário  
- **Subtotal:** linha com "SUBTOTAL" ou "S/IVA" + valor monetário
- **Método pagamento:** tokens "MULTIBANCO", "MB WAY", "NUMERÁRIO", "VISA", "MASTERCARD", "TRANSFERÊNCIA"

---

### Camada 3: InvoiceFieldValidator
**Ficheiro novo:** `core/validation/InvoiceFieldValidator.kt`  
**Modelo novo:** `core/validation/ValidatedField.kt`

Cada campo extraído passa pela validação antes de ser incluído no draft.

```kotlin
data class ValidatedField<T>(
    val value: T?,
    val state: FieldState,
    val reason: String? = null   // explicação quando suspeito ou em falta
)

enum class FieldState {
    VALID,      // encontrado e validação passou — mostra em verde
    SUSPECT,    // encontrado mas validação falhou — mostra em amarelo com aviso
    MISSING     // não encontrado — mostra em cinzento com aviso
}
```

#### Regras de validação por campo

**NIF (fornecedor e cliente):**
1. Deve ter exactamente 9 dígitos
2. Não contém letras ou símbolos
3. Primeiro dígito ∈ {1, 2, 3, 5, 6, 7, 8, 9} (nunca 0 ou 4)
4. Checksum português válido (algoritmo módulo 11):
   ```
   soma = Σ (dígito[i] × peso[i]) para i=0..7, pesos = [9,8,7,6,5,4,3,2]
   resto = soma % 11
   dígito_controlo = if (resto < 2) 0 else (11 - resto)
   válido = (dígito_controlo == dígito[8])
   ```

**Data de emissão:**
1. Deve ser uma data válida no calendário
2. Não deve ser posterior à data actual (tolerância: +3 dias)
3. Não deve ser anterior a 2000-01-01
4. Normalizada para formato `dd/MM/yyyy`

**Hora:**
1. Formato `HH:mm` ou `H:mm`
2. Hora ∈ [0, 23], minuto ∈ [0, 59]

**Número de fatura:**
1. Deve conter prefixo documental português: FT, FR, FS, NC, ND, OR, GT, AA, DA
2. Deve conter separador `/` ou `-`
3. Deve conter parte numérica após separador

**Valores monetários (total, subtotal, IVA):**
1. Deve ser numérico após normalização (substituir `,` por `.`)
2. Deve ser positivo
3. Cross-validation quando os 3 valores estão presentes:
   - `|subtotal + IVA_total - total| ≤ 0.02` (tolerância de 2 cêntimos para arredondamentos)

**Taxa de IVA por linha:**
1. Deve ser um dos valores legais em Portugal: 6, 13 ou 23
2. Se fora destes valores → SUSPECT

**Total por linha de produto:**
1. `|quantidade × preço_unitário - total_linha| ≤ 0.02`
2. Se divergência superior → SUSPECT

**Método de pagamento:**
1. Normaliza para lista canónica: MULTIBANCO, MB WAY, NUMERÁRIO, VISA, MASTERCARD, TRANSFERÊNCIA, DÉBITO DIRETO
2. Se valor reconhecível mas com ortografia diferente → normaliza e marca VALID
3. Se não reconhecível → MISSING

---

### Camada 4: Merge com QR code AT
**Ficheiro alterado:** `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt`

Os campos do QR code AT têm **prioridade absoluta** — substituem sempre o extrator posicional para os campos em comum. São considerados automaticamente VALID (assinados pela AT).

Campos cobertos pelo QR code AT:
- `A` → NIF fornecedor
- `B` → NIF cliente (**o nosso NIF**)
- `F` → Data emissão
- `G` → Número fatura
- `N` → Total IVA
- `O` → Total com IVA

Campos **não cobertos** pelo QR (sempre vêm do extrator posicional):
- Nome da empresa
- Morada
- Hora
- Linhas de produtos
- Método de pagamento
- Subtotal

---

## Ecrã de Revisão — 3 Estados Visuais

**Ficheiro alterado:** `feature/receiptreview/ui/ReceiptReviewScreen.kt`

Cada campo passa a ter cor e ícone conforme o estado:

| Estado | Cor fundo | Ícone | Significado |
|---|---|---|---|
| VALID | Verde claro `#E8F5E9` | ✓ | Extraído e validado |
| SUSPECT | Amarelo `#FFF8E1` | ⚠️ | Encontrado mas suspeito — confirme |
| MISSING | Cinzento `#F5F5F5` | ❓ | Não encontrado — verifique na imagem |

O botão "Guardar fatura" fica disponível mesmo com campos SUSPECT ou MISSING — o utilizador pode guardar após revisão visual. Não existe bloqueio forçado.

---

## Ficheiros — Resumo de Alterações

### Ficheiros novos
| Ficheiro | Responsabilidade |
|---|---|
| `core/ocr/StructuredOcrResult.kt` | Modelo de dados OCR com posições normalizadas |
| `core/extractor/PositionAwareReceiptExtractor.kt` | Extração por zonas e colunas |
| `core/validation/ValidatedField.kt` | Modelo campo validado (valor + estado + razão) |
| `core/validation/InvoiceFieldValidator.kt` | Todas as regras de validação |
| `core/validation/NifValidator.kt` | Algoritmo checksum NIF português (módulo 11) |

### Ficheiros alterados
| Ficheiro | O que muda |
|---|---|
| `core/ocr/MlKitTextRecognizer.kt` | Devolve `StructuredOcrResult` em vez de `String` |
| `feature/receiptflow/viewmodel/ReceiptFlowViewModel.kt` | Usa novo pipeline, orquestra validação |
| `feature/receiptflow/viewmodel/ReceiptFlowUiState.kt` | Inclui campos com estado de validação |
| `feature/receiptreview/ui/ReceiptReviewScreen.kt` | 3 estados visuais por campo |

### Ficheiros removidos / desativados
| Ficheiro | Decisão |
|---|---|
| `core/llm/MediaPipeLlmExtractor.kt` | Desativado como primário; mantido como fallback opcional |
| `core/parser/HeuristicReceiptParser.kt` | Mantido como fallback de último recurso |

---

## Fases de Implementação

### Fase 1 — Base (OCR estruturado + validação)
1. `StructuredOcrResult` + `ValidatedField` + `NifValidator`
2. Alterar `MlKitTextRecognizer` para devolver estrutura com posições
3. `InvoiceFieldValidator` com todas as regras
4. Testes unitários das regras de validação (especialmente NIF checksum)

### Fase 2 — Extrator posicional
1. `PositionAwareReceiptExtractor` — zonas e extração de cabeçalho/rodapé
2. Extração de tabela de produtos com detecção de colunas
3. Integração no `ReceiptFlowViewModel`

### Fase 3 — UI e merge QR
1. Atualizar `ReceiptReviewScreen` com 3 estados visuais
2. Expor campo NIF cliente (campo B do QR) na UI
3. Merge final QR + extrator posicional

### Fase 4 — Testes de campo
1. Testar com 10+ faturas reais de fornecedores variados
2. Ajustar thresholds de zonas e colunas conforme necessário
3. Decidir se o Gemma é mantido como fallback ou removido

---

## Critérios de Sucesso

- Velocidade de processamento: < 3 segundos do toque na câmara ao ecrã de revisão preenchido
- NIF nunca aceite com checksum inválido
- Cross-validation monetária detecta divergências ≥ 0.03€
- Faturas sem QR code processadas sem erros (apenas mais campos MISSING)
- Zero "alucinações" — nenhum valor inventado, apenas MISSING quando não encontrado
