package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Transport.FERRY;

import java.util.*;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private ScotlandYardPlayer mrX;
	private List<ScotlandYardPlayer> detectives;
    private Colour currentPlayer;
    private int currentRound;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

        requireNonNull(mrX);
	    this.mrX = new ScotlandYardPlayer(mrX.player, mrX.colour, mrX.location, mrX.tickets);
        requireNonNull(firstDetective);
        requireNonNull(restOfTheDetectives);
        this.rounds = requireNonNull(rounds);
        this.graph = requireNonNull(graph);
        currentPlayer = BLACK;
        currentRound = NOT_STARTED;

        if(mrX.colour != BLACK)
            throw new IllegalArgumentException("Mr X should be black!");

        detectives = new ArrayList<>();
        detectives.add(new ScotlandYardPlayer(firstDetective.player, firstDetective.colour, firstDetective.location, firstDetective.tickets));

        if(!(mrX.tickets.containsKey(TAXI) &&
                mrX.tickets.containsKey(BUS) &&
                mrX.tickets.containsKey(UNDERGROUND) &&
                mrX.tickets.containsKey(FERRY) &&
                mrX.tickets.containsKey(SECRET) &&
                mrX.tickets.containsKey(DOUBLE)))
            throw new IllegalArgumentException("Every mrX needs fields for every possible ticket");

        //adds detectives to ScotlandyardPlayer list
        for (PlayerConfiguration player : restOfTheDetectives){
        	requireNonNull(player);
            if(player.colour == BLACK)
                throw new IllegalArgumentException("Detectives have black colour?!");
            if(!(player.tickets.containsKey(TAXI) &&
                    player.tickets.containsKey(BUS) &&
                    player.tickets.containsKey(UNDERGROUND) &&
                    player.tickets.containsKey(FERRY) &&
                    player.tickets.containsKey(SECRET) &&
                    player.tickets.containsKey(DOUBLE)))
                throw new IllegalArgumentException("Every player needs fields for every possible ticket");
            detectives.add(new ScotlandYardPlayer(player.player, player.colour, player.location, player.tickets));
		}

        //tests for identical locations and colours + secret/double moves
        Set<Integer> loc = new HashSet<>();
        Set<Colour> col = new HashSet<>();
        for (ScotlandYardPlayer player : detectives) {
            if (loc.contains(player.location()))
                throw new IllegalArgumentException("Duplicate location");
            loc.add(player.location());
            if (col.contains(player.colour()))
                throw new IllegalArgumentException("Duplicate colour");
            col.add(player.colour());

            //MUIE PSD
            if(player.hasTickets(SECRET) || player.hasTickets(DOUBLE))
                throw new IllegalArgumentException("Detectives don't have secret moves!");
        }



 	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return graph;
	}

}
