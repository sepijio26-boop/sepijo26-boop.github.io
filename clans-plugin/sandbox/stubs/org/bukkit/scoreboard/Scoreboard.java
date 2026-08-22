package org.bukkit.scoreboard;
public interface Scoreboard {
    Team getTeam(String name);
    Team registerNewTeam(String name);
}
