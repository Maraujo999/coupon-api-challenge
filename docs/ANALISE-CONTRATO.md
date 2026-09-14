# Análise do contrato da Coupon API

Análise realizada em 11/09/2026. Este arquivo registra evidências e decisões propostas antes da implementação. Não é um relatório de testes executados contra produção.

**1. Fontes e alcance da análise**

Foram lidas as três páginas do PDF `Desafio técnico _ Notion.pdf`, as três imagens fornecidas e as operações do [Apidog](https://n1m0i5k0zu.apidog.io/). As imagens complementam comentários cortados na impressão: desconto é valor absoluto, sem preocupação com moeda; objeto de domínio é diferente de entidade JPA.

Os arquivos Markdown indicados pelo [índice oficial para LLMs](https://n1m0i5k0zu.apidog.io/llms.txt) expõem a especificação OpenAPI de cada operação. Eles foram consultados para verificar campos, obrigatoriedade, valores padrão e enumerações que a página visual não mostra integralmente.

O OpenAPI consultado contém `servers: []`, `security: []` e nenhuma definição de esquema de autenticação. As operações estão marcadas como em desenvolvimento. Não há base de produção identificada nesses documentos. Nenhuma requisição de criação ou exclusão foi enviada a um sistema remoto.

Portanto, respostas reais de erro, latência, headers de produção e regras não documentadas continuam desconhecidos. Uma URL de execução eventualmente fornecida deve ser analisada separadamente; os exemplos do Apidog não são prova de comportamento em execução.

**2. Contrato de sucesso observado**

| Operação | Entrada | Sucesso documentado | Corpo |
| --- | --- | --- | --- |
| `POST /coupon` | JSON de criação | `201` | Cupom criado |
| `GET /coupon/{id}` | Identificador descrito como UUID | `200` | Cupom consultado |
| `DELETE /coupon/{id}` | Identificador descrito como UUID | `204` | Sem corpo |

Referências: [criação](https://n1m0i5k0zu.apidog.io/coupon-23755524e0.md), [consulta](https://n1m0i5k0zu.apidog.io/couponid-23762372e0.md), [exclusão](https://n1m0i5k0zu.apidog.io/couponid-23762429e0.md).

| Campo de entrada | Tipo documentado | Obrigatório | Observação |
| --- | --- | --- | --- |
| `code` | string | Sim | Normalizar antes de validar tamanho final |
| `description` | string | Sim | Não há tamanho máximo especificado |
| `discountValue` | number | Sim | Mínimo 0,5 vem do enunciado |
| `expirationDate` | string | Sim | Exemplos usam data e hora UTC; schema não declara `format` |
| `published` | boolean | Não | Default documentado: `false` |

A resposta possui oito campos obrigatórios: `id`, `code`, `description`, `discountValue`, `expirationDate`, `status`, `published` e `redeemed`. `status` admite `ACTIVE`, `INACTIVE` e `DELETED`; `redeemed` tem default `false`. O objeto é retornado diretamente, sem envelope `data`. Esses detalhes vêm dos schemas de [POST](https://n1m0i5k0zu.apidog.io/coupon-23755524e0.md) e [GET](https://n1m0i5k0zu.apidog.io/couponid-23762372e0.md).

**3. Divergências e cuidados**

| Evidência | Risco de copiar literalmente | Tratamento proposto |
| --- | --- | --- |
| Página DELETE mostra JSON de exemplo, mas declara 204 sem corpo; OpenAPI não define conteúdo | Resposta HTTP incoerente | Implementar 204 com zero bytes de corpo |
| Exemplos de criação usam datas de 2025 | Exemplo já viola regra de data passada | Exemplos executáveis geram data futura dinamicamente |
| Descrição e data do exemplo de resposta POST diferem da entrada | Alterar dados sem regra de negócio | Preservar descrição, valor e instante informado; só normalizar código e representação de timezone |
| UUID do exemplo GET difere do UUID do caminho | Consultar registro errado | Garantir resposta com o mesmo identificador solicitado |
| POST apresenta `ACTIVE` com `published=false` | Inferir que todo não publicado é inativo | Tratar publicação e status separadamente |
| Enum inclui `INACTIVE`, sem operação ou regra de transição | Criar regra nova por suposição | Reservar o valor sem inventar transição automática |
| Ausência de respostas de erro | Afirmar que 400/409/422 foram observados | Apresentar como contrato complementar projetado |
| Ausência de autenticação no OpenAPI | Apresentar JWT como requisito original | Documentar como extensão solicitada pelo candidato |

As divergências de exemplos foram observadas nas páginas de [POST](https://n1m0i5k0zu.apidog.io/), [GET](https://n1m0i5k0zu.apidog.io/couponid-23762372e0) e [DELETE](https://n1m0i5k0zu.apidog.io/couponid-23762429e0). A decisão sobre 204 também segue a [semântica HTTP](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.3.5).

**4. Regras confirmadas pelo enunciado**

| ID | Regra | Consequência no projeto |
| --- | --- | --- |
| RN01 | Quatro campos obrigatórios na criação | Validar entrada e proteger construção do domínio |
| RN02 | Código alfanumérico com seis caracteres após retirar especiais | Value object normaliza e valida; persistência recebe forma normalizada |
| RN03 | Desconto absoluto >= 0,5, sem máximo de negócio | BigDecimal; sem limite percentual ou arredondamento monetário |
| RN04 | Expiração não pode estar no passado na criação | Comparação com um instante controlável no teste |
| RN05 | Pode criar publicado | Aceitar ambos os valores de `published` |
| RN06 | Pode excluir a qualquer momento | Publicação, expiração e resgate não criam bloqueios adicionais |
| RN07 | Exclusão lógica preserva dados do cadastro | Alterar estado e metadados de exclusão, mantendo a linha |
| RN08 | Não excluir novamente o já excluído | Estado terminal protegido no domínio e contra concorrência |
| RN09 | Regras em objetos de domínio, distintos de entidade JPA | Domínio sem anotações de frameworks |
| RN10 | Pleno: testes >=80%, H2, GitHub público, Docker/Compose e Swagger | Entregáveis e critérios objetivos de conclusão |

**5. Decisões propostas para lacunas, sujeitas a revisão se surgir evidência**

| Tema | Decisão de trabalho | Justificativa |
| --- | --- | --- |
| Alfabeto do código | Letras ASCII e dígitos; retirar demais caracteres; preservar maiúsculas/minúsculas | Regra determinística, sem transformação de caixa não solicitada |
| Tamanho após limpeza | Rejeitar se diferente de seis | Truncar ou completar criaria outro código |
| Unicidade de código | Não impor nesta entrega | Não consta no enunciado; UUID identifica o recurso. Documentar como questão de produto |
| Descrição vazia | Rejeitar apenas vazia/em branco; preservar conteúdo válido | Obrigatoriedade deve ter significado; não cortar texto silenciosamente |
| `published` ausente | Assumir false | Default consta no schema |
| `published: null` | Rejeitar como entrada inválida | Quando presente, o schema pede boolean; não equiparar null a ausência |
| Datas | Receber ISO-8601 com offset, comparar como Instant e retornar UTC | Não depender do fuso da máquina |
| Expiração igual ao instante de criação | Aceitar no relógio do teste | A regra proíbe passado, não exige estritamente futuro |
| Estado inicial | `ACTIVE`, independentemente de `published`; `redeemed=false` | Compatível com exemplo; não há criação de cupom resgatado |
| Expiração após cadastro | Não mudar status automaticamente | Não há regra publicada para `ACTIVE -> INACTIVE`; expiração continua disponível no objeto |
| GET de excluído | 404; banco continua preservado | Retira o recurso da consulta normal. É decisão local, não resultado observado em PRD |
| DELETE repetido | 409 com código de erro específico | Preserva a proibição explícita do enunciado |
| Consulta interna para DELETE | Incluir registros excluídos | Permite distinguir inexistente de já excluído |
| Status, id e redeemed enviados no POST | Não permitir que controlem o domínio | DTO de criação só contém campos de entrada; campos desconhecidos não são vinculados |
| JWT | Exigido nos três endpoints; emissão de token de demonstração documentada | Pedido expresso do usuário; muda a exigência de acesso em relação ao OpenAPI de origem |
| Limites técnicos de entrada | Definidos na implementação com testes de fronteira, sem apresentá-los como regras comerciais | Memória, parser e banco são finitos; não prometer números infinitos |

A checagem de `published: null` deverá ser feita no binding/validação HTTP, preservando o default de ausência. Não usar um `Boolean` seguido de `null -> false` se isso impedir distinguir as duas situações. O teste precede a configuração de desserialização.

A segunda resposta DELETE ser 409 não contradiz a idempotência HTTP: a repetição não causa uma segunda alteração no recurso, e a resposta pode diferir. Essa é uma interpretação apoiada na [definição de métodos idempotentes](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.2).

**6. Contrato complementar de erros proposto**

| Situação | HTTP | Código estável proposto |
| --- | --- | --- |
| JSON inválido, campo obrigatório ausente/null, tipo errado, UUID inválido, data sem offset | 400 | `INVALID_REQUEST` |
| Código inválido após normalização | 422 | `INVALID_COUPON_CODE` |
| Descrição vazia/em branco | 422 | `INVALID_COUPON_DESCRIPTION` |
| Desconto abaixo do mínimo | 422 | `INVALID_DISCOUNT_VALUE` |
| Expiração no passado | 422 | `EXPIRATION_IN_PAST` |
| Registro inexistente ou excluído em consulta normal | 404 | `COUPON_NOT_FOUND` |
| Exclusão de registro já excluído | 409 | `COUPON_ALREADY_DELETED` |
| Escrita concorrente perde disputa de versão | 409 | `COUPON_CONCURRENT_MODIFICATION` |
| Bearer ausente/inválido/expirado ou credenciais inválidas na emissão | 401 | `UNAUTHORIZED` |
| Identidade válida sem permissão para operação | 403 | `FORBIDDEN` |
| Tentativas excessivas no endpoint de emissão de token | 429 | `TOO_MANY_REQUESTS` |
| Método não suportado | 405 | `METHOD_NOT_ALLOWED` |
| Tipo de mídia não suportado | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| Accept sem representação suportada | 406 | `NOT_ACCEPTABLE` |
| Limite técnico de corpo efetivamente implementado excedido | 413 | `PAYLOAD_TOO_LARGE` |
| Falha inesperada | 500 | `INTERNAL_ERROR` |

Esses erros não foram especificados pelo Apidog. A distinção proposta é: 400 para interpretação/estrutura; 422 para conteúdo estruturado que viola o negócio; 409 para estado conflitante. Não transformar toda exceção em 400, nem toda falha de banco em 409. Headers como `Allow` e `WWW-Authenticate` devem ser preservados nos casos aplicáveis, conforme [HTTP](https://www.rfc-editor.org/rfc/rfc9110.html).

Formato planejado: `application/problem+json`, com `type`, `title`, `status`, `detail`, `instance`, `code`, `correlationId` e, quando aplicável, `errors` contendo campo e código. Usar `ProblemDetail` como base e records pequenos para detalhes de validação; não devolver stack trace nem mensagens SQL. O Spring oferece suporte nativo a esse formato: [Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).

**7. Pendência externa e tratamento**

A URL base de uma API em execução não foi localizada no material. Isso não impede implementar e testar o contrato local. Se for fornecida, registrar método, caminho, horário, status, headers relevantes e corpo sanitizado de consultas permitidas. Operações de escrita remota só fazem sentido em ambiente de testes identificado, com dados de teste e escopo autorizado. Não chamar uma página HTML do Apidog de endpoint de negócio.
