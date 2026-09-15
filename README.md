# SeOrganize

Sistema interno para o dia a dia da operação: solicitações no Kanban, reserva de salas, fila de ligações e um painel de gestão para quem administra.

Feito para recepção e equipe de TI no mesmo lugar, com login por sessão e perfis de acesso.

## O que o sistema cobre

- **Kanban** de solicitações, com comentários, arquivo e exportação de relatório em Excel (administrador)
- **Reservas de salas**, com ocupação visível e cancelamento pelo solicitante
- **Fila de ligações** para a recepção, com histórico do dia
- **Cadastros** de usuários, salas e contatos da agenda
- **Perfil** com troca de senha e sessão invalidada depois da alteração

## Stack

Java 17 · Spring Boot 3 · Thymeleaf · Spring Security · JPA · PostgreSQL (produção) · H2 (desenvolvimento local)

## Como rodar localmente

Na pasta `Sla_Reuniao/Sla_Reuniao`:

```bash
set SPRING_PROFILES_ACTIVE=dev,local
mvnw.cmd spring-boot:run
```

Abra [http://localhost:8080](http://localhost:8080).

Com o perfil `local`, o banco é H2 em memória. Usuários de exemplo:

| Perfil         | E-mail        | Senha     |
|----------------|---------------|-----------|
| Administrador  | admin@local   | admin1234 |
| Usuário        | ana@local     | admin1234 |
| Recepção       | carla@local   | admin1234 |

Esses acessos existem só no ambiente local. Em produção o primeiro administrador nasce pelas variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD`.

## Produção

O deploy previsto é no Render (`render.yaml`), com PostgreSQL e o perfil `prod`. A aplicação escuta a porta `PORT` e lê `DATABASE_URL`.
