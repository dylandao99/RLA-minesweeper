# RLA-minesweeper
## Reinforcement Machine Learning Minesweeper Algorithm

### Analysis Paper: https://drive.google.com/file/d/0B-PEhlj3kw_bdFdBVHNDSkJndU0/view

A Reinforcement Machine Learning Minesweeper Algorithm written in Java.
It learns Minesweeper by *failing repeatedly and learning from its mistakes.*
It scans a Minesweeper board for subsets of numbers, generates all possible mine configurations, then tests them upon future encounters at it plays. Depending on success or failure, the algorithm sorts mine configuration theories into rules that it follows to play better. 

## Features
* Fully-featured Minesweeper game
  * Custom Minesweeper boards of any size
* First click optimization
* General Play performance improvement as it plays
  * Deduce mine patterns with certainty
  * Probability analysis for non-certain scenarios
* Variables that change the algorithm's accuracy
* .csv game data export

### Algorithm Initial Knowledge
* Scan board for subsets of numbers, mines, and flags
* Minesweeper is won by clearing all non-mine cells
  * More cells cleared = better performance

### Minesweeper Rules and Specifications
* First move is never a mine
* 3x3 box around first click is always clear

### Planned Features
* Probability analysis
* Save learned rules after program closes
