# AgrupamentoDeVeiculosInteligentes
Trata-se de um repositório para material de apoio ao artigo "Uma Nova Metodologia para formação de Grupos em Redes Interveiculares". Este repositório contém todos aqueles arquivos que não puderam fazer parte do referido artigo devido à natureza do conteúdo e o excesso de material.

# Legendas do Vídeo
As cores dos carros presentes no vídeo, representam um dos estados possíveis na máquina de estados que se encontra no artigo referência ou na figura states.png contida neste repositório. A seguir, se encontra o mapeamento das cores nos estados:
  - Branca: Estado SOLE
  - Amarela: Estado LEADER
  - Laranja: Estado WAITING
  - Preta: Estado ASP
  - Demais cores (Magenta, Azul, Ciano, Cinza, Cinza Escuro, Cinza Claro, Verde, Rosa e Vermelho): Estado MEMBER. Cabendo ressaltar que as cores aqui presentes são aquelas padrões da biblioteca java.awt.Color. Ademais, quando um grupo é criado, o líder do mesmo define aleatoriamente a cor do mesmo, facilitando a identificação de grupos no vídeo.

# Códigos
- VehicleUnit: Implementação de todo o algoritmo e protocolo distribuído a ser executado dentro de cada veículo inserido na simulação com o simulador VSimRTI.
- SimCounter: Código utilizado para realização do parser de arquivos de logs criados pelos veículos na simulação e contagem de grupos formados para criação dos datasets.
