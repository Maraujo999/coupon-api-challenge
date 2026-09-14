# Matriz de testes planejados

Esta é a matriz preparada antes da implementação, em 11/09/2026. Os nomes abaixo foram propostas de organização; alguns cenários foram reunidos nas mesmas classes ou em testes parametrizados. Consulte TDD-EXECUCAO.md para as classes efetivamente criadas e VALIDACAO.md para resultados medidos. A matriz não substitui o relatório de execução.

Os casos devem testar comportamento e limites, evitando testes de getters ou de detalhes privados. Utilizar fixtures pequenas, BigDecimal a partir de strings, Clock fixo e dados independentes. Comparar JSON estruturalmente, sem depender da ordem das propriedades.

**1. Domínio: testes unitários sem Spring**

| ID | Classe | Cenário e expectativa | Regra |
| --- | --- | --- | --- |
| D01 | CouponCodeTest | Seis caracteres alfanuméricos válidos permanecem iguais | RN02 |
| D02 | CouponCodeTest | `ABC-123` resulta em `ABC123` | RN02 |
| D03 | CouponCodeTest | Remover especiais em posições variadas antes de contar | RN02 |
| D04 | CouponCodeTest | Cinco ou sete caracteres após limpeza geram erro; sem truncar/completar | RN02 |
| D05 | CouponCodeTest | Null, vazio e somente especiais geram erro | RN01/RN02 |
| D06 | CouponCodeTest | Minúsculas preservadas; caracteres Unicode seguem política ASCII declarada | Decisão local |
| D07 | CouponCodeTest | Normalizar duas vezes preserva resultado | RN02 |
| D08 | DiscountValueTest | 0,5 e 0,50 válidos, com comparação numérica coerente | RN03 |
| D09 | DiscountValueTest | 0,4999, zero e negativos inválidos | RN03 |
| D10 | DiscountValueTest | 0,5001 e valor muito acima de 100 aceitos sem regra percentual | RN03 |
| D11 | DiscountValueTest | Null rejeitado e valor válido não arredondado | RN01/RN03 |
| D12 | CouponTest | Criação com campos válidos mantém dados e defaults de estado | RN01/RN05 |
| D13 | CouponTest | Descrição nula, vazia/em branco inválida; descrição válida preservada | RN01 |
| D14 | CouponTest | Expiração um instante antes de agora rejeitada | RN04 |
| D15 | CouponTest | Expiração igual e posterior a agora aceitas com Clock fixo | RN04 |
| D16 | CouponTest | Expiração nula rejeitada | RN01 |
| D17 | CouponTest | Pode criar publicado ou não publicado, mantendo ACTIVE e redeemed=false | RN05/decisão |
| D18 | CouponTest | Excluir preserva id e todos os campos de cadastro, altera status/deletedAt | RN07 |
| D19 | CouponTest | Excluir cupom expirado, publicado ou resgatado é permitido | RN06 |
| D20 | CouponTest | Segunda exclusão falha e não altera deletedAt nem dados | RN08 |
| D21 | CouponTest | Reconstituir cupom cuja data agora está no passado continua válido | RN04/RN06 |
| D22 | CouponTest | Reconstituição preserva status e dados; não aceita combinações estruturais inválidas | RN07 |

**2. Aplicação: portas substituídas e regras reais**

| ID | Classe | Cenário e expectativa |
| --- | --- | --- |
| A01 | CreateCouponServiceTest | Usa domínio real, persiste código normalizado e retorna registro criado |
| A02 | CreateCouponServiceTest | Entrada que viola regra não chega à escrita do repositório |
| A03 | CreateCouponServiceTest | Usa um instante consistente da dependência Clock na operação |
| A04 | CreateCouponServiceTest | Duas criações com mesmo código não recebem bloqueio inventado de unicidade |
| A05 | GetCouponServiceTest | Registro existente retorna dados corretos |
| A06 | GetCouponServiceTest | Inexistente e excluído são indisponíveis para consulta normal |
| A07 | GetCouponServiceTest | Expirado não é excluído nem perde capacidade de ser consultado por regra inventada |
| A08 | DeleteCouponServiceTest | Carrega estado incluindo excluídos e efetua transição permitida |
| A09 | DeleteCouponServiceTest | Inexistente e já excluído produzem falhas distintas, sem nova escrita |
| A10 | DeleteCouponServiceTest | Falha de persistência/concorrência não resulta em retorno de sucesso |

