package appguru.FREE6.commands;

import appguru.FREE6.Main;
import appguru.FREE6.chat.Bot;
import static appguru.FREE6.chat.Bot.DISCORD_COLOR;
import static appguru.FREE6.chat.Bot.setRankRole;
import appguru.FREE6.db.GuildStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ImportCommand extends Command {

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[] {Permission.MANAGE_SERVER};
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        String id = e.getGuild().getId();
        if (args.length == 1) {
            String arg = args[0];
            if (arg.startsWith("<") && arg.endsWith(">")) {
                arg = arg.substring(1, arg.length() - 1);
            }
            if (arg.startsWith("https://mee6.xyz/leaderboard/")) {
                String candidate = arg.substring("https://mee6.xyz/leaderboard/".length());
                if (RoleCommand.isValidID(candidate)) {
                    id = candidate;
                } else {
                    b.errorReply(e, "No valid leaderboard URL given.");
                    return;
                }
            } else {
                if (RoleCommand.isValidID(arg)) {
                    id = arg;
                } else {
                    b.errorReply(e, "No valid Guild ID given.");
                    return;
                }
            }
        }
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("XP Import", null, null);
        eb.setThumbnail("https://twemoji.maxcdn.com/36x36/2b07.png");
        eb.setColor(DISCORD_COLOR);
        eb.setDescription("Importing MEE6 XP from [leaderboard](https://mee6.xyz/leaderboard/" + id + ")...");
        
        final String final_id = id;
        final long started = System.currentTimeMillis();
        
        e.getChannel().sendMessage(eb.build()).queue(message -> {
            GuildStorage gs = GuildStorage.get(e.getGuild().getIdLong());
            String base_url = "https://mee6.xyz/api/plugins/levels/leaderboard/" + final_id + "?page=";
            int page = 0;
            int members_imported = 0;
            while (true) {
                // Timeout
                if (System.currentTimeMillis() - started > 30 * 60 * 1000) {
                    Main.LOGGER.info("Import failed : Timeout");
                    eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f6ab.png");
                    eb.setDescription("Importing MEE6 XP from [leaderboard](https://mee6.xyz/leaderboard/" + final_id + ") failed : Timed out (took longer than 30 min). Please try again later.");
                    message.editMessage(eb.build()).queue();
                    return;
                }

                try {
                    URL in = new URL(base_url + page);
                    String leaderboard_page_json = new String(in.openStream().readAllBytes());
                    Map<String, Object> leaderboard_page = new ObjectMapper().readValue(leaderboard_page_json, new TypeReference<Map<String, Object>>() {});
                    Object member_object;
                    if ((member_object = leaderboard_page.get("players")) == null) {
                        break;
                    } else {
                        List members = (List) member_object;
                        if (members.isEmpty()) {
                            break;
                        }
                        for (Object member:members) {
                            Map attributes = (Map) member;
                            Object member_id, member_xp;
                            if ((member_id = attributes.get("id")) != null && (member_xp = attributes.get("xp")) != null) {
                                String m_id = (String) member_id;
                                double m_xp = ((Number)member_xp).doubleValue();
                                try {
                                    Member d_member = e.getGuild().getMemberById(m_id);
                                    if (d_member != null) {
                                        gs.setScore(Long.parseUnsignedLong(m_id), m_xp);
                                        int new_level = GuildStorage.getLevel(m_xp);
                                        Long rew = gs.getRoleReward(new_level);
                                        if (rew != null) {
                                            Role reward = e.getGuild().getRoleById(rew);
                                            if (reward != null) {
                                                setRankRole(e.getGuild(), gs, d_member, reward);
                                            }
                                        }
                                        members_imported++;
                                    }
                                } catch (Exception ex) {}
                            }
                        }
                        if (members.size() < 100) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    Main.LOGGER.info("Import failed : ", ex);
                    eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f6ab.png");
                    eb.setDescription("Importing MEE6 XP from [leaderboard](https://mee6.xyz/leaderboard/" + final_id + ") failed : An unexpected error occurred. Make sure the leaderboard exists and try again later.");
                    message.editMessage(eb.build()).queue();
                    return;
                }
                page++;
            }
            eb.setThumbnail("https://twemoji.maxcdn.com/36x36/2705.png");
            eb.setDescription("Importing MEE6 XP from [leaderboard](https://mee6.xyz/leaderboard/" + final_id + ") finished. " + 
                    (members_imported == 1 ? "1 member has been imported.":members_imported + " members have been imported."));
            message.editMessage(eb.build()).queue();
        });
    }
    
}
