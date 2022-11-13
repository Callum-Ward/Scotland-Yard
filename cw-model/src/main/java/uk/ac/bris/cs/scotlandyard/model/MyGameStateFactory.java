package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source){
		final var singleMoves = new ArrayList<Move.SingleMove>();
		for(int destination : setup.graph.adjacentNodes(source)) {
			boolean empty = true;
			for(Player d : detectives){
				if (d.location() == destination) {
					empty = false;
					break;
				}
			}
			if(empty){
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {
						Move.SingleMove singleMove = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
						singleMoves.add(singleMove);
					}
					if (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET)) {
						Move.SingleMove singleMove = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
						singleMoves.add(singleMove);
					}
				}
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source,
			ImmutableList<LogEntry> log){
		final var doubleMoves = new ArrayList<Move.DoubleMove>();
		if(!player.has(ScotlandYard.Ticket.DOUBLE)) return ImmutableSet.copyOf(doubleMoves); // Return empty set if player has no x2 card
		if(log.size()+2 > setup.rounds.size()) return ImmutableSet.copyOf(doubleMoves); // Return empty set if there aren't enough rounds for a double move
		for(int destination1 : setup.graph.adjacentNodes(source)) {
			// Check that destination1 is free
			boolean empty1 = true;
			for(Player d : detectives){
				if(d.location() == destination1){
					empty1 = false;
					break;
				}
			}
			if (!empty1) continue;
			// Check player has ticket to get to destination1
			boolean hasTicket1 = false;
			ScotlandYard.Transport t1 = null;
			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
				if (player.has(t.requiredTicket()) || (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET))) {
					hasTicket1 = true;
					t1 = t;
				}
				if (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET)) {
					hasTicket1 = true;
				}
			}
			if (!hasTicket1) continue;

			for(int destination2 : setup.graph.adjacentNodes(destination1)){
				boolean empty = true;
				for(Player d : detectives){
					if (d.location() == destination2) {
						empty = false;
						break;
					}
				}
				if(empty){
					for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
						if (player.has(t2.requiredTicket())) {
							boolean sameTickets = false;
							boolean enoughTickets = false;
							if(t1.requiredTicket().equals(t2.requiredTicket())) sameTickets = true;
							if (!sameTickets) enoughTickets = true;
							if(sameTickets && player.hasAtLeast(t1.requiredTicket(), 2)) enoughTickets = true;
							if (enoughTickets){
								Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2);
								doubleMoves.add(doubleMove);
							}

						}
						if (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET)) {		// Use secret ticket to get to dest. 2
							Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, ScotlandYard.Ticket.SECRET, destination2);
							doubleMoves.add(doubleMove);
						}
						if (player.isMrX() && player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)){	// Use secret ticket to get to both destinations
							Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, ScotlandYard.Ticket.SECRET, destination2);
							doubleMoves.add(doubleMove);
						}
						if (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET) && player.has(t2.requiredTicket())) {	// Use secret ticket to get to dest. 1
							Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, t2.requiredTicket(), destination2);
							doubleMoves.add(doubleMove);
						}
					}
				}
			}

		}
		return ImmutableSet.copyOf(doubleMoves);
	}

	// Helper method for constructor to assign winner or empty set if game still in play
	ImmutableSet<Piece> determineWinner(ImmutableList<LogEntry> log,
										GameSetup setup,
										List<Player> detectives,
										Player mrX,
										ImmutableSet<Move> moves,
										ImmutableSet<Piece> remaining){
		ImmutableSet<Piece> winners;

		// MrX wins if he has not been caught and game ends
		if(log.size() > setup.rounds.size() || remaining.isEmpty()){
			winners = ImmutableSet.of(mrX.piece());
			return winners;
		}

		// MrX wins if all detectives are stuck on their turn
		boolean detectiveHasMove = false;
		if(!remaining.contains(mrX.piece())) {
			for(Player d : detectives){
				if(!(makeSingleMoves(setup, detectives, d, d.location()).isEmpty())) detectiveHasMove = true;
			}
			if (!detectiveHasMove) {
				winners = ImmutableSet.of(mrX.piece());
				return winners;
			}
		}

		// MrX wins if no detectives have tickets left
		boolean gotTickets = false;
		for (Player d : detectives){
			for(Integer i : d.tickets().values()){
				if(i != 0) gotTickets = true;
			}
		}
		if(!gotTickets){
			winners = ImmutableSet.of(mrX.piece());
			return winners;
		}

		// Detectives win if MrX captured
		boolean detectivesWin = false;
		for (Player d : detectives)
			if (d.location() == mrX.location()) detectivesWin = true;

		// Detectives win if MrX stuck on his turn
		if(remaining.contains(mrX.piece())) {
			boolean mrXHasMoves = false;
			for (Move m : moves) if (m.commencedBy().isMrX()) mrXHasMoves = true;
			if (!mrXHasMoves) detectivesWin = true;
		}

		// If detectives have won, return them as set
		if(detectivesWin){
			ArrayList<Piece> winnersList = new ArrayList<>();
			for(Player d : detectives) winnersList.add(d.piece());
			return ImmutableSet.copyOf(winnersList);
		}

		// If this point reached, no one has won yet and game still in play
		return ImmutableSet.of();
	}

	public final class MyGameState implements GameState{
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			// Validity checks
			if(mrX == null) throw new NullPointerException("There is no mrX");
			if(detectives == null) throw new NullPointerException("There are no detectives");
			if(!mrX.isMrX()) throw new IllegalArgumentException("MrX is a detective");
			boolean anotherMrX = false;
			for (Player d : detectives)	if (d.isMrX()) anotherMrX = true;
			if(mrX.isMrX() && anotherMrX) throw new IllegalArgumentException("More than one mrX");
			for (Player d : detectives){
				if (d.has(ScotlandYard.Ticket.SECRET) || d.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has incorrect ticket");
			}

			for (int i = 0; i < detectives.size(); i++){
				for (int j = 0; j < detectives.size(); j++){
					if (i!=j && detectives.get(i).piece().equals(detectives.get(j).piece())) throw new IllegalArgumentException("Duplicate detective");
					if (i!=j && detectives.get(i).location() == detectives.get(j).location()) throw new IllegalArgumentException("Two detectives in same location");
				}
			}

			if(setup.rounds.isEmpty()) throw new IllegalArgumentException("Reveal rounds list is empty");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");

			// Initialise attributes if validity checks passed
			this.setup = setup;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.everyone = ImmutableList.<Player>builder().addAll(detectives).add(mrX).build();

			// Remove any player from remaining if they have no moves
			ArrayList<Piece> newRemaining = new ArrayList<>();
			for(Player p : everyone){
				if(remaining.contains(p.piece())){
					if(p.piece().isDetective()) {
						if (!(makeSingleMoves(setup, detectives, p, p.location()).isEmpty())) {
							newRemaining.add(p.piece());
						}
					}else if(p.piece().isMrX()){
						if (!(makeSingleMoves(setup, detectives, p, p.location()).isEmpty()) || !(makeDoubleMoves(setup, detectives, mrX, mrX.location(), log)).isEmpty()) {
							newRemaining.add(p.piece());
						}
					}
				}
			}
			this.remaining = ImmutableSet.copyOf(newRemaining);

			var singleMoves = new ArrayList<Move.SingleMove>();
			var doubleMoves = new ArrayList<Move.DoubleMove>();

			if(this.remaining.contains(mrX.piece())) {
				singleMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location())); // Only do this if current player is mrX
				doubleMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location(), log)); // Only do this if current player is mrX
			}
			else {
				for (Player d : detectives) {
					if (this.remaining.contains(d.piece())) singleMoves.addAll(makeSingleMoves(setup, detectives, d, d.location())); // Only do this if current player is detectives
				}
			}

			var allMoves = new ArrayList<Move>();
			allMoves.addAll(singleMoves);
			allMoves.addAll(doubleMoves);
			this.moves = ImmutableSet.copyOf(allMoves);
			this.winner = determineWinner(log, setup, detectives, mrX, moves, remaining);
			if(!winner.isEmpty()) moves = ImmutableSet.of();	// Moves should be empty if winner exists
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			ImmutableSet<Player> playerSet = ImmutableSet.copyOf(everyone);
			ArrayList<Piece> pieceSet = new ArrayList<>();
			for(Player p : playerSet){
				pieceSet.add(p.piece());
			}
			return ImmutableSet.copyOf(pieceSet);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player d : detectives){
				if (d.piece().equals(detective)){
					return Optional.of(d.location());
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			boolean exists = false;
			for (Player d : detectives){
				if (d.piece().equals(piece)) {
					exists = true;
					break;
				}
			}
			if (piece.isMrX()) exists = true;
			if(exists) {
				TicketBoard ticketBoard = ticket -> {
					for (Player d : detectives) {
						if (d.piece().equals(piece)) {
							return d.tickets().get(ticket);
						}
					}
					return mrX.tickets().get(ticket);
				};
				return Optional.of(ticketBoard);
			}else return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		// Implementation of moveVisitor to use in advance method
		private class MyMoveVisitor implements Move.Visitor<Move> {
			private Player updatedPlayer;
			private ArrayList<LogEntry> updatedLog = new ArrayList<>();

			@Override
			public Move visit(Move.SingleMove move) {
				if(move.commencedBy().isMrX()) {
					this.updatedPlayer = new Player(move.commencedBy(), mrX.tickets(), move.destination);
					if(setup.rounds.get(log.size())) updatedLog.add(LogEntry.reveal(move.ticket, move.destination));
					else updatedLog.add(LogEntry.hidden(move.ticket));
				}
				else for(Player d : detectives){
					if(move.commencedBy().equals(d.piece())){
						this.updatedPlayer = new Player(move.commencedBy(), d.tickets(), move.destination);
					}
				}
				this.updatedPlayer = this.updatedPlayer.use(move.tickets());
				return move;
			}

			@Override
			public Move visit(Move.DoubleMove move) {
				this.updatedPlayer = new Player(move.commencedBy(), mrX.tickets(), move.destination2);
				this.updatedPlayer = this.updatedPlayer.use(move.tickets());

				// Add both tickets used to log
				if(setup.rounds.get(log.size())) {
					updatedLog.add(LogEntry.reveal(move.ticket1, move.destination1));
				}else {
					updatedLog.add(LogEntry.hidden(move.ticket1));
				}
				if(setup.rounds.get(log.size()+1)){
					updatedLog.add(LogEntry.reveal(move.ticket2, move.destination2));
				}else {
					updatedLog.add(LogEntry.hidden(move.ticket2));
				}
				return move;
			}

			Player getUpdatedPlayer() {
				return updatedPlayer;
			}

			ArrayList<LogEntry> getUpdatedLog(){
				return updatedLog;
			}
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move); // Check move is valid
			Player updatedMrX = mrX;
			ArrayList<Player> updatedDetectives = new ArrayList<>(detectives);
			ArrayList<LogEntry> updatedLog = new ArrayList<>(log);

			MyMoveVisitor myMoveVisitor = new MyMoveVisitor();
			move.visit(myMoveVisitor);

			updatedLog.addAll(myMoveVisitor.getUpdatedLog());	// Use visitor to update log

			// Use visitor to update location of player
			if(move.commencedBy().isMrX()){
				updatedMrX = myMoveVisitor.getUpdatedPlayer();
			}
			else for(Player d : detectives) {
				if(move.commencedBy().equals(d.piece())) {
					updatedDetectives.remove(d);
					updatedDetectives.add(myMoveVisitor.getUpdatedPlayer());
				}
			}

			// Update remaining players in round
			ArrayList<Piece> updatedRemaining = new ArrayList<>(remaining);
			if(move.commencedBy().isMrX()){
				updatedRemaining.remove(mrX.piece());
				for (Player d : updatedDetectives) updatedRemaining.add(d.piece());
			}else if(move.commencedBy().isDetective()) {
				updatedMrX = updatedMrX.give(move.tickets());
				for(Player d : detectives){
					if(move.commencedBy().equals(d.piece())){
						updatedRemaining.remove(d.piece());
					}
				}
				if(updatedRemaining.isEmpty() && setup.rounds.size() > updatedLog.size()) {
					updatedRemaining.add(updatedMrX.piece());
					for (Player d : updatedDetectives) updatedRemaining.add(d.piece());
				}
			}
			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(updatedLog), updatedMrX, ImmutableList.copyOf(updatedDetectives));
		}
	}


	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		ArrayList<Piece> pieceSet = new ArrayList<>();
		pieceSet.add(mrX.piece());
		for(Player p : detectives){
			pieceSet.add(p.piece());
		}
		ImmutableSet<Piece> players = ImmutableSet.copyOf(pieceSet);
		return new MyGameState(setup, players, ImmutableList.of(), mrX, detectives);

	}

}
