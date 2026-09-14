# Evidências de execução TDD

Registro iniciado em 11/09/2026. Logs completos locais ficam em tmp e não são publicados.

| Ciclo | RED observado antes da implementação | GREEN | Evidência |
| --- | --- | --- | --- |
| Domínio | Compilação sem tipos; após assinaturas: 35 testes, 23 falhas de asserção e 6 erros de operações ainda não implementadas | 35 testes passando | CouponCodeTest, DiscountValueTest, CouponTest; logs domain-red-compile, domain-red e domain-green |
| Casos de uso | Compilação sem tipos; após assinaturas: 11 testes, 7 falhas e 4 erros de operações ainda não implementadas | 11 testes passando | CouponServicesTest; application-red e application-red-assert |

Os ciclos seguintes também começaram pelos testes, antes das classes correspondentes. Falhas de compilação/contexto direcionaram os adapters e as configurações; o build integrado passou após a implementação.

| Responsabilidade | Classes de teste implementadas |
| --- | --- |
| Persistência, migração, concorrência e precisão | CouponPersistenceIT |
| Contrato HTTP, validação e erros | CouponControllerTest |
| Commit, rollback e sucesso após commit | TransactionBoundaryTest |
| Correlação e corpo limitado, inclusive sem Content-Length | RequestFiltersTest |
| Assinatura/emissão, claims, senha, permissões e login | TokenServiceTest, ApiSecurityIT, SecurityConfigurationTest |
| Limite de tentativas | LoginAttemptLimiterTest, TokenRateLimitIT |
| Geração/carregamento de chaves | RsaKeyProviderTest |
| Independência do domínio e aplicação | ArchitectureTest |
| Detecção de vazamento e erros nos logs | scripts/test_check_logs.py |

O verificador de logs teve dois testes falhando antes da correção das expressões regulares. A documentação Swagger ganhou primeiro uma asserção HTTP real que falhou por ausência de ApiProblem; depois foi implementado o schema e o mapeamento dos erros.

O histórico Git mantém etapas de entrega; nem toda passagem RED foi publicada como commit quebrado. Os relatórios finais e os gates do CI demonstram o estado executável entregue. Testes parametrizados contam cada entrada como uma execução; a matriz de cenários não deve ser somada como se fosse a quantidade de métodos.

O scaffold veio do Spring Initializr. Corrigido o parent gerado de 4.1.1.RELEASE para 4.1.1, cuja publicação foi confirmada no Maven Central. Nenhuma regra foi implementada no scaffold.
