package com.game.controller;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest")
public class PlayerController {

    private final PlayerService service;

    @Autowired
    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping("/players")
    public List<Player> getPlayersWithParam (@RequestParam(value = "name", required = false) String name,
                                             @RequestParam(value = "title", required = false) String title,
                                             @RequestParam(value = "race", required = false) Race race,
                                             @RequestParam(value = "profession", required = false) Profession profession,
                                             @RequestParam(value = "after", required = false) Long afterDate,
                                             @RequestParam(value = "before", required = false) Long beforeDate,
                                             @RequestParam(value = "banned", required = false) Boolean banned,
                                             @RequestParam(value = "minExperience", required = false) Integer minExperience,
                                             @RequestParam(value = "maxExperience", required = false) Integer maxExperience,
                                             @RequestParam(value = "minLevel", required = false) Integer minLevel,
                                             @RequestParam(value = "maxLevel", required = false) Integer maxLevel,
                                             @RequestParam(defaultValue = "0", value = "pageNumber") Integer pageNumber,
                                             @RequestParam(defaultValue = "3", value = "pageSize") Integer pageSize,
                                             @RequestParam(defaultValue = "ID", value = "playerOrder") PlayerOrder playerOrder) {
        List<Player> players = service.getFilteredPlayers(name,title,race,profession,
                afterDate,beforeDate,banned,minExperience, maxExperience,
                minLevel,maxLevel);
        return service.getSortedPlayers(players,pageNumber,pageSize, playerOrder);
    }


    @GetMapping("/players/count")
    public int getPlayersCount(@RequestParam(value="name", required = false) String name,
                               @RequestParam(value = "title", required = false) String title,
                               @RequestParam(value = "race", required = false) Race race,
                               @RequestParam(value = "profession", required = false) Profession profession,
                               @RequestParam(value = "after", required = false) Long after,
                               @RequestParam(value = "before", required = false) Long before,
                               @RequestParam(value = "banned", required = false) Boolean banned,
                               @RequestParam(value = "minExperience", required = false) Integer minExperience,
                               @RequestParam(value = "maxExperience", required = false) Integer maxExperience,
                               @RequestParam(value = "minLevel", required = false) Integer minLevel,
                               @RequestParam(value = "maxLevel", required = false) Integer maxLevel) {
        return service.getCount(name,title,race,profession,after,before,banned,
                minExperience,maxExperience,minLevel,maxLevel);
    }


    @PostMapping("/players")
    public Player createPlayer(@RequestBody Player player) {
        return service.createNewPlayer(player);
    }


    @GetMapping("/players/{id}")
    public Player getPlayerByID(@PathVariable("id") String id) {
        return service.getPlayer(id);
    }

    @PostMapping("/players/{id}")
    public Player update (@PathVariable("id") String id,
                          @RequestBody Map<String,String> request) {
        return service.updatePlayer(id,request);
    }

    @DeleteMapping("/players/{id}")
    public void delete (@PathVariable(value = "id") String id) {
        service.delete(id);
    }

}
