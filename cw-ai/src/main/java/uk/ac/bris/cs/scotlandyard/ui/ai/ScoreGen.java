package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.Arrays;
import java.util.List;

public class ScoreGen {

    private Integer score;
    private Node node;
    private Integer[][] shortestPaths;

    public ScoreGen(Integer[][] shortestPaths) {
        this.shortestPaths = shortestPaths;
    }

    // Calculate node score based on mrX distance to detectives/ferry
    public void setScore(Node node) {
        this.node = node;
        score = 0;
        if (!winOrLost()) {
            if (node.availableMoves.asList().get(0).commencedBy().isMrX() && node.mrXTurn) {
                for (Move move : node.availableMoves) if (move.commencedBy().isMrX()) score += 2;
            }
            detDistances();
            disToFerry();
        }
        node.score += score;
    }

    // Set score to high value if move will result in win, lowest value if mrX will lose
    private boolean winOrLost() {
        if (!node.winner.isEmpty()) {
            if (node.winner.asList().get(0).isMrX() ) score = 10000000;
            else score = -10000000;
            return true;
        }
        return false;
    }

    // Calculate distance from detectives, adjust score accordingly
    private void detDistances() {
        for (Integer detPos : node.playerLocations) {
            score += (pow(4,shortestPaths[node.mrxPos][detPos])/5);
        }
    }

    // Calculate distance from ferry, adjust score accordingly
    private void disToFerry() {
        List<Integer> places = Arrays.asList(108,115,157,194);
        for (Integer ferryLoc : places) score -= (pow(2,shortestPaths[node.mrxPos][ferryLoc])) /10;
    }

    // Helper function to computer power
    private Integer pow(Integer a,Integer b) {
        Integer total = 1;
        for (int i = 0;i < b;i++ ) total *= a;
        return total;
    }
}
