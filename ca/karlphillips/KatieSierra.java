package ca.karlphillips;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.Event;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.simdezimon.overwatch.Entry;
import com.simdezimon.overwatch.OverwatchStats;
import com.simdezimon.overwatch.Player;
import com.simdezimon.overwatch.PlayerData;
import com.simdezimon.overwatch.PlayerId;

public class KatieSierra extends ListenerAdapter {

	private static final String KATIE_SIERRA_TOKEN = "MzA3MzU1MTE3OTg2OTA2MTEz.C-RYLg.KL0ywFu2Lme7enn0q1AkJJzA0IU";

	private static final char CALLSIGN = '!';
	private static final String REGION = Region.NA;
	private static final String PLATFORM = Platform.PC;
	private static final OverwatchStats OWStats = initStats();

	private static final String EMOJI_ACCEPT = "\u2705";
	private static final String EMOJI_UNSURE = "\u2754";
	private static final String EMOJI_REJECT = "\u274C";
	private static final String EMOJI_CRYLAUGH = "\uD83D\uDE02";
	private static final String EMOJI_THINKING = "\uD83D\uDCAD"; // Same as
																						// EMOJI_UNSURE
																						// for now

	private static OverwatchStats initStats() {
		try {
			return new OverwatchStats();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<DiscordPlayer> players = new ArrayList<DiscordPlayer>();

	public static void main(String[] args) {
		try {
			JDA jda = new JDABuilder(AccountType.BOT).setToken(KATIE_SIERRA_TOKEN)
					.addListener(new KatieSierra()).buildAsync();
		} catch (LoginException e) {
			// Authentication error
			e.printStackTrace();
		} catch (RateLimitedException e) {
			// Too many logins error
			e.printStackTrace();
		}

	}

	public KatieSierra() {
		/*
		 * try { OWStats = new OverwatchStats(); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */
	}

	/**
	 * Activates on message received in server of interest. NOTE: Method is
	 * single threaded and queues all messages!
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		
		if (event.getAuthor().isBot())
			return;

		final JDA jda = event.getJDA();
		final User author = event.getAuthor(); // The user that sent the message
		final Message message = event.getMessage(); // The message that was
																	// received.
		final String[] content = message.getContent().split(" ");
		final MessageChannel channel = event.getChannel(); // This is the
																			// MessageChannel that
																			// the message was sent
																			// to.
		// There shouldn't be any messages that have no characters (no out of
		// bounds)
		// but if there is add a check here
		if (message.getContent().charAt(0) != CALLSIGN)
			return; // Ignored

		if (cmd(content, "ready") || cmd(content, "r")) {
			if (content.length == 3) {
				// Add player with given rank.

				if (!content[2].matches("^-?\\d+$")) {
					addThoughtsTo(message, EMOJI_UNSURE);
					channel.sendMessage("Please enter a number between 1 and 5000.")
							.queue();
					return;
				}
				
				// If the number is invalid ( > MAX_INT ) or out of SR range, the
				// method ends. Yeah it's ugly
				int rank = -1;
				try {
					rank = Integer.parseInt(content[2]);
					if (rank > 5000 || rank < 1) {
						throw new IllegalArgumentException();
					}
				} catch (IllegalArgumentException e) {
					addThoughtsTo(message, EMOJI_CRYLAUGH);
					channel
							.sendMessage(
									"Yo stop playing I know you ain't that rank ...")
							.queue();
					return;
				}

				String formatted = formatBattlenet(content[1]);
				DiscordPlayer p = new DiscordPlayer(new PlayerId(formatted, REGION, PLATFORM), author.getId());
				Entry e = new Entry(new PlayerData());
				e.getData().setRank(rank);
				p.getEntries().add(e);
				players.add(p);
				addThoughtsTo(message, EMOJI_UNSURE);
				return;
			} else if (content.length == 2) {
				// No rank given, look it up

				String formatted = formatBattlenet(content[1]);
				PlayerId id = new PlayerId(formatted, REGION, PLATFORM);
				
				// If Player does not have a rank cached
				// Change this later for when you want to save cache on exit
				// Have it like, check if the entry is too old instead or smth
				if (!OWStats.getIndex().getPlayers().containsKey(id.toString())) {
					DiscordPlayer player = new DiscordPlayer(id, author.getId());
					players.add(player);
					addThoughtsTo(message, EMOJI_THINKING);
					
					if (OWStats != null) {
						Runnable getRank = () -> {
							try {
								OWStats.fetchPlayer(id);
								addThoughtsTo(message, EMOJI_ACCEPT);
								channel.sendMessage("Successfully added " + formatted
										+ " to the queue").queue();
							} catch (IOException e) {
								addThoughtsTo(message, EMOJI_REJECT);
								channel
										.sendMessage(
												"Something went wrong and we couldn't find "
														+ formatted
														+ "'s rank ... \n(cAsE sEnSitIvE)")
										.queue();
								e.printStackTrace();
							}
						};
						new Thread(getRank).start();
					} else {
						addThoughtsTo(message, EMOJI_UNSURE);
						channel
								.sendMessage(
										"OverwatchStats is offline. Please enter your rank after your battletag")
								.queue();
						return;
					}
				}
				if (players.contains(OWStats.getIndex().getPlayer(id))) {
					addThoughtsTo(message, EMOJI_REJECT);
					channel.sendMessage("You are already in queue").queue();
					return;
				} else {
					channel.sendMessage("You are not in the queue ???").queue();
					return;
				}
			} else if (content.length == 1) {
				// No battlenet tag given
				addThoughtsTo(message, EMOJI_UNSURE);
				channel.sendMessage("Please include your Battlenet tag. ("
						+ CALLSIGN + "r *user*#*1234*)").queue();
				return;
			}
		}

		if (cmd(content, "remove")) {
			if (content.length != 2) {
				addThoughtsTo(message, EMOJI_UNSURE);
				channel.sendMessage("Please include a battlenet tag to remove. ("
						+ CALLSIGN + "remove *battlenet tag*)").queue();
			}
			String formatted = formatBattlenet(content[1]);
			for (Player p : players) {
				if (p.getId().getTag().equalsIgnoreCase(formatted)) {
					players.remove(p);
					addThoughtsTo(message, EMOJI_ACCEPT);
					channel.sendMessage(
							"Successfully removed " + content[1] + " from the queue.")
							.queue();
					return;
				}
			}
			addThoughtsTo(message, EMOJI_REJECT);
			channel.sendMessage(content[1] + " is not currently queued ... ")
					.queue();
		}

		if (cmd(content, "scrim")) {
			if (players.size() < 12) {
				addThoughtsTo(message, EMOJI_REJECT);
				channel.sendMessage("Not enough players ... ").queue();
				return;
			}
			
			// Sort the list low to hi
			players.sort((a, b) -> {
				int rankA = a.getEntries().get(0).getData().getRank();
				int rankB = b.getEntries().get(0).getData().getRank();
				return rankA - rankB;
			});

			// Split into scrims
			int extraPlayers = players.size() % 12;
			List<DiscordPlayer> removed = new ArrayList<DiscordPlayer>();
			if (extraPlayers != 0) {
				for (int i = 0; i < extraPlayers; i++) {
					// I really shouldn't modify the players list
					// But oh well I'll just keep it for now
					int r = (int) (Math.random() * players.size());
					DiscordPlayer p = players.remove(r);
					removed.add(p);
				}
			}

			List<Scrim> scrims = new ArrayList<Scrim>();
			scrims.add(new Scrim(Region.NA, Platform.PC));
			for (Player p : players) {
				// Current Working Scrim
				Scrim cws = scrims.get(scrims.size() - 1);
				if (cws.isFull()) {
					cws = new Scrim(REGION, PLATFORM);
					scrims.add(cws);
				}
				cws.register(p);
			}
			for (Scrim s : scrims) {
				s.makeTeams();
			}

			// Print out scrims
			StringBuilder sb = new StringBuilder();
			sb.append("```Markdown");

			int longestNameLength = -1;
			for (Player p : players) {
				int challenger = p.getId().getTag().length();
				if (challenger > longestNameLength) {
					longestNameLength = challenger;
				}
			}

			for (int s = 0; s < scrims.size(); s++) {
				Player[] teamA = scrims.get(s).getTeamA();
				Player[] teamB = scrims.get(s).getTeamB();
				sb.append("\n#Group " + (s + 1) + ":\n");
				for (int i = 0; i < Scrim.MAX_TEAM_SIZE; i++) {
					String pATag = teamA[i].getId().getTag();
					// int pARank = teamA[i].getEntries().get(0).getData().getRank();
					// //Player A rank
					String pBTag = teamB[i].getId().getTag();
					// int pBRank = teamB[i].getEntries().get(0).getData().getRank();
					// //Player B rank
					sb.append(String.format("%" + longestNameLength + "s", pATag));
					sb.append(String.format(" | ", pBTag));
					sb.append(pBTag);
					sb.append("\n");
				}
			}
			if (removed.size() != 0) {
				sb.append("\n#Sitting Out:\n*These guys are picked at random*\n");
				for (int i = 0; i < removed.size() - 1; i++) {
					sb.append(removed.get(i).getId().getTag() + ", ");
				}
				sb.append(removed.get(removed.size() - 1).getId().getTag());

				// time to fix what shouldn't be broken in the first place
				for (DiscordPlayer p : removed) {
					players.add(p);
				}

			}
			sb.append("```");

			channel.sendMessage(sb.toString()).queue();
			// Done Successfully!
			addThoughtsTo(message, EMOJI_ACCEPT);
			return;
		}

		// DEBUG COMMANDS

		if (cmd(content, "add")) {
			for (int i = 0; i < Integer.parseInt(content[1]); i++) {
				DiscordPlayer p = new DiscordPlayer(new PlayerId(
						"p" + (int) (Math.random() * 123), REGION, PLATFORM), "bot" + Math.random());
				Entry e = new Entry(new PlayerData());
				e.getData().setRank((int) (Math.random() * 5000));
				p.getEntries().add(e);
				players.add(p);
			}
			addThoughtsTo(message, EMOJI_ACCEPT);
			return;
		}

		if (cmd(content, "print")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < players.size() - 1; i++) {
				sb.append(players.get(i).getId().getTag() + ", ");
			}
			sb.append(players.get(players.size() - 1).getId().getTag());
			addThoughtsTo(message, EMOJI_ACCEPT);
			channel.sendMessage(sb.toString()).queue();
			return;
		}

		// Didn't detect a command
		addThoughtsTo(message, EMOJI_UNSURE);
		channel.sendMessage("Sorry, I'm not quite sure what you mean ...")
				.queue();
	}

	/**
	 * Remove a player from queue if he is anything but OnlineStatus.Online
	 */
	@Override
	public void	onUserOnlineStatusUpdate(UserOnlineStatusUpdateEvent event) {
		if(event.getPreviousOnlineStatus() == OnlineStatus.ONLINE) {
			if(remove(event.getUser()) != null) {
				PrivateChannel pm = event.getUser().openPrivateChannel().complete();
				pm.sendMessage("Since you were afk, we removed you from the queue.").queue();
				pm.close();
			}
		}
	}
	
	private DiscordPlayer remove(String Id) {
		for(DiscordPlayer p : players) {
			if(p.getDiscordId().equals(Id)) {
				players.remove(p);
				return p;
			}
		}
		return null;
	}
	
	private DiscordPlayer remove(User user) {
		return remove(user.getId());
	}
	
	private void addThoughtsTo(Message message, String emoji) {
		/*
		 * //Remove my thought if I already had one String id = message.getId();
		 * MessageChannel channel = message.getTextChannel(); message =
		 * channel.getMessageById(id).complete();
		 * 
		 * List<MessageReaction> reactions = message.getReactions();
		 * for(MessageReaction r : reactions) { if(r.isSelf()) r.removeReaction();
		 * }
		 */
		// Add my new thoughts!
		message.addReaction(emoji).queue();
	}

	private String formatBattlenet(String id) {
		return id.replace('#', '-');
	}

	private boolean cmd(String[] cnt, String command) {
		return cnt[0].equalsIgnoreCase(CALLSIGN + command);
	}

}
