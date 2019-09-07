package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import appguru.FREE6.db.GuildStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.HashMap;
import java.util.Iterator;

import static appguru.FREE6.chat.Bot.DISCORD_COLOR;

public class ResetCommand extends Command {

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
        return 0;
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Leaderboard Reset");
        eb.setColor(DISCORD_COLOR);
        GuildStorage gs = GuildStorage.get(e.getGuild().getIdLong());
        Long confirmation = gs.awaitingConfirmation;
        if (confirmation == null || System.currentTimeMillis() - confirmation > 30000) {
            gs.awaitingConfirmation = System.currentTimeMillis();
            eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f501.png");
            eb.setDescription("Are you sure you want to reset the entire leaderboard ? Call this command again within the next 30s to confirm.");
        } else {
            gs.reset();
            Iterator<Member> members = e.getGuild().getMemberCache().iterator();
            Long rewID = gs.getRoleReward(0);
            Role newRole = null;
            if (rewID != null) {
                newRole = e.getGuild().getRoleById(rewID);
            }
            if (newRole == null) {
                while (members.hasNext()) {
                    Member m = members.next();
                    if (!m.getUser().isBot()) {
                        for (Role r : m.getRoles()) {
                            if (gs.isRoleReward(r.getIdLong())) {
                                try {
                                    e.getGuild().removeRoleFromMember(m, r).queue();
                                } catch (HierarchyException ex) {}
                            }
                        }
                    }
                }
            } else {
                while (members.hasNext()) {
                    Member m = members.next();
                    if (!m.getUser().isBot()) {
                        Bot.setRankRole(e.getGuild(), gs, m, newRole);
                    }
                }
            }
            eb.setThumbnail("https://twemoji.maxcdn.com/36x36/2705.png");
            eb.setDescription("Leaderboard resetted successfully.");
        }
        e.getChannel().sendMessage(eb.build()).queue();
    }
}