Verificações de interação são úteis para provar ausência de escrita e dependências importantes. Não testar a ordem de cada chamada interna sem efeito de negócio.

**3. HTTP, contrato e mapping**

| ID | Classe | Cenário e expectativa |
| --- | --- | --- |
| H01 | CouponControllerTest | POST válido ->201, Location correto e oito campos de resposta |
| H02 | CouponControllerTest | Entrada com especiais retorna e persiste código normalizado |
| H03 | CouponControllerTest | published omitido ->false; explícito false/true preservado; null ->400 |
| H04 | CouponControllerTest | Campos obrigatórios ausentes/null ->400 com detalhe por campo |
| H05 | CouponControllerTest | JSON malformado, tipos errados, data inválida/sem offset ->400 |
| H06 | CouponControllerTest | Regra violada em JSON estruturalmente válido ->422 e código específico |
| H07 | CouponControllerTest | GET válido ->200 e id da resposta igual ao caminho |
| H08 | CouponControllerTest | UUID inválido ->400; UUID inexistente ->404 |
| H09 | CouponControllerTest | DELETE válido ->204 e corpo realmente vazio |
| H10 | CouponControllerTest | GET após exclusão ->404; DELETE repetido ->409 |
| H11 | CouponControllerTest | Valor grande, desconto fracionário e data com offset mantêm significado no JSON |
| H12 | CouponControllerTest | Campos de saída injetados no POST não alteram id/status/redeemed |
| H13 | GlobalExceptionHandlerTest | 400/404/409/422/500 têm status HTTP coerente com body e mesmo padrão |
| H14 | GlobalExceptionHandlerTest | Falha inesperada ->500 sem mensagem SQL, stack trace ou detalhe interno |
| H15 | GlobalExceptionHandlerTest | Método/mídia/Accept inválidos mantêm 405/415/406 e headers aplicáveis |
| H16 | RequestLimitsTest | Corpo acima do limite realmente implementado ->413, incluindo transferência sem Content-Length |
| H17 | MapperTest | Mapeamento preserva campos, datas, valor e default; não executa regras de criação na leitura |
| H18 | OpenApiIT | Swagger e schema publicados refletem requests, responses, erros e Bearer |

Os limites técnicos numéricos e de corpo serão fixados e documentados antes dos testes de fronteira correspondentes. `spring.servlet.multipart.*` não será aceito como prova de limite de JSON. Falhas de parser/representação recebem erro claro, nunca truncamento silencioso.

**4. Persistência real, migrações e concorrência**

| ID | Classe | Cenário e expectativa |
| --- | --- | --- |
| P01 | MigrationIT | Banco H2 vazio recebe migração Flyway e Hibernate valida o schema |
| P02 | MigrationIT | Nova execução do migrate no mesmo banco não destrói dados nem reaplica versão concluída |
| P03 | CouponPersistenceIT | Persistir e reler mantém todos os campos, UUID e estado |
| P04 | CouponPersistenceIT | 0,5, 0,5001, escalas variadas e valores grandes fazem round-trip exato |
| P05 | CouponPersistenceIT | Expiração com offset/subsegundos mantém instante e precisão declarada |
| P06 | CouponPersistenceIT | Soft delete mantém linha e dados; status/deletedAt refletem exclusão |
| P07 | CouponPersistenceIT | Cupom criado no passado do teste e agora expirado pode ser lido/excluído |
| P08 | CouponPersistenceIT | Busca interna reconhece já excluído; consulta normal respeita 404 da camada de aplicação |
| P09 | CouponPersistenceIT | Falha durante escrita provoca rollback e não deixa estado parcial |
| P10 | ConcurrentDeleteIT | Duas transações disputam a mesma versão: só uma alteração é confirmada; outra falha de forma controlada |
| P11 | ConcurrentDeleteIT | Ao repetir após a disputa, registro continua preservado e não recebe outra data de exclusão |
| P12 | CouponPersistenceIT | Código repetido pode coexistir com UUID diferente, conforme decisão de ausência de unicidade |
| P13 | CouponPersistenceIT | Constraints estruturais rejeitam dados inválidos em escrita direta, sem regra temporal dinâmica |

