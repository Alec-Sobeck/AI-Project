# CS4725
CS4725 (Intro to AI) Project

This agent is designed specifically to play a modified version of the game Quarto (see: https://en.wikipedia.org/wiki/Quarto_(board_game)).

Instead of the standard game, which is played on a 4x4 board with 16 pieces, this version of the game is played on a 5x5 board with 32 pieces (each piece has an additional addtribute, I believe we called it 'wood' or 'metal'). This causes the search space to be much larger than for the standard game, as the branching factor becomes 32, instead of 16.

The agent itself is based on a highly optimized Monte Carlo Tree Search (MCTS) with Upper Confidence Bound for Trees (UCT). These optimizations helped a lot because the agent was able to explore a lot of the search space in the maximum allowed time.

Out of the 30 agents written by myself and others in the class, this agent placed #1, and was undefeated in something like 100 games against other people's agents.

