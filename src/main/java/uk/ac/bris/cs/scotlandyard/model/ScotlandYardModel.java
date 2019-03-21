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

import java.text.CollationElementIterator;
import java.util.*;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

import javax.swing.text.html.Option;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private ScotlandYardPlayer mrX;
	private List<ScotlandYardPlayer> detectives;
    private Colour currentPlayer;
    private int currentRound;
    private Map<Colour, ScotlandYardPlayer> players;

    private boolean gameOver;
    private List<Colour> playersList;
    private Set<Colour> winningPlayer = new HashSet<>();
	private Collection<Spectator> spectators = new HashSet<>();
    private int lastMrX = 0;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

        requireNonNull(mrX);
	    this.mrX = new ScotlandYardPlayer(mrX.player, mrX.colour, mrX.location, mrX.tickets);
        requireNonNull(firstDetective);
        requireNonNull(restOfTheDetectives);
        this.rounds = requireNonNull(rounds);
        this.graph = requireNonNull(graph);
        if(rounds.isEmpty() || graph.isEmpty())
        	throw new IllegalArgumentException("Rounds and/or graph should never be empty");
		//winningPlayer = new HashSet<>();

        currentPlayer = BLACK;
        currentRound = NOT_STARTED;
        gameOver = false;

		players = new HashMap<>();
		players.put(mrX.colour, this.mrX);


       if(mrX.colour != BLACK)
            throw new IllegalArgumentException("Mr X should be black!");
		if(!(mrX.tickets.containsKey(TAXI) &&
				mrX.tickets.containsKey(BUS) &&
				mrX.tickets.containsKey(UNDERGROUND) &&
				mrX.tickets.containsKey(SECRET) &&
				mrX.tickets.containsKey(DOUBLE)))
			throw new IllegalArgumentException("Every mrX needs fields for every possible ticket");


		detectives = new ArrayList<>();
		if(firstDetective.colour == BLACK)
			throw new IllegalArgumentException("Detectives have black colour?!");
		if(!(firstDetective.tickets.containsKey(TAXI) &&
				firstDetective.tickets.containsKey(BUS) &&
				firstDetective.tickets.containsKey(UNDERGROUND) &&
				firstDetective.tickets.containsKey(SECRET) &&
				firstDetective.tickets.containsKey(DOUBLE)))
			throw new IllegalArgumentException("firstDetective needs fields for every possible ticket");
		detectives.add(new ScotlandYardPlayer(firstDetective.player, firstDetective.colour, firstDetective.location, firstDetective.tickets));


        playersList = new ArrayList<>();
        playersList.add(mrX.colour);
		playersList.add(firstDetective.colour);
        //adds detectives to ScotlandYardPlayer list
        for (PlayerConfiguration player : restOfTheDetectives){
        	requireNonNull(player);
            if(player.colour == BLACK)
                throw new IllegalArgumentException("Detectives have black colour?!");
            if(!(player.tickets.containsKey(TAXI) &&
                    player.tickets.containsKey(BUS) &&
                    player.tickets.containsKey(UNDERGROUND) &&
                    player.tickets.containsKey(SECRET) &&
                    player.tickets.containsKey(DOUBLE)))
                throw new IllegalArgumentException("Every player needs fields for every possible ticket");
            detectives.add(new ScotlandYardPlayer(player.player, player.colour, player.location, player.tickets));
            playersList.add(player.colour);
		}

        //tests for identical locations and colours + secret/double moves
        Set<Integer> loc = new HashSet<>();
        Set<Colour> col = new HashSet<>();
        for (ScotlandYardPlayer player : detectives) {
            if (loc.contains(player.location()) || player.location() == mrX.location)
                throw new IllegalArgumentException("Duplicate location");
            loc.add(player.location());
            if (col.contains(player.colour()) || player.colour() == mrX.colour)
                throw new IllegalArgumentException("Duplicate colour");
            col.add(player.colour());

            //check that detectives don't have secret or double tickets
            if(player.hasTickets(SECRET) || player.hasTickets(DOUBLE))
                throw new IllegalArgumentException("Detectives don't have secret or double moves!");

            //Maps each detective to a colour
			players.put(player.colour(), player);
        }
 	}


	@Override
	public void registerSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!spectators.contains(spectator)) spectators.add(spectator);
		else throw new IllegalArgumentException("Duplicate spectator");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (spectators.contains(spectator)) spectators.remove(spectator);
		else throw new IllegalArgumentException("Spectator can't be unregistered, since it was not registered");
	}

	@Override
	public void startRotate() {
		ScotlandYardPlayer player = players.get(currentPlayer);
		if(isGameOver()) throw new IllegalStateException("Game is over at the beginning of the rotation");
		Set<Move> validMoves = validMove(player.colour());
		player.player().makeMove(this, player.location(), validMoves, this);
	}

	private Set<Move> validMove(Colour player) {
		ScotlandYardPlayer p;
		p = players.get(player);
		Set<Move> set = new HashSet<>();
		Collection<Edge<Integer, Transport>> edges = getGraph().getEdgesFrom(new Node<Integer>(p.location()));
		for (Edge<Integer, Transport> edge : edges){
			Transport t = edge.data();
			int destination = edge.destination().value();
			boolean mere = true;
			if(!p.hasTickets(Ticket.fromTransport(t)))
				mere = false;
			for (ScotlandYardPlayer d : detectives){
				if(d.location() == destination)
					mere = false;
			}
			if(mere)
				set.add(new TicketMove(p.colour(), Ticket.fromTransport(t), destination));
		}
		return set;
	}


	@Override
	public void accept(Move move){

	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		return Collections.unmodifiableList(playersList);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return Collections.unmodifiableSet(winningPlayer);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		ScotlandYardPlayer p = players.get(colour);
		if(p == null)
			return Optional.empty();
		if(p.isDetective())
			return Optional.of(p.location());

		if(currentRound != 0 && rounds.get(currentRound-1)) {
			lastMrX = p.location();
			return Optional.of(lastMrX);
		}
		return Optional.of(lastMrX);
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {

		ScotlandYardPlayer p = players.get(colour);
		if(p == null)
			return Optional.empty();
		return Optional.ofNullable(p.tickets().get(ticket));
	}

	@Override
	public boolean isGameOver() {
		return gameOver;
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
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}

}
