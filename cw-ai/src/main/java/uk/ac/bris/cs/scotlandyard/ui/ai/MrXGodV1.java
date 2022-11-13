package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MrXGodV1 implements Ai {

	private ShortestPath spCalc;
	private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;
	private Integer[][] shortestPaths;

	@Nonnull @Override public String name() {return "MrXGodV1";}

	@Override
	public void onStart() {
		// Produce ScotSPs.txt, commented to avoid wait at start of every game
		// Uses ShortestPath class to create text file containing shortest paths between each node

		/*try {
			defaultGraph = readGraph(Resources.toString(Resources.getResource("graph.txt"),StandardCharsets.UTF_8));
		} catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
		try {
			spCalc = new ShortestPath(defaultGraph);
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		// Read in ScotSPs.txt to store shortest paths in 2d array
		shortestPaths = new Integer[200][200];
		try {
			File SP = new File("ScotSPs.txt");
			Scanner readFile = new Scanner((SP));
			String spData = "";
			while (readFile.hasNextLine()) spData = readFile.nextLine();
			char[] vals = spData.toCharArray();
			int posInData=0;
			String mrXPos = "";
			String detPos= "";
			String data = "";
			for (char val : vals) {
				if (val == ' ') {
					switch (posInData) {
						case 0:
							mrXPos = data;
							posInData++;
							break;
						case 1:
							detPos = data;
							posInData++;
							break;
						case 2:
							shortestPaths[Integer.parseInt(mrXPos)][Integer.parseInt(detPos)] = Integer.parseInt(data);
							posInData =0;
							break;
					}
					data = "";
				}
				else data += val;
			}
			readFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// Create a gameTree and return the best move generated
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		final Integer rounds = 1;
		GameTree tree = null;
		tree = new GameTree(board,rounds,shortestPaths);
		return tree.getBestMove();
	}
}