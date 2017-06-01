package ca.karlphillips;

import java.util.ArrayList;
import java.util.List;

import com.simdezimon.overwatch.Player;

/**
 * Organizes a PUGS (pick up games) by taking in battletags, searching up their
 * rank, and making "fair" teams.
 * 
 * @author Karl Phillips
 *
 */
public class Scrim {

	public static final int MAX_PLAYERS = 12;
	public static final int MAX_TEAM_SIZE = MAX_PLAYERS / 2;

	public interface REGISTRATION {
		public static final boolean OPEN = true;
		public static final boolean CLOSED = false;
	}

	private String region;
	private String platform;

	private boolean registration;

	private List<Player> players = new ArrayList<Player>(12);
	private Player[] teamA = new Player[MAX_TEAM_SIZE];
	private Player[] teamB = new Player[MAX_TEAM_SIZE];

	public Scrim(String region, String platform) {
		if (region == null || platform == null)
			throw new IllegalArgumentException();
		if (region.equals(Region.NA) || region.equals(Region.EU)
				|| region.equals(Region.ASIA)) {
			this.region = region;
		} else {
			throw new IllegalArgumentException();
		}
		if (platform.equals(Platform.PC) || platform.equals(Platform.PS)
				|| platform.equals(Platform.XBOX)) {
			this.platform = platform;
		} else {
			throw new IllegalArgumentException();
		}

		setRegistrationStatus(REGISTRATION.OPEN);
	}

	/**
	 * Registers the player to the Scrim.
	 * 
	 * @param aPlayer Player to register.
	 * @return true if registered, false if player was not
	 */
	public boolean register(Player aPlayer) {
		if (players.size() < MAX_PLAYERS && !playerRegistered(aPlayer)) {
			players.add(aPlayer);
			if (isFull()) {
				setRegistrationStatus(REGISTRATION.CLOSED);
				truncatePlayers(); // Just in case
			}
			return true;
		} else {
			return false;
		}
	}

	private void truncatePlayers() {
		while (players.size() > MAX_PLAYERS) {
			players.remove(players.size() - 1);
		}
	}

	/**
	 * Populates the 2 teams. Does nothing if not enough players.
	 */
	public void makeTeams() {
		if (getRegistrationStatus())
			return; // Registration open, Still need more players
		players.sort((a, b) -> {
			int rankA = a.getEntries().get(0).getData().getRank();
			int rankB = b.getEntries().get(0).getData().getRank();
			return rankA - rankB;
		});
		
		//Even players in teamA, odd players in team B
		//Should be roughly even after sorting the list low -> hi
		List<Player> a = new ArrayList<Player>();
		List<Player> b = new ArrayList<Player>();
		for(int i = players.size() - 1; i >= 0; i--) {
			if(a.size() == MAX_TEAM_SIZE) b.add(players.get(i));
			if(b.size() == MAX_TEAM_SIZE) a.add(players.get(i));
			if (sum(a) < sum(b)) {
				a.add(players.get(i));
			} else {
				b.add(players.get(i));
			}
		}
		this.teamA = a.toArray(new Player[MAX_TEAM_SIZE]);
		this.teamB = b.toArray(new Player[MAX_TEAM_SIZE]);
	}

	
	private int sum(List<Player> team) {
		int total = 0;
		for(Player p : team) {
			total += p.getEntries().get(0).getData().getRank();
		}
		return total;
	}
	
	/**
	 * Returns a clone of the players in TeamA. Empty if not enough players
	 * 
	 * @return TeamA
	 */
	public Player[] getTeamA() {
		makeTeams();
		return this.teamA.clone();
	}

	/**
	 * Returns a clone of the players in TeamB. Empty if not enough players
	 * 
	 * @return TeamB
	 */
	public Player[] getTeamB() {
		makeTeams();
		return this.teamB.clone();
	}

	/**
	 * Checks if a player is already registered for the Scrim.
	 * 
	 * @param battletag the player to check
	 * @return if the player is registered
	 */
	private boolean playerRegistered(Player aPlayer) {
		return players.contains(aPlayer);
	}

	/**
	 * Sets whether or not the Scrim should accept new players
	 * 
	 * @param isOpen true if new players should be accepted
	 */
	public void setRegistrationStatus(boolean isOpen) {
		this.registration = isOpen;
	}

	/**
	 * Returns whether or not the Scrim should accept new players
	 * 
	 * @return true if new players should be accepted
	 */
	public boolean getRegistrationStatus() {
		return this.registration;
	}
	
	/**
	 * Returns whether or not the scrim has max players
	 * @return if the scrim is full
	 */
	public boolean isFull() {
		return players.size() >= MAX_PLAYERS;
	}

}
