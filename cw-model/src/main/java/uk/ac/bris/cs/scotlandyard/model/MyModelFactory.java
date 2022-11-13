package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	public final class MyModel implements Model{

		private Board.GameState state;
		private HashSet<Observer> observers;

		private MyModel(Board.GameState state){
			this.state = state;
			this.observers = new HashSet<>();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException();
			if(observers.contains(observer)) throw new IllegalArgumentException();
			observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException();
			if(!observers.contains(observer)) throw new IllegalArgumentException();
			observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			// Call advance method
			this.state = state.advance(move);

			// Check if game is over
			boolean gameOver = true;
			if(state.getWinner().isEmpty()) gameOver = false;

			// Notify observers of new state
			for(Observer o : observers){
				if(gameOver) o.onModelChanged(getCurrentBoard(), Observer.Event.GAME_OVER);
				else o.onModelChanged(getCurrentBoard(), Observer.Event.MOVE_MADE);
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		MyGameStateFactory stateFactory = new MyGameStateFactory();
		Board.GameState state = stateFactory.build(setup, mrX, detectives);

		return new MyModel(state);
	}
}