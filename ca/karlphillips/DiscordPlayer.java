package ca.karlphillips;

import com.simdezimon.overwatch.Player;
import com.simdezimon.overwatch.PlayerId;

import net.dv8tion.jda.core.entities.User;

public class DiscordPlayer extends Player {

	private String discordId;
	
	public DiscordPlayer() {
		super();
	}

	public DiscordPlayer(PlayerId id, String discordId) {
		super(id);
		this.discordId = discordId;
	}
	
	public DiscordPlayer(Player player, String discordId) {
		super(player.getId());
		this.discordId = discordId;
	}

	public DiscordPlayer(PlayerId id, User user) {
		super(id);
		this.discordId = user.getId();
	}
	
	public String getDiscordId() {
		return discordId;
	}
}
