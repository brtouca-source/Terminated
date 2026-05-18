MUNDO MÁGICO ADM PWA AVANÇADO - @toucabr

Arquivos:
- index.html
- manifest.json
- sw.js
- icons/

Como usar:
1. Suba estes arquivos em um repositório separado do jogo.
2. Ative GitHub Pages/Vercel.
3. No Firebase Authentication > Settings > Authorized domains, adicione o domínio do painel.
4. Garanta que existe em Realtime Database:
   admins/SEU_UID_GOOGLE = true

Funções:
- Dashboard
- Salas
- Jogadores
- Manutenção
- Banidos
- Logs
- Backup JSON
- Instalação como PWA

Observação:
Sem mexer no jogo, algumas funções como banimento não bloqueiam o jogador dentro do game.js.
Elas registram e permitem moderação via painel. Para banimento ativo no jogo, o game.js teria que ler /bans.
