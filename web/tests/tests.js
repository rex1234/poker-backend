function determineWinnings(players) {
    //sort players by currentBet

    const result = players.sort(function (a, b) {
        return a[0] > b[0] ? 1 : -1;
    });

    //players[players.length-1] -= players[players.length-1] - players[players.length-2];

    //get sum of the chips
    let pot = 0;
    for (let i = 0; i < result.length; i++) {
        pot += result[i][0];
    }

    let remainder = 0;
    let taken = 0;

    for (let i = 0; i < result.length - 1; i++) {
        let splitting = 1;
        for (let k = i + 1; k < result.length; k++) {

            //lost
            if (result[i][1] < result[k][1]) {
                remainder += result[i][0];
                result[i][0] = 0;
                break;
            }

            //splitted on the way
            if (result[i][1] === result[k][1]) {
                splitting++;
            }

            //not lost
            if (k === result.length - 1) {
                let winnings = (result[i][0] - taken) * (result.length - i) + remainder;
                taken = result[i][0];
                winnings /= splitting;
                result[i][0] = winnings;
                remainder = 0;
                if (splitting > 1) {
                    remainder = winnings * (splitting - 1);
                }
                splitting = 0;

                pot -= winnings;
            }
        }
    }

    result[result.length - 1][0] = pot;

    console.log(result);
}

determineWinnings([
    //currentBet
    //HandRank -- the higher, the better; 0 = folded
    [300, 7, 'Adam'],
    [200, 10, 'Herbert'],
    [150, 10, 'Bretislav'],
    [20, 0, 'Picka'],
    [40, 3, 'Kukatko']
]);

determineWinnings([
    [300, 7, 'Adam'],
    [200, 10, 'Herbert'],
]);

determineWinnings([
    [300, 7, 'Adam'],
    [200, 7, 'Herbert'],
    [150, 12, 'Bretislav'],
    [20, 13, 'Picka'],
    [70, 12, 'Kukatko']
]);

determineWinnings([
    [300, 0, 'Adam'],
    [200, 10, 'Herbert'],
]);

determineWinnings([
    [300, 0, 'Adam'],
    [200, 10, 'Herbert'],
    [99, 10, 'John'],
]);
