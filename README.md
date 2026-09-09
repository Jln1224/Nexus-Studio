# Nexus Studio
Bem-vindos ao centro de documentação da Nexus Studio. Este projeto foca no desenvolvimento de um ecossistema robusto para gestão e catalogação de hardware de alta performance. Através da união entre a robustez da Programação Orientada a Objetos em Java e a agilidade do SQLite, construímos uma solução capaz de gerenciar especificações técnicas detalhadas, compatibilidade de sockets e eficiência energética.

## Executar a interface JavaFX

Com Java 17+ e Maven instalados, execute:

```bash
mvn javafx:run
```

A interface está em português e possui abas para catálogo (com painel de detalhes e imagem), cadastro, relatórios, compatibilidade, histórico de preços, estoque e montagem de PC. Relatórios filtrados são exportados para `Desktop/nexus-relatorio-AAAA-MM-DD.pdf`; configurações montadas usam `Desktop/nexus-configuracao-AAAA-MM-DD.pdf`. O banco `nexus_studio.db` é inicializado automaticamente na primeira execução, incluindo as tabelas de histórico de preços e inventário.

O menu de console continua disponível pela classe `Main`.