Para P10 usar conexões/transações independentes, coordenação com barreira/latch e timeout. Evitar `Thread.sleep` e teste inteiro dentro de uma transação compartilhada. Ler o estado final em transação nova. Um mock do repositório não prova proteção contra concorrência.

**5. Segurança, filtros e logs**

| ID | Classe | Cenário e expectativa |
| --- | --- | --- |
| S01 | TokenServiceTest | Credenciais válidas produzem token com emissor, audiência, subject, expiração e scopes atribuídos pelo servidor |
| S02 | TokenServiceTest | Credenciais inválidas não emitem token; cliente não consegue escolher permissões |
| S03 | JwtSecurityIT | Requisição sem Bearer ->401 com header apropriado |
| S04 | JwtSecurityIT | Token expirado, assinatura adulterada, algoritmo indevido, issuer/audience errados ->401 |
| S05 | JwtSecurityIT | Token válido com leitura acessa GET; escrita exige permissão correspondente |
| S06 | JwtSecurityIT | Token válido sem permissão ->403, distinto de falha de autenticação |
| S07 | SecurityErrorContractTest | 401/403 usam ProblemDetail e correlationId, mesmo sem entrar no controller |
| S08 | TokenRateLimitTest | Excesso no login ->429; janela/limite testados com tempo controlado |
| S09 | SecurityConfigurationIT | Fora de demo, chaves/credenciais obrigatórias ausentes não geram configuração insegura silenciosa |
| S10 | CorrelationIdFilterTest | Gera id ausente, propaga id válido e trata id excessivo/inválido sem injeção em log |
| S11 | CorrelationIdFilterTest | Limpa MDC em sucesso e falha; id não vaza para outra requisição |
| S12 | LogSanitizationTest | Login/erro não registram senha, Bearer, corpo completo ou descrição do cupom |
| S13 | LogSanitizationTest | Evento de sucesso não é emitido se transação falha |
| S14 | SecurityConfigurationIT | Sessão stateless, Swagger/health conforme política e sem console H2 aberto por acidente |

JWT deve ser assinado e passar pelo decoder real em parte da suíte. Testes com autenticação simulada são úteis para autorização, mas insuficientes para S04.

**6. Arquitetura e entrega executável**

| ID | Classe/etapa | Cenário e expectativa |
| --- | --- | --- |
| Q01 | ArchitectureTest | Domínio sem Spring, JPA, Jackson ou Servlet |
| Q02 | ArchitectureTest | Aplicação depende do domínio e de suas portas, não de adapters/frameworks |
| Q03 | ArchitectureTest | Controller não acessa repositório JPA diretamente; entidade não sai pela API |
| Q04 | JaCoCo | >=90% linhas/branches em domínio+aplicação e >=80% linhas no código autoral total |
| Q05 | Smoke JAR | Processo real inicia, fica healthy e atende ciclo completo autenticado |
| Q06 | Smoke Docker | Imagem builda, Compose fica healthy e o mesmo ciclo funciona |
| Q07 | Revisão operacional | Logs de startup, sucesso e falha analisados; problemas encontrados corrigidos |
| Q08 | CI/PR | Checks pertencem ao commit final da branch, passam e relatórios estão acessíveis |
| Q09 | Merge | Main remota contém a entrega e sua validação final é confirmada |
| Q10 | Mutação opcional | Alterações nas comparações e remoção de guardas são detectadas pelos testes |

**7. Registro de execução a preencher durante a implementação**

| Ciclo | Cenário(s) | Teste escrito antes | Falha observada | Implementação/refatoração | Comando e resultado | Commit verde |
| --- | --- | --- | --- | --- | --- | --- |
| A iniciar | - | - | - | - | - | - |

Manter evidência suficiente para rastrear o trabalho, sem versionar logs contendo credenciais nem afirmar que um teste planejado já passou. A matriz pode agrupar testes parametrizados; a quantidade de linhas não deve ser anunciada como quantidade de testes executados.
