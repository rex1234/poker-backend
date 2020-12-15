package io.pokr.game.tools

object WinningsCalculator {

    // TODO: add remaining chip to SB / BB (in case of 1 chip split)
    fun calculateWinnings(results_: List<HandComparator.PlayerHandComparisonResult>) {
        results_.map { it.player }.forEach {
            it.chips -= it.currentBet
        }

        val results = results_.sortedBy { it.player.currentBet }
        var pot = results.sumBy { it.player.currentBet }

        var remainder = 0
        var taken = 0

        for (i in 0 until results.size) {
            var splitting = 1

            for (k in i + 1 until results.size) {

                // lost
                if (results[i].rank > results[k].rank) {
                    remainder += results[i].player.currentBet
                    results[i].winnings = 0
                    break
                }

                // split on the way
                if (results[i].rank == results[k].rank) {
                    splitting++
                }

                // not lost
                if (k == results.size - 1) {
                    var winnings = (results[i].player.currentBet - taken) * (results.size - i) + remainder
                    taken = results[i].player.currentBet
                    winnings /= splitting
                    results[i].winnings = winnings
                    remainder = 0
                    if (splitting > 1) {
                        remainder = winnings * (splitting - 1);
                    }
                    splitting = 0

                    pot -= winnings
                }
            }
        }

        results.last().winnings = pot

        results.forEach {
            it.player.lastWin = it.winnings
            it.player.chips += it.winnings
        }
    }
}
