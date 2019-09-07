package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import appguru.FREE6.db.GuildStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import redis.clients.jedis.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static appguru.FREE6.chat.Bot.DISCORD_COLOR;

public class RoleCommand extends Command {
    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[0];
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    public static boolean isValidID(String s) {
        if (s.length() == 0) {
            return false;
        }
        if (s.charAt(0) == '0') {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    public static EmbedBuilder getEmbedBuilder() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DISCORD_COLOR);
        eb.setTitle("Role rewards");
        eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f386.png");
        return eb;
    }

    public static void buildList(GuildStorage gs, EmbedBuilder eb) {
        Set<Tuple> rewards = gs.listRoleRewards();
        if (rewards.isEmpty()) {
            eb.setDescription("No role rewards set.");
        } else {
            eb.appendDescription("Level / rewarded role\n");
            for (Tuple t : rewards) {
                eb.appendDescription("**" + ((int) t.getScore()) + "** - <@&" + Long.toUnsignedString(GuildStorage.byteArrayToLong(t.getBinaryElement())) + ">\n");
            }
        }
    }

    public static void buildReward(GuildStorage gs, int level, EmbedBuilder eb) {
        long role_id = gs.getRoleReward(level);
        eb.setDescription("Role reward for level " + level + " is <@&" + Long.toUnsignedString(role_id) + ">.");
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        byte[] payload = null;
        GuildStorage gs = GuildStorage.get(e.getGuild().getIdLong());
        EmbedBuilder eb = getEmbedBuilder();
        if (args.length == 0) {
            payload = new byte[0];
            buildList(gs, eb);
        } else {
            int level;
            try {
                level = Integer.parseInt(args[0]);
            } catch (Exception ex) {
                b.errorReply(e, "No valid level given : Needs to be a positive integer up to " + GuildStorage.LEVELS.length + ".");
                return;
            }
            if (args.length == 2) {
                Role chosen_role = null;
                if (level < 0 || level > GuildStorage.LEVELS.length) {
                    b.errorReply(e, "Level out of range : Needs to be a positive integer up to " + GuildStorage.LEVELS.length + ".");
                    return;
                }
                String id = args[1];
                if (args[1].startsWith("<@&") && args[1].endsWith(">")) {
                    id = args[1].substring(3, args[1].length() - 1);
                }
                if (!isValidID(id) || (chosen_role = e.getGuild().getRoleById(id)) == null) {
                    List<Role> roles = e.getGuild().getRolesByName(args[1], true);
                    if (roles.size() == 0) {
                        b.errorReply(e, "No roles named `" + args[1] + "` found.");
                        return;
                    } else {
                        if (roles.size() > 1) {
                            for (Role r : roles) {
                                if (r.equals(args[1])) {
                                    if (chosen_role == null) {
                                        chosen_role = r;
                                    } else {
                                        chosen_role = null;
                                        break;
                                    }
                                }
                            }
                        } else {
                            chosen_role = roles.get(0);
                        }

                        if (chosen_role == null) {
                            b.errorReply(e, "Multiple roles possible. Please use the mention or ID.");
                            return;
                        }
                    }
                }
                eb.setDescription("Role reward for level " + level + " was successfully set to " + chosen_role.getAsMention() + ".");
                eb.setThumbnail("https://twemoji.maxcdn.com/36x36/2705.png");
                gs.setRoleReward(level, chosen_role.getIdLong());
                e.getChannel().sendMessage(eb.build()).queue();
                // Updating all users with a score ranging from this level to the next one
                Iterator<byte[]> affected = gs.getAffectedMembers(level);
                while (affected.hasNext()) {
                    Member m = e.getGuild().getMemberById(GuildStorage.byteArrayToLong(affected.next()));
                    if (m != null) {
                        Bot.setRankRole(e.getGuild(), gs, m, chosen_role);
                    }
                }
                return;
            } else {
                payload = new byte[] {(byte)(level & 0xFF)};
                buildReward(gs, level, eb);
            }
        }
        final byte[] pload = payload; // payload is final
        e.getChannel().sendMessage(eb.build()).queue(m -> {
            if (pload != null) {
                gs.setEmbed(m.getChannel().getIdLong(), m.getIdLong(), pload);
            }
            m.addReaction("\uD83D\uDD04").queue();
        });
    }
}
