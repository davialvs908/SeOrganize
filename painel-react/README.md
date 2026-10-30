# Painel de Salas (React)

Front-end em React que mostra, em tempo real, a ocupação das salas do SeOrganize.
Consome a API REST do back-end Spring Boot e se atualiza sozinho a cada 30 segundos.

## O que tem

- Resumo com salas livres, salas em uso e reservas restantes
- Cartão por sala com status (Livre / Em uso), quem está usando e a próxima reserva
- Filtro por status e busca por nome, andar ou recurso
- Agenda com as próximas reuniões
- Layout responsivo (desktop e celular)
- Redireciona para o login quando a sessão expira (API responde 401)

## Stack

React 18 · Vite · JavaScript · Vitest · Testing Library

## API consumida

| Método | Rota                  | Retorno                                                    |
|--------|-----------------------|------------------------------------------------------------|
| GET    | `/api/painel/me`      | Nome, perfil e iniciais do usuário logado                  |
| GET    | `/api/painel/salas`   | Salas disponíveis com status atual e próxima reserva       |
| GET    | `/api/painel/ocupacao`| Reservas ativas (em andamento e futuras), ordenadas        |

A API usa a mesma sessão do SeOrganize: só quem está logado consegue acessar.

## Como rodar

```bash
cd painel-react
npm install

npm run dev:mock   # roda com dados de exemplo, sem precisar do back-end
npm run dev        # usa a API real; o Vite repassa /api para o Spring em :8080
npm test           # testes com Vitest e Testing Library
npm run build      # gera o build dentro do Spring Boot (static/painel)
```

Com o Spring rodando, o painel fica em `http://localhost:8080/painel` depois do login.
