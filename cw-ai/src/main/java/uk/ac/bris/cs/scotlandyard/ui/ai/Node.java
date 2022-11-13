package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import java.util.ArrayList;
import java.util.Collections;

public class Node implements Comparable<Node> {
    public Integer score,mrxPos,playerNewPos;
    public ImmutableSet<Piece> winner;
    public Move prevMove;
    public ArrayList<Integer> playerLocations;
    public boolean mrXTurn,worthy;
    public ImmutableSet<Move> availableMoves;
    public ArrayList<Node> nodes;
    public Board.GameState nodeState;

    // Initialise new node to hold its parent node and player positions
    public Node(Move prevMove, Integer playerNewPos, boolean mrxTurn) {
        nodes = new ArrayList<>();
        playerLocations = new ArrayList<>();
        winner = ImmutableSet.copyOf(Collections.emptySet());
        this.prevMove = prevMove;
        this.mrXTurn = mrxTurn;
        if (mrxTurn) mrxPos = playerNewPos;
        else {
            this.playerNewPos = playerNewPos;
            mrxPos = 0;
        }
        worthy = false;
        score = 0;
    }

    public boolean isLeaf() {return !winner.isEmpty();}

    // Allows nodes to be compared according to their scores
    @Override
    public int compareTo(Node otherNode) {
        return this.score - otherNode.score;
    }
}