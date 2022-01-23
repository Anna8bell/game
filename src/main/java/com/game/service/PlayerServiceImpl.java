package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exceptions.BadRequestException;
import com.game.exceptions.NotFoundException;
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
        if (
            // Без значений
                player.getName() == null
                        || player.getTitle() == null
                        || player.getRace() == null
                        || player.getProfession() == null
                        || player.getBirthday() == null
                        || player.getExperience() == null

                        // Условия
                        || player.getTitle().length() > 30
                        || player.getName().length() > 12
                        || player.getName().equals("")
                        || player.getExperience() < 0
                        || player.getExperience() > 10000000
                        || player.getBirthday().getTime() < 0
                        || player.getBirthday().before(new Date(946684800000L))
                        || player.getBirthday().after(new Date(32503680000000L))
        )
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        player.setLevel((int) (Math.sqrt((double) 2500 + 200 * player.getExperience()) - 50) / 100);
        player.setUntilNextLevel(50 * (player.getLevel() + 1) * (player.getLevel() + 2) - player.getExperience());

        return repository.save(player);
    }

    @Override
    public Player getPlayer(String id) {
        Long newId;
        try {
            newId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!validId(newId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (repository.existsById(newId)) {
            return repository.findById(newId).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public Boolean validId(Long id) {
        return id > 0;
    }

    @Override
    public Player updatePlayer(String id, Map<String, String> request) {
        long parseId = getValidId(id);
        Player player = getPlayer(id);
        //создание request для метода creationWithValidation
        //begin
        String name = request.get("name");
        if (name == null) {request.put("name", player.getName());}

        String title = request.get("title");
        if (title == null) {request.put("title",player.getTitle());}

        String race = request.get("race");
        if (race == null) {request.put("race",player.getRace().toString());}

        String profession = request.get("profession");
        if (profession == null ) {request.put("profession",player.getProfession().toString());}

        String birthday = request.get("birthday");
        if (birthday == null) {
            request.put("birthday", ( (Long) player.getBirthday().getTime()).toString());
        }

        String banned = request.get("banned");
        if (banned == null) {request.put("banned",((Boolean) player.isBanned()).toString());}

        String experience = request.get("experience");
        if (experience == null) {
            request.put("experience",((Integer) player.getExperience()).toString());
        }
        //end
        player = creationWithValidation(request);
        player.setId(parseId);
        repository.save(player);
        return player;
    }

    private Player creationWithValidation(Map<String,String> request) {
        String name = request.get("name");
        String title = request.get("title");
        String race = request.get("race");
        String profession = request.get("profession");
        String birthday = request.get("birthday");
        String experience = request.get("experience");
        String banned = request.get("banned");
        // проверка на нули
        if (name != null && title != null && race != null && profession != null && birthday != null && experience != null) {
            Player player = new Player();
            // проверка длины имени 12 (не пустая строка)
            if (name.length()<=12 && !name.trim().equals("")) {
                player.setName(name);
            } else throw new BadRequestException(); //except
            // проверка длины тайтла 30
            if (title.length()<=30) {
                player.setTitle(title);
            } else throw new BadRequestException(); //except
            // опыт 0..10_000_000
            int exp = Integer.parseInt(experience);
            if (exp>=0 && exp<=10_000_000) {
                player.setExperience(exp);
                int lvl =getLvl(exp);
                player.setLevel(lvl);
                int untilNextLvl = getUntilNextLvl(lvl,exp);
                player.setUntilNextLevel(untilNextLvl);
            } else throw new BadRequestException(); //except
            // birthday > [Long] 0 and [2000..3000y]
            long brthday = Long.parseLong(birthday);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(brthday);
            Calendar afterBorder = new GregorianCalendar(2000,0,0);
            Calendar beforeBorder = new GregorianCalendar(3000,0,0);
            if (brthday > 0 && cal.after(afterBorder) && cal.before(beforeBorder)) {
                player.setBirthday(cal.getTime());
            } else throw new BadRequestException(); //except
            // banned
            if (banned!=null) {
                boolean isBanned = Boolean.parseBoolean(banned);
                player.setBanned(isBanned);
            }
            Race playerRace = Race.valueOf(race);
            player.setRace(playerRace);
            Profession playerProfession = Profession.valueOf(profession);
            player.setProfession(playerProfession);
            return player;
        } else throw new BadRequestException();
    }

    private int getLvl(int exp) {
        return (int) (Math.sqrt(2500+200*exp)-50)/100;
    }

    private int getUntilNextLvl(int lvl, int exp) {
        return 50*(lvl+1)*(lvl+2)-exp;
    }

    private boolean isValidExperience(Integer experience) {
        return experience >= 0 && experience <= 10000000;
    }

    private boolean isValidDate(Date date) {
        if (date == null) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(1999, 11, 31);
        Date after = calendar.getTime();
        calendar.set(3000, 11, 31);
        Date before = calendar.getTime();

        return  (date.before(before) && date.after(after));
    }

    @Override
    public void delete(String id) {
        repository.delete(getPlayer(id));
    }

    private long getValidId(String id) {
        long parseId;
        try {
            parseId = Long.parseLong(id);
        } catch (Exception e) {
            throw new BadRequestException();
        }
        if (parseId<=0) throw new BadRequestException();
        return parseId;
    }

}
