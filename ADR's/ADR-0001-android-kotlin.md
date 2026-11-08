# 0001. Desenvolver o aplicativo Android nativo com Kotlin e Jetpack Compose

* **Status:** Aceito
* **Data:** 2026-11-07

## Contexto
O RotaCerta precisa disponibilizar funções móveis distintas para entregadores, clientes e suporte. Entre elas estão consultar rotas, acompanhar encomendas em mapas, receber avisos e transmitir localização durante uma entrega, inclusive quando o aplicativo está em segundo plano. O app deve integrar-se à mesma API utilizada pelo painel web e preservar a sessão autenticada.

## Decisão
Adotar um **aplicativo Android nativo em Kotlin**, com **Jetpack Compose e Material 3** para a interface e **Navigation Compose** para navegação. Organizar o código por módulos funcionais (`acesso/`, `entregador/`, `cliente/`, `suporte/`) e componentes compartilhados em `core/`. Utilizar **Retrofit/OkHttp** para a API, **Coroutines** para operações assíncronas, **DataStore Preferences** para sessão e **osmdroid/OpenStreetMap** para mapas. Empregar um serviço de rastreamento para o envio de GPS em segundo plano, conforme implementado no módulo do entregador.

## Alternativas Consideradas
* **Aplicativo híbrido ou multiplataforma:** Poderia compartilhar parte do código entre plataformas, mas a implementação existente usa APIs e serviços nativos do Android para a experiência móvel e o rastreamento.
* **Interfaces Android baseadas em XML:** Alternativa consolidada, mas exigiria outra abordagem para construir e atualizar telas em comparação ao Compose.
* **Injeção de dependência com Hilt/Dagger:** Poderia padronizar a criação de objetos em projetos maiores, mas o aplicativo utiliza um `AppContainer` manual, mais simples para a estrutura atual.


## Consequências
* **Positivas:**
  * Integração direta com recursos Android, incluindo serviço de localização e notificações.
  * Interface declarativa com componentes reutilizáveis e separação por perfis de uso.
  * Comunicação padronizada com a API e armazenamento local da sessão.
* **Negativas:**
  * Código específico de Android, sem execução nativa direta em iOS.
  * Necessidade de gerenciar permissões, bateria e restrições do sistema para localização em segundo plano.
  * Dependência de conectividade e tratamento de falhas de rede durante o rastreamento.
  * Maior tempo de compilação em determinados cenários: projetos Kotlin podem apresentar compilações mais demoradas.

