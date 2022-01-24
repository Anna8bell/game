package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exceptions.BadRequestException;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class PlayerServiceImpl implements PlayerService {
    private PlayerRepository repository;

    @Autowired
    public PlayerServiceImpl(PlayerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Player> getFilteredPlayers(String name, String title, Race race, Profession profession,
                                           Long after, Long before, Boolean banned, Integer minExperience,
                                           Integer maxExperience, Integer minLevel, Integer maxLevel) {
        List<Player> result = new ArrayList<>();
        final Date afterDate = after == null ? null : new Date(after);
        final Date beforeDate = before == null ? null : new Date(before);

        repository.findAll().forEach(player -> {
            if (name!=null && !player.getName().contains(name)) return;
            if (title!=null && !player.getTitle().contains(title)) return;
            if (race != null && player.getRace() != race) return;
            if (profession != null && player.getProfession() != profession) return;
            if (after != null && player.getBirthday().before(afterDate)) return;
            if (before != null && player.getBirthday().after(beforeDate)) return;
            if (banned != null && player.isBanned() != banned) return;
            if (minExperience != null && player.getExperience().compareTo(minExperience) < 0) return;
            if (maxExperience != null && player.getExperience().compareTo(maxExperience) > 0) return;
            if (minLevel != null && player.getLevel().compareTo(minLevel) < 0) return;
            if (maxLevel != null && player.getLevel().compareTo(maxLevel) > 0) return;
            result.add(player);
        });
        return result;
    }



    @Override
    public List<Player> getSortedPlayers(List<Player> players, Integer pageNumber, Integer pageSize, PlayerOrder playerOrder) {
        int pageNum = pageNumber + 1;
        int count = pageSize;
        List<Player> sortedPlayers = new ArrayList<>();
        if (playerOrder.equals(PlayerOrder.NAME))
            players.sort(Comparator.comparing(Player::getName));
        else if (playerOrder.equals(PlayerOrder.EXPERIENCE))
            players.sort(Comparator.comparing(Player::getExperience));
        else if (playerOrder.equals(PlayerOrder.BIRTHDAY))
            players.sort(Comparator.comparing(Player::getBirthday));
        for (int i = pageNum * count - (count - 1) - 1; i < count * pageNum && i < players.size(); i++) {
            sortedPlayers.add(players.get(i));
        }
        return sortedPlayers;
    }

    @Override
    public int getCount(String name, String title, Race race, Profession profession,
                        Long after, Long before, Boolean banned, Integer minExperience,
                        Integer maxExperience, Integer minLevel, Integer maxLevel) {
        return getFilteredPlayers(name, title, race, profession, after, before, banned,
                minExperience, maxExperience, minLevel, maxLevel).size();
    }

    @Override
    public Player createNewPlayer(Player player) {
        if (!validNewPlayer(player)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        player.setLevel((int) (Math.sqrt((double) 2500 + 200 * player.getExperience()) - 50) / 100);
        player.setUntilNextLevel(50 * (player.getLevel() + 1) * (player.getLevel() + 2) - player.getExperience());
        return repository.save(player);
    }

    private Boolean validNewPlayer(Player player) {
        return player.getName() != null
                && player.getTitle() != null
                && player.getRace() != null
                && player.getProfession() != null
                && player.getBirthday() != null
                && player.getExperience() != null

                && isValidTitle(player.getTitle())
                && isValidName(player.getName())
                && isValidExperience(player.getExperience())
                && isValidDate(toCalendar(player.getBirthday()));
    }

    @Override
    public Player getPlayer(String id) {
        long newId = getValidId(id);
        if (repository.existsById(newId)) {
            return repository.findById(newId).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Player updatePlayer(String id, Map<String, String> request) {
        long parseId = getValidId(id);
        Player player = getPlayer(id);

        if (request.get("name") == null) {
            request.put("name", player.getName());
        }
        String name = request.get("name");

        if (request.get("title") == null) {
            request.put("title",player.getTitle());
        }
        String title = request.get("title");

        if (request.get("race") == null) {
            request.put("race",player.getRace().toString());
        }
        String race = request.get("race");

        if (request.get("profession") == null ) {
            request.put("profession",player.getProfession().toString());
        }
        String profession = request.get("profession");


        if (request.get("birthday") == null) {
            request.put("birthday", ((Long)player.getBirthday().getTime()).toString());
        }
        String birthday = request.get("birthday");

        if (request.get("banned") == null) {
            request.put("banned",(player.isBanned()).toString());
        }
        String banned = request.get("banned");

        if (request.get("experience") == null) {
            request.put("experience",(player.getExperience()).toString());
        }
        String experience = request.get("experience");


        // проверка на нули
        if (name == null && title == null && race == null && profession == null && birthday == null && experience == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // name
        if (isValidName(name)) {
            player.setName(name);
        } else throw new BadRequestException();

        // title
        if (isValidTitle(title)) {
            player.setTitle(title);
        } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        // exp and level
        int exp = Integer.parseInt(experience);
        if (isValidExperience(exp)) {
            player.setExperience(exp);
            int lvl = getLvl(exp);
            player.setLevel(lvl);
            int untilNextLvl = getUntilNextLvl(lvl,exp);
            player.setUntilNextLevel(untilNextLvl);
        } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        // birthday
        long birthdayLong = Long.parseLong(birthday);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(birthdayLong);
        if (isValidDate(cal)) {
            player.setBirthday(cal.getTime());
        } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        // banned
        if (banned != null) {
            boolean isBanned = Boolean.parseBoolean(banned);
            player.setBanned(isBanned);
        } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        player.setRace(Race.valueOf(race));
        player.setProfession(Profession.valueOf(profession));
        player.setId(parseId);
        repository.save(player);
        return player;
    }

    private long getValidId(String id) {
        long parseId;
        try {
            parseId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!isValidId(parseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return parseId;
    }


    private int getLvl(int exp) {
        return (int) (Math.sqrt(2500+200*exp)-50)/100;
    }

    private int getUntilNextLvl(int lvl, int exp) {
        return 50*(lvl+1)*(lvl+2)-exp;
    }

    private boolean isValidId(Long id) {
        return id > 0;
    }

    private boolean isValidName(String name) {
        return name.length() <= 12 && !name.trim().equals("");
    }

    private boolean isValidTitle(String title) {
        return title.length() <= 30;
    }

    private boolean isValidExperience(Integer experience) {
        return experience >= 0 && experience <= 10000000;
    }

    private boolean isValidDate(Calendar date) {
        Calendar afterBorder = new GregorianCalendar(2000,0,0);
        Calendar beforeBorder = new GregorianCalendar(3000,0,0);
        return (date.getTime().getTime() > 0) && date.before(beforeBorder) && date.after(afterBorder);
    }

    private Calendar toCalendar(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }


    @Override
    public void delete(String id) {
        repository.delete(getPlayer(id));
    }

}
