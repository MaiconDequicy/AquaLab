# 🌊 Aqua Lab

Aplicativo Android para monitoramento remoto da qualidade da água, integrado a uma arquitetura IoT e análise inteligente baseada em IA.

---

## 🌎 Contexto

O monitoramento da qualidade da água na região amazônica enfrenta desafios logísticos, com processos manuais lentos e dificuldade de acesso a dados em tempo real.

O Aqua Lab foi desenvolvido para resolver esse problema, permitindo coleta, análise e visualização de dados ambientais de forma remota e contínua.

---

## 🚀 Funcionalidades

* Monitoramento de pH, temperatura e turbidez
* Visualização de dados em gráficos
* Mapa interativo de pontos de coleta
* Classificação da qualidade da água (escala cromática)
* Funcionamento offline com sincronização automática
* Análise inteligente com IA (GPT)

---

## 🧠 Arquitetura do Sistema

O sistema é composto por duas camadas principais:

### 🔌 IoT

* Sensores físicos coletam dados ambientais
* Comunicação via LoRa (baixo consumo e longa distância)
* Envio de dados para Firebase

### 📱 Aplicativo Android

* Desenvolvido em Kotlin
* Arquitetura MVVM
* Persistência local com Room
* Sincronização com Firebase

---

## 🧩 Arquitetura de Software

O app segue o padrão MVVM:

* **View**: Activities e Fragments (interface)
* **ViewModel**: gerenciamento de estado e lógica
* **Model/Repository**: acesso a dados (Room + Firebase)

O projeto segue o princípio de *Single Source of Truth*.

---

## 📊 Processamento de Dados

Os dados dos sensores são:

* Filtrados por tipo (pH, temperatura)
* Processados para identificar valores mais recentes
* Convertidos para visualização em gráficos
* Utilizados para cálculo da qualidade da água

---

## 🤖 Análise com IA

O sistema utiliza IA para gerar diagnósticos automáticos:

* Combina dados de sensores + dados climáticos
* Envia para modelo GPT
* Retorna:

  * Classificação da água
  * Explicação técnica
  * Explicação simplificada

---

## 🌐 Funcionamento Offline

O aplicativo utiliza Room para armazenar dados localmente, permitindo:

* Coleta de dados sem internet
* Sincronização automática com Firebase quando a conexão é restabelecida

---

## 👥 Público-alvo

* Pesquisadores ambientais
* Educadores
* Usuários interessados em qualidade da água

---

## ⚙️ Tecnologias

* Kotlin
* Android SDK
* MVVM
* Room
* Firebase
* Retrofit
* OpenAI API
* OpenWeather API

---

## 📈 Melhorias futuras

* Suporte a novos sensores
* Alertas automáticos
* Melhorias no modelo de IA
* Avaliação de usabilidade

---

## 👨‍💻 Autor

**Maicon Costa**
