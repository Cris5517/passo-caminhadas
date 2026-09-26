# Passo

Aplicativo web para registrar caminhadas com GPS, distância, duração e ritmo médio. Requer Java 17.

## Executar

Na pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Execute os testes com:

```powershell
.\mvnw.cmd test
```

Abra `http://localhost:8080` depois de iniciar a aplicação.

## Caminhadas

O navegador pede permissão para acessar o GPS ao iniciar uma sessão. O mapa usa dados do OpenStreetMap.

| Método | Rota | Resultado |
| --- | --- | --- |
| `POST` | `/api/walks` | Inicia uma caminhada |
| `GET` | `/api/walks/active` | Recupera uma sessão em andamento |
| `GET` | `/api/walks` | Lista caminhadas |
| `PUT` | `/api/walks/{id}/progress` | Atualiza distância e duração |
| `POST` | `/api/walks/{id}/pause` | Pausa a caminhada |
| `POST` | `/api/walks/{id}/resume` | Retoma uma caminhada pausada |
| `POST` | `/api/walks/{id}/finish` | Finaliza e salva a sessão |
| `DELETE` | `/api/walks/{id}` | Exclui uma caminhada |

## Tarefas

Os endpoints existentes de tarefas continuam disponíveis no prefixo `/api/tasks`.

| Método | Rota | Resultado |
| --- | --- | --- |
| `POST` | `/api/tasks` | Cria tarefa e retorna `201 Created` |
| `GET` | `/api/tasks` | Lista tarefas |
| `GET` | `/api/tasks/{id}` | Busca uma tarefa |
| `PUT` | `/api/tasks/{id}` | Substitui os dados da tarefa |
| `DELETE` | `/api/tasks/{id}` | Exclui tarefa e retorna `204 No Content` |

Exemplo de corpo para criação e atualização:

```json
{
	"title": "Estudar Spring",
	"description": "Revisar JPA e validação",
	"completed": false
}
```

O título é obrigatório e aceita até 120 caracteres. A descrição aceita até 2000 caracteres. Respostas inválidas retornam `400 Bad Request`; IDs inexistentes retornam `404 Not Found`.

## Banco de dados

Os dados são guardados em um arquivo H2 local (`taskdb.mv.db`) e permanecem após reiniciar a aplicação. O arquivo é ignorado pelo Git. O console H2 fica disponível em `/h2-console`, com JDBC URL `jdbc:h2:file:./taskdb`, usuário `sa` e senha vazia.

