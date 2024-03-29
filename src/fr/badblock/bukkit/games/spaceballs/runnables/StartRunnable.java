package fr.badblock.bukkit.games.spaceballs.runnables;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.badblock.bukkit.games.spaceballs.PluginSB;
import fr.badblock.bukkit.games.spaceballs.configuration.SpaceMapConfiguration;
import fr.badblock.bukkit.games.spaceballs.players.SpaceScoreboard;
import fr.badblock.gameapi.GameAPI;
import fr.badblock.gameapi.players.BadblockPlayer;
import fr.badblock.gameapi.utils.i18n.TranslatableString;
import fr.badblock.gameapi.utils.i18n.messages.GameMessages;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StartRunnable extends BukkitRunnable {
	public    static final int 		     TIME_BEFORE_START = 60;
	protected static 	   StartRunnable task 		       = null;
	public    static 	   GameRunnable  gameTask		   = null;

	public static int time = TIME_BEFORE_START;

	@Override
	public void run() {
		GameAPI.setJoinable(time >= 5);
		if(time == 0){
			for(Player player : Bukkit.getOnlinePlayers()){
				BadblockPlayer bPlayer = (BadblockPlayer) player;
				bPlayer.playSound(Sound.ORB_PICKUP);
			}

			String winner = GameAPI.getAPI().getBadblockScoreboard().getWinner().getInternalName();
			File   file   = new File(PluginSB.MAP, winner + ".json");

			SpaceMapConfiguration config = new SpaceMapConfiguration(GameAPI.getAPI().loadConfiguration(file));
			config.save(file);
			PluginSB.getInstance().setMapConfiguration(config);
			GameAPI.getAPI().balanceTeams(true);

			gameTask = new GameRunnable(config);
			gameTask.runTaskTimer(GameAPI.getAPI(), 0, 20L);

			cancel();
		} else if(time % 10 == 0 || time <= 5){
			sendTime(time);
		}

		if(time == 3){
			GameAPI.getAPI().getBadblockScoreboard().endVote();

			for(Player player : Bukkit.getOnlinePlayers()){
				new SpaceScoreboard((BadblockPlayer) player);
			}
		}

		sendTimeHidden(time);

		time--;
	}

	protected void start(){
		sendTime(time);

		runTaskTimer(GameAPI.getAPI(), 0, 20L);
	}

	private void sendTime(int time){
		ChatColor color = getColor(time);
		TranslatableString title = GameMessages.startIn(time, color);

		for(Player player : Bukkit.getOnlinePlayers()){
			BadblockPlayer bPlayer = (BadblockPlayer) player;

			bPlayer.playSound(Sound.NOTE_PLING);
			bPlayer.sendTranslatedTitle(title.getKey(), title.getObjects());
			bPlayer.sendTimings(2, 30, 2);
		}
	}

	private void sendTimeHidden(int time){
		ChatColor color = getColor(time);
		TranslatableString actionbar = GameMessages.startInActionBar(time, color);

		for(Player player : Bukkit.getOnlinePlayers()){
			BadblockPlayer bPlayer = (BadblockPlayer) player;

			if(time > 0)
				bPlayer.sendTranslatedActionBar(actionbar.getKey(), actionbar.getObjects());
			bPlayer.setLevel(time);
			bPlayer.setExp(0.0f);
		}
	}

	private ChatColor getColor(int time){
		if(time == 1)
			return ChatColor.DARK_RED;
		else if(time <= 5)
			return ChatColor.RED;
		else return ChatColor.AQUA;
	}

	public static void joinNotify(int currentPlayers, int maxPlayers){
		if ((!GameAPI.getAPI().isHostedGame() && currentPlayers + 1 < PluginSB.getInstance().getConfiguration().maxPlayersInTeam)
				|| (GameAPI.getAPI().isHostedGame() && currentPlayers + 1 < PluginSB.getInstance().getMaxPlayers())) return;

		startGame(false);
		int a = time - (TIME_BEFORE_START / Bukkit.getMaxPlayers());
		if (time >= 30 && (a <= 30 || Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers())) time = 30;
		else if (time >= 30) time = a;
	}

	public static void startGame(boolean force){
		if(task == null){
			task = new StartRunnable();
			task.start();
		}
	}

	public static void stopGame(){
		if(gameTask != null){
			gameTask.forceEnd = true;
			time = TIME_BEFORE_START;
		} else if(task != null){
			task.cancel();
			time = time > 120 ? time : 120;
			GameAPI.setJoinable(true);
		}

		task = null;
		gameTask = null;
	}

	public static boolean started(){
		return task != null;
	}
}
