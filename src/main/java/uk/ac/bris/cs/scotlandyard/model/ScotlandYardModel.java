package uk.ac.bris.cs.scotlandyard.model;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

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
        checkGameOver();
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
		if(checkGameOver())
			throw new IllegalStateException("Game is over at the beginning of the rotation");
		Set<Move> validMoves = validMove(player.colour());
		player.player().makeMove(this, player.location(), validMoves, this);
	}

	private Set<Move> validMoves = new HashSet<>();

	private Set<Move> validMove(Colour player) {
		ScotlandYardPlayer p;
		p = players.get(player);
		validMoves.clear();
		Collection<Edge<Integer, Transport>> edges = getGraph().getEdgesFrom(new Node<>(p.location()));
		for (Edge<Integer, Transport> edge : edges){
			Transport t = edge.data();
			int destination = edge.destination().value();
			boolean mere = true;

			for (ScotlandYardPlayer d : detectives){
				if(d.location() == destination)
					mere = false;
			}

			if(mere && p.hasTickets(SECRET))
				validMoves.add(new TicketMove(p.colour(), SECRET, destination));

			if(!p.hasTickets(Ticket.fromTransport(t)))
				mere = false;

			if(mere)
				validMoves.add(new TicketMove(p.colour(), Ticket.fromTransport(t), destination));
		}
		//System.out.println(validMoves.toString());
		if(validMoves.isEmpty() && player != mrX.colour())
			validMoves.add(new PassMove(player));
		if(player == mrX.colour()){
			validMoves.addAll(possibleDoubleMoves(validMoves));
		}
		return Collections.unmodifiableSet(validMoves);
	}

	private Set<Move> possibleDoubleMoves(Set<Move> moves){

		//System.out.println(rounds.size());
		if(!mrX.hasTickets(DOUBLE) || getCurrentRound() > rounds.size()-2)
			return Collections.emptySet();
		Set<Move> db = new HashSet<>();
		for(Move move : moves)
			if (move instanceof TicketMove) {
				TicketMove m = (TicketMove) move;
				Collection<Edge<Integer, Transport>> edges = getGraph().getEdgesFrom(new Node<>(m.destination()));
				for (Edge<Integer, Transport> edge : edges) {
					Transport t = edge.data();
					boolean mere = true;
					int destination = edge.destination().value();

					for (ScotlandYardPlayer d : detectives) {
						if (d.location() == destination)
							mere = false;
					}
					if (mere) {
						if(mrX.hasTickets(SECRET) && m.ticket()!=SECRET)
							db.add(new DoubleMove(mrX.colour(), m, new TicketMove(mrX.colour(), SECRET, destination)));
						if(m.ticket() == SECRET && mrX.hasTickets(SECRET, 2))
							db.add(new DoubleMove(mrX.colour(), m, new TicketMove(mrX.colour(), SECRET, destination)));

						if (m.ticket() == Ticket.fromTransport(t)) {
							if (!mrX.hasTickets(Ticket.fromTransport(t), 2))
								mere = false;
						} else if (!mrX.hasTickets(Ticket.fromTransport(t)))
							mere = false;

					}
					if (mere)
						db.add(new DoubleMove(mrX.colour(), m, new TicketMove(mrX.colour(), Ticket.fromTransport(t), destination)));

				}
			}
		return db;
	}

	private Colour nextPlayer(Colour currentPlayer){
		int index = playersList.indexOf(currentPlayer) + 1;
		if(index == 1)
			currentRound++;
		if (index < playersList.size())
			return playersList.get(index);
		else{
			return playersList.get(0);
		}
	}

	@Override
	public void accept(Move move){
        requireNonNull(move);
        if(!validMoves.contains(move))
        	throw new IllegalArgumentException("Move not in valid moves: "+move.toString() + "\nValid Moves: + "+ validMoves.toString());

        if(!checkGameOver()) {
			ScotlandYardPlayer cp;
			if (move instanceof TicketMove) {
				cp = players.get(currentPlayer);
				TicketMove tm = (TicketMove) move;
				cp.location(tm.destination());
				if (cp.isDetective())
					mrX.addTicket(tm.ticket());
				if (cp.isMrX()) {
					if (rounds.get(getCurrentRound()))
						lastMrX = mrX.location();
					else move = new TicketMove(BLACK, tm.ticket(), lastMrX);
				}
				cp.removeTicket(tm.ticket());
			}

			if (move instanceof DoubleMove) {
				DoubleMove doublemove = (DoubleMove) move;
				TicketMove firstmove = doublemove.firstMove();
				TicketMove secondmove = doublemove.secondMove();
				if (!rounds.get(getCurrentRound())) {
					lastMrX = firstmove.destination();
					currentRound++;
					//accept(doublemove.firstMove());
					players.get(currentPlayer).removeTicket(DOUBLE);
					players.get(currentPlayer).removeTicket(firstmove.ticket());
					players.get(currentPlayer).removeTicket(secondmove.ticket());
				}

			}
			currentPlayer = nextPlayer(currentPlayer);
			checkGameOver();
			cp = players.get(currentPlayer);
			cp.player().makeMove(this, cp.location(), validMove(currentPlayer), this);
		}


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

	private boolean checkGameOver(){

		gameOver = false;
		//no more rounds :(
		if(getCurrentRound() > rounds.size() - 1){
			gameOver = true;
			winningPlayer.add(mrX.colour());
			return true;
		}
		//Detectives are stuck
		gameOver = true;
		for(ScotlandYardPlayer detective : detectives){
			//System.out.println(validMove(detective.colour()).toString());
			if(!validMove(detective.colour()).contains(new PassMove(detective.colour())))
				gameOver = false;
		}
		if(gameOver){
			winningPlayer.add(mrX.colour());
			return true;
		}

		//mrX is captured
		for (ScotlandYardPlayer detective : detectives)
			if(detective.location() == mrX.location()){
				winningPlayer.addAll(playersList.subList(1, playersList.size()));
				gameOver = true;
				return true;
			}
		//MrX stucked
		if(getCurrentPlayer() == mrX.colour())
			if(validMove(mrX.colour()).isEmpty()){
				winningPlayer.addAll(playersList.subList(1, playersList.size()));
				gameOver = true;
				return true;
			}
		return false;


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

