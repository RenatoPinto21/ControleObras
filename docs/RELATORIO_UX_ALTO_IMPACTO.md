# Relatório — Melhorias UX de Alto Impacto
**Projeto:** Controle Obras (Android)  
**Data:** 2026-08-11  
**Estado:** Implementação concluída — pronto para compilar e testar

---

## 1. Resumo

Implementadas 4 melhorias UX de alto impacto identificadas na análise:

1. Dashboard com card de presenças hoje
2. Indicador de sync com timestamp no header
3. Alertas e lembretes locais via WorkManager
4. Presenças com swipe e agrupamento por função

---

## 2. Ficheiros Alterados

### 2.1 HomeViewModel.kt (feature/home/viewmodel/)
- **Injeção:** `SubFuncRepository` adicionado ao constructor
- **ResumoDia:** novo campo `totalPresencas: Int`
- **Novo método:** `carregarPresencasHoje()` — consulta JDBC para contar presenças do dia
- **`ultimaSync`:** novo StateFlow com timestamp da última sincronização
- **`observarResumoDia()`:** usa `.copy()` para preservar `totalPresencas` ao atualizar
- **`verificarFeedbackFatura()`:** agora também recarrega presenças ao retomar

### 2.2 HomeScreen.kt (feature/home/ui/)
- **3º card:** "Presenças" com ícone People e cor azul no `SecaoResumoDia`
- **ChipEstadoBd:** recebe `ultimaSync`, mostra hora da última sync (ex: "14:32") quando ONLINE
- **HomeHeader:** novo parâmetro `ultimaSync: Long`
- **Import:** `Icons.Outlined.People`

### 2.3 AppPreferences.kt (core/common/)
- **Novo campo:** `ultimaSyncTimestamp: Long` — persiste timestamp da última sync bem-sucedida
- **Nova constante:** `CHAVE_ULTIMA_SYNC`

### 2.4 ControleObrasApplication.kt
- **Implementa:** `Configuration.Provider` para integração WorkManager + Hilt
- **HiltWorkerFactory:** injetada via `@Inject`
- **Canal de notificação:** "Lembretes" criado em `onCreate()`
- **LembreteScheduler:** agendado em `onCreate()`

### 2.5 AndroidManifest.xml
- **Permissão:** `POST_NOTIFICATIONS` adicionada
- **Provider:** desativação do `WorkManagerInitializer` automático (usamos HiltWorkerFactory)

### 2.6 LembreteWorker.kt (core/worker/) — NOVO
- **@HiltWorker** com injeção de `SubFuncRepository` + `TalaoDao`
- **Verifica:** presenças não registadas (SUBFUNC_REG) e talões pendentes
- **Notificações:** só em dias úteis (seg–sex), depois das 9h
- **Canal:** `lembretes_controle_obras`

### 2.7 LembreteScheduler.kt (core/worker/) — NOVO
- Agendamento periódico a cada 4h com `PeriodicWorkRequest`
- Requisito: `NetworkType.CONNECTED` (precisa de rede para JDBC)
- Política: `ExistingPeriodicWorkPolicy.KEEP` (não duplica)

### 2.8 TalaoDao.kt (core/database/dao/)
- **Novo método:** `contarPorData(data: String): Int` — contagem simples para o Worker

### 2.9 PresencasScreen.kt (feature/presencas/ui/)
- **Agrupamento:** lista agrupada por `designacao` com `stickyHeader`
- **Cabeçalhos:** nome da função em UPPERCASE, contador do grupo, botão "Todos/Limpar"
- **Swipe:** `SwipeToDismissBox` em cada item — arrastar direita = toggle presença
- **Fundo revelado:** verde escuro (marcar) ou vermelho escuro (desmarcar)
- **Per-group toggle:** botão no header do grupo seleciona/limpa todos do grupo

### 2.10 libs.versions.toml
- **workRuntime:** `2.10.1`
- **hiltWork:** `1.3.0`
- **Novas libs:** `androidx-work-runtime-ktx`, `androidx-hilt-work`, `androidx-hilt-compiler`

### 2.11 app/build.gradle.kts
- **Dependências:** `work-runtime-ktx`, `hilt-work`, `hilt-compiler` (androidx) adicionadas

---

## 3. Notas Técnicas

- O `SwipeToDismissBox` retorna `false` no `confirmValueChange` para impedir que o item desapareça — apenas executa o toggle e volta à posição
- A contagem de presenças no dashboard é assíncrona (JDBC via IO dispatcher) — pode demorar uns segundos no primeiro carregamento
- O WorkManager precisa de rede para consultar SUBFUNC_REG — constraint `NetworkType.CONNECTED`
- A permissão `POST_NOTIFICATIONS` precisa ser pedida em runtime no Android 13+ — a notificação é simplesmente omitida se não tiver permissão
- O `workManagerConfiguration` usa a property getter do Kotlin em vez de `getWorkManagerConfiguration()` para compatibilidade com versões recentes da API

---

## 4. Compilação

```bash
cd C:\Users\Estagio\Projetos\ControleObras
.\gradlew assembleDebug
```

### Instalar no tablet:
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 5. Próximos Passos

- Testar notificações no tablet (conceder permissão POST_NOTIFICATIONS)
- Verificar se a query de presenças no dashboard é rápida o suficiente
- Considerar pedir permissão de notificações na primeira utilização
- Testar swipe em tablets com diferentes densidades de ecrã
