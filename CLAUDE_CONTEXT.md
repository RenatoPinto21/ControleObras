# CLAUDE_CONTEXT.md

# Controle Obras - Contexto do Projeto

## Visão Geral

Este projeto destina-se ao desenvolvimento de uma aplicação Android
profissional chamada **Controle Obras** para utilização em tablets
Android em ambiente de construção civil.

O projeto será desenvolvido por fases e deverá manter elevada qualidade
de código, arquitetura consistente e facilidade de manutenção.

## Estado Atual

-   Projeto criado no Android Studio
-   Linguagem: Kotlin
-   Interface: Jetpack Compose
-   Build: Gradle Kotlin DSL
-   Package: `pt.controleobras.app`
-   Localização: `C:\Users\Estagio\Projetos\ControleObras`
-   Git inicializado
-   Primeiro commit realizado
-   Projeto enviado para GitHub

Nunca criar um novo projeto. Todo o desenvolvimento deverá ocorrer sobre
este projeto.

## Hardware Principal

Tablet Android

-   Processador MediaTek Helio G81
-   RAM 8 GB (+4 GB virtual)
-   Armazenamento 128 GB
-   Câmara 16 MP
-   Resolução 1920x1200

A aplicação deverá ser otimizada para tablets.

## Objetivo da versão 1

Sem login.

O utilizador pode:

1.  Tirar fotografia de um talão (CameraX)
2.  Ou escolher uma imagem da galeria
3.  Executar OCR com Google ML Kit
4.  Extrair automaticamente:
    -   Empresa
    -   NIF
    -   Morada
    -   Data
    -   Hora
    -   Número da fatura
    -   Produtos
    -   Quantidades
    -   Preços
    -   IVA
    -   Total
5.  Permitir correção manual
6.  Guardar:
    -   Imagem original
    -   JSON
    -   XML
7.  Guardar dados localmente com Room Database

A aplicação deverá funcionar totalmente offline.

## Tecnologias

-   Kotlin
-   Jetpack Compose
-   MVVM
-   Material Design 3
-   Room Database
-   CameraX
-   Google ML Kit OCR
-   Kotlin Serialization
-   Version Catalog
-   Git
-   GitHub

## Regras para o Claude

-   Trabalhar sempre por fases.
-   Nunca gerar milhares de linhas de código numa única resposta.
-   Explicar primeiro a arquitetura.
-   Explicar que ficheiros serão alterados.
-   Confirmar que o projeto continua compilável.
-   Não inventar APIs.
-   Não criar código duplicado.
-   Seguir SOLID e Clean Code.
-   Fazer perguntas quando existirem dúvidas.

## Roadmap

1.  Arquitetura
2.  Navegação
3.  Ecrã Inicial
4.  CameraX
5.  Galeria
6.  OCR
7.  Parser de Talões
8.  Room Database
9.  JSON
10. XML
11. Gestão de Obras
12. Relatórios
13. Sincronização
14. Dashboard

## Estrutura Esperada

    ControleObras
    │
    ├── README.md
    ├── CLAUDE_CONTEXT.md
    ├── PROJECT_ARCHITECTURE.md
    ├── DEVELOPMENT_PLAN.md
    ├── CLAUDE_RULES.md
    ├── ROADMAP.md
    ├── CHANGELOG.md
    ├── TODO.md
    └── app

## Filosofia

Priorizar qualidade acima da velocidade.

Cada fase deve terminar com um projeto compilável.

Nunca reestruturar o projeto sem justificação.

Sempre manter documentação atualizada.

Sempre preservar compatibilidade com Android Studio.
