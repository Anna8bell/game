package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;

import java.util.List;
import java.util.Map;

public interface PlayerService {

    List<Player> getFilteredPlayers(String name, String title, Race race, Profession profession,
                                    Long after, Long before, Boolean banned, Integer minExperience,
                                    Integer maxExperience, Integer minLevel, Integer maxLevel);


    int getCount(String name, String title, Race race, Profession profession,
                 Long after, Long before, Boolean banned, Integer minExperience,
                 Integer maxExperience, Integer minLevel, Integer maxLevel);

    List<Player> getSortedPlayers(List<Player> players, Integer pageNumber, Integer pageSize, PlayerOrder playerOrder);

    Player getPlayer(String id);

    Player createNewPlayer(Player player);

    Player updatePlayer(String id, Map<String,String> request);

    void delete(String id);
}
