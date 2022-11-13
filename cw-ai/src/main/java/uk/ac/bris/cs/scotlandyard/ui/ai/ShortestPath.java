package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

// Code used to produce ScotSPs.txt

public class ShortestPath {

    private Integer[][] shortestPaths;

    private int size;
    private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;

    public ShortestPath(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) throws IOException {
        size = graph.nodes().size();
        this.graph = graph;
        shortestPaths = new Integer[size+1][size+1];
        setSPs();
    }

    private void setSPs() throws IOException {
        FileWriter myWriter = new FileWriter("ScotSPs.txt");
        for (Integer startNode =1;startNode <= size;startNode++) {
            for (Integer endNode = 1; endNode<= size ; endNode++) {
                shortestPaths[startNode][endNode] = 10;
                if (startNode == endNode) {
                    shortestPaths[startNode][endNode] = 0;
                    myWriter.write(startNode + " " + endNode + " 0" + " ");

                }
                else {
                    ArrayList<Integer> visited = new ArrayList<>();
                    visited.add(startNode);
                    SetSP(startNode,endNode,visited);
                    System.out.println("Shortest path between " + startNode + " & " + endNode + " " + shortestPaths[startNode][endNode]);
                        myWriter.write(startNode + " " + endNode + " " + shortestPaths[startNode][endNode] + " ");
                }
            }
        }
        myWriter.close();
    }


    private void SetSP(Integer start,Integer end, ArrayList<Integer> visited) {
        if (graph.adjacentNodes(visited.get(visited.size()-1)).contains(end)) {
            if (visited.size() < shortestPaths[start][end]) shortestPaths[start][end] = visited.size();
        }
        else if (visited.size() < shortestPaths[start][end]){
            for (Integer pos : graph.adjacentNodes(visited.get(visited.size()-1))) {
                if(!visited.contains(pos)) {
                    ArrayList<Integer> newVisited = new ArrayList<>(visited);
                    newVisited.add(pos);
                    SetSP(start,end,newVisited);
                }
            }
        }
    }
}
