# Validação da implementação

Registro da implementação em 11/09/2026. Este arquivo distingue execução local e validação no CI.

## Resultado local

| Verificação | Resultado |
| --- | --- |
| Surefire: unitários, HTTP, arquitetura e configurações | 88 execuções; 0 falhas, erros ou ignorados |
| Failsafe: persistência, segurança e limite de login | 23 execuções; 0 falhas, erros ou ignorados |
| Verificador de logs (Python) | 2 testes passando |
| JaCoCo: linhas do projeto | 542/553, ou 98,01% |
| JaCoCo: branches do projeto | 102/114, ou 89,47% |
| Domínio e aplicação | 100% de linhas e branches mensuráveis |
| JAR iniciado em processo próprio | 16 verificações HTTP aprovadas |
| Logs da execução HTTP | 0 ERROR, 3 WARN analisados, nenhum segredo/marcador detectado |

Os números Java incluem cada entrada de testes parametrizados. O relatório de cobertura exclui somente implementações geradas pelo MapStruct. As métricas incluem unitários e integração.

## Verificações

- Unitários e testes de integração: Maven Surefire/Failsafe, com gates de cobertura e formatação aprovados.
- Banco real do desafio: H2 em memória com Flyway, validação do schema, decimais grandes/fracionários, precisão temporal, rollback, exclusão lógica e concorrência.
- Segurança: assinatura e claims JWT, 401/403, expiração, token sem assinatura, login e limites.
- HTTP real: 16 verificações contra o JAR, incluindo ciclo completo e respostas inválidas.
- Logs: inspeção automatizada de ERROR, JWT, senha e marcador do corpo de requisição; eventos de criação, exclusão e HTTP obrigatórios.
- PIT no Linux: 29 mutantes gerados, 29 detectados (100%); nenhum sobrevivente ou erro de execução. O alvo são os objetos de domínio e serviços de aplicação.
- CI Linux: a cobertura reproduziu os mesmos 542/553 linhas e 102/114 branches medidos localmente.
- Docker/Compose: imagem construída no Linux, container saudável, 16 verificações HTTP aprovadas e inspeção dos logs aprovada (0 ERROR, 3 WARN conhecidos, nenhum segredo/marcador detectado).

Evidências reproduzíveis: [workflow de validação](https://github.com/Maraujo999/coupon-api-challenge/actions/workflows/ci.yml). O artifact `validation-reports` contém JaCoCo, Surefire, Failsafe, PIT e o resultado sanitizado do JAR. Artifacts têm retenção de sete dias; os comandos do README reproduzem os relatórios.

## Limitações observadas no ambiente local

O Docker Desktop local falhou ao inicializar o listener de inferência em um socket no AppData. Nenhum dado de container foi removido. A execução do Compose foi comprovada separadamente no CI Linux.

A JVM Windows falhou ao abrir um socket temporário sob o AppData durante a inicialização do Tomcat. O helper local aplica `-Djdk.net.unixdomain.tmpdir` ao diretório `tmp` do projeto. Com isso, o JAR iniciou e respondeu às verificações HTTP.

O PIT iniciou no Windows, mas o processo filho não carregou classes de teste no caminho com espaços/acentos. A verificação de mutação foi transferida para o checkout Linux do CI; uma execução abortada não foi contada como sucesso.

## Avisos de inicialização analisados

A execução do JAR emitiu três WARN conhecidos: Flyway informa que o H2 gerenciado pelo Boot é mais novo que sua versão testada; springdoc informa que a documentação JSON e o Swagger UI estão habilitados.

As migrações e o schema foram exercitados com o H2 efetivamente usado. Swagger é um requisito do desafio e fica acessível na demonstração. Os avisos foram mantidos, sem mudar níveis de log para ocultá-los.

Nenhum endpoint de produção foi identificado no Apidog. Todos os testes HTTP reportados se referem à aplicação implementada ou ao container do CI.
