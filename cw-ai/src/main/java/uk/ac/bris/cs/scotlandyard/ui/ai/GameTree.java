package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public class GameTree {

    private Node rootNode;
    private Board.GameState rootState;
    private ScoreGen scoreO;
    private Move myMove;
    private int branchesOpened;

    // Initialise GameTree with state of root node
    public GameTree(Board board, Integer maxDepth,Integer[][] shortestPaths ) {
        branchesOpened = 0;
        this.rootState = constructState(board);
        if (!rootState.getWinner().isEmpty()) throw new RuntimeException("AI: Game already over.");
        this.scoreO = new ScoreGen(shortestPaths);
        rootNode = new Node(null,rootState.getAvailableMoves().asList().get(0).source(),false);
        Cloner cloner = new Cloner();
        Board.GameState stateSim = cloner.deepClone(rootState);
        rootNode.nodeState = stateSim;
        rootNode.availableMoves = rootState.getAvailableMoves();
        rootNode.mrxPos = rootNode.availableMoves.asList().get(0).source();
        rootNode.worthy = true;
        rootNode.winner = rootState.getWinner();
        ArrayList<Node> root = new ArrayList<>();
        root.add(rootNode);
        treeGen(root,maxDepth);
        Node bestNode = null;
        for (Node n : rootNode.nodes) {
            if (n.worthy) {
                System.out.println("Destination: " + n.mrxPos + " Score: " + n.score);
                if (bestNode == null) bestNode = n;
                if (n.score > bestNode.score) bestNode = n;
            }
        }
        myMove = bestNode.prevMove;
    }

    public Move getBestMove() {return myMove;}

    // Recursively create nodes to generate tree
    private void treeGen(ArrayList<Node> nodes, Integer maxDepth)  {
        for (Node node : nodes) {
            // Only continues branch if node isn't leaf and has a "good" score
            if (!node.isLeaf() && node.worthy) {
                branchesOpened ++;
                System.out.println("branches opened: " + branchesOpened);
                if (!(maxDepth ==0 && node.mrXTurn)) {
                    genChildNodes(node);
                    if (node.mrXTurn) treeGen(node.nodes,maxDepth -1);
                    else treeGen(node.nodes,maxDepth);

                    // Get mrx new pos from child
                    for (Node childNode : node.nodes) if (childNode.worthy) node.score += childNode.score;
                }
            }
        }
    }

    // Check if move is the final one to check
    private boolean finalMove(Node node) {
        if (node.availableMoves.asList().get(0).commencedBy().isMrX()) return true;
        return false;
    }

    // Clone state of each node and advance for each move to create child nodes
    private void genChildNodes(Node node) {
        // PrevMoves required to init each node by simulating all game states
        boolean mrxTurn = false;
        boolean finalMove = finalMove(node);
        ArrayList<Piece> found = new ArrayList<>();
        Cloner cloner = new Cloner();
        for (Move move : node.availableMoves) {
            if (found.size() < 2 || found.contains(move.commencedBy())) {
                if (!found.contains(move.commencedBy())) found.add(move.commencedBy());
                Move.DoubleMove newDMove= null;
                Move.SingleMove newSMove = null;
                int count = 0;
                for (ScotlandYard.Ticket t : move.tickets()) count ++;
                if (count == 1) newSMove = (Move.SingleMove)move;
                else newDMove = (Move.DoubleMove)move;
                Node childNode;
                if (move.commencedBy().isMrX()) mrxTurn = true;
                if (newDMove==null) {
                    childNode = new Node(move,newSMove.destination,mrxTurn);
                    if (newSMove.destination == node.mrxPos) System.out.println("Mrx lost");
                }
                else childNode = new Node(move,newDMove.destination2,mrxTurn);
                if (!finalMove) childNode.mrxPos = node.mrxPos;
                Board.GameState stateSim = cloner.deepClone(node.nodeState);
                stateSim = stateSim.advance(move);
                initNodeFromState(stateSim,childNode);
                node.nodes.add(childNode);
            }
            else System.out.println("Reduced number of players predicted");
        }
        pruneBranch(node, finalMove(node));
    }

    // Prune branch to remove any "unworthy" nodes
    private void pruneBranch(Node node,boolean max) {
        if (node.nodes.size() > 4) {
            // Choose how much each layer will be pruned
            Integer nodesToUse = node.nodes.size()-1;
            if (nodesToUse > 2) nodesToUse = 2;
            ArrayList<Node> scores = new ArrayList<>();
            for (Node n : node.nodes) scores.add(n);
            int scoreSize = scores.size();
            Collections.sort(scores);
            int start = 0;
            int end = nodesToUse;
            if (max) {
                start = scoreSize-nodesToUse-1;
                end = scoreSize;
            }
            for (int i = 0;i<scoreSize-1;i++) {
                if (i >=start && i < end) scores.get(i).worthy = true;
                else scores.get(i).nodeState = null;
            }
        }
        else for (Node n : node.nodes) n.worthy = true;
    }

    private void initNodeFromState(Board.GameState stateSim, Node childNode) {
        // Clone the root state to simulate each game state individually based on prevMoves
        // If not root bring state instance to current move in sim
        childNode.nodeState = stateSim;
        childNode.winner = stateSim.getWinner();
        if (childNode.winner.isEmpty()) {
            childNode.availableMoves = stateSim.getAvailableMoves();
            if (childNode.availableMoves.asList().get(0).commencedBy().isMrX()) {
                childNode.mrxPos = childNode.availableMoves.asList().get(0).source();
            }
            // Set everyone's locations each round
            getPlayerLocs(childNode);
        }
        scoreO.setScore(childNode);     // Set score to prune
    }

    private void getPlayerLocs(Node node) {
        //if last turn was mrx store all players locations in node
        if (node.mrXTurn) {
            ArrayList<Piece> found = new ArrayList<>();
            for (Move m : node.availableMoves) {
                if (!found.contains(m.commencedBy())){
                    found.add(m.commencedBy());
                    node.playerLocations.add(m.source());
                }
            }
            if (node.playerLocations.isEmpty() || node.playerLocations.get(0) == 0) throw new RuntimeException("Couldn't retrieve players location.");
        }
        else node.playerLocations.add(node.playerNewPos);
    }

    // Use StateFactory to construct a new state from a given board
    private Board.GameState constructState(Board board)  {
        ArrayList<Player> detectives = new ArrayList<>();
        Player mrxPlayer;
        Piece mrxPiece = null;
        int mrxLocation = 0;
        for (Move m : board.getAvailableMoves()) {
            if (m.commencedBy().isMrX()) {
                mrxPiece = m.commencedBy();
                mrxLocation = m.source();
            }
        }
        mrxPlayer = getPlayer(board,mrxPiece,mrxLocation);
        for (Piece p : board.getPlayers()) {
            if (p.isDetective()) {
                int loc = board.getDetectiveLocation((Piece.Detective)p).orElse(0);
                if (loc ==0) throw new RuntimeException("Couldn't retrieve detectives location.");
                detectives.add(getPlayer(board,p,loc));
            }
        }
        return 	MyGameStateFactory.a(board.getSetup(), mrxPlayer,ImmutableList.copyOf(detectives));
    }

    private static Player getPlayer(Board board, Piece p, int location) {
        Optional<Board.TicketBoard> ticketsBoard = board.getPlayerTickets(p);

        ImmutableMap<ScotlandYard.Ticket, Integer> tickets =  new ImmutableMap.Builder<ScotlandYard.Ticket, Integer>()
                .put(ScotlandYard.Ticket.TAXI,ticketsBoard.get().getCount(ScotlandYard.Ticket.TAXI))
                .put(ScotlandYard.Ticket.BUS,ticketsBoard.get().getCount(ScotlandYard.Ticket.BUS))
                .put(ScotlandYard.Ticket.UNDERGROUND,ticketsBoard.get().getCount(ScotlandYard.Ticket.UNDERGROUND))
                .put(ScotlandYard.Ticket.DOUBLE,ticketsBoard.get().getCount(ScotlandYard.Ticket.DOUBLE))
                .put(ScotlandYard.Ticket.SECRET,ticketsBoard.get().getCount(ScotlandYard.Ticket.SECRET))
                .build();
        return new Player(p,tickets,location);
    }
}