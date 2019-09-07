package appguru.FREE6.chat;

import appguru.FREE6.commands.*;
import appguru.FREE6.db.EmbedData;
import appguru.FREE6.db.GuildStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import redis.clients.jedis.Tuple;

import java.time.Instant;
import java.util.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;

public class Bot extends ListenerAdapter {
    public static final int DISCORD_COLOR = 0x7289DA;
    public static HashSet<String> CONTROL_EMOTES = new HashSet<String>();
    static {
        CONTROL_EMOTES.add("⬅");
        CONTROL_EMOTES.add("🔄");
        CONTROL_EMOTES.add("➡");
        CONTROL_EMOTES.add("🏆");
        for (char c = '1'; c <= '9'; c++) {
            CONTROL_EMOTES.add(c + "\u20E3");
        }
        CONTROL_EMOTES.add("🔟");
    }

    public static final String README_URL = "https://github.com/appgurueu/FREE6";
    public String prefix;
    public HashMap<String, Command> commands=new HashMap();

    public Bot() {
        prefix = ">>";
    }

    public void errorReply(MessageReceivedEvent e, String error) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DISCORD_COLOR);
        eb.setThumbnail("https://twemoji.maxcdn.com/36x36/1f6ab.png");
        eb.setTitle("Error");
        eb.setDescription(error);
        e.getChannel().sendMessage(eb.build()).queue();
    }

    public static void setRankRole(Guild g, GuildStorage gs, Member m, Role role) {
        int i = 0;
        List<Role> roles = m.getRoles();
        addMissing:
        {
            for (; i < roles.size(); i++) {
                if (roles.get(i).getIdLong() == role.getIdLong()) {
                    break addMissing;
                }
                if (gs.isRoleReward(roles.get(i).getIdLong())) {
                    try {
                        g.removeRoleFromMember(m, roles.get(i)).queue();
                    } catch (HierarchyException ex) {}
                }
            }
            try {
                g.addRoleToMember(m, role).queue();
            } catch (HierarchyException ex) {}
        }
        for (i++; i < roles.size(); i++) {
            if (gs.isRoleReward(roles.get(i).getIdLong())) {
                try {
                    g.removeRoleFromMember(m, roles.get(i)).queue();
                } catch (HierarchyException ex) {}
            }
        }
    }

    public EmbedBuilder registerInfo(String command, String title, String info) {
        EmbedBuilder eb=new EmbedBuilder();
        eb.setTitle(title, README_URL +"#"+title.toLowerCase());
        eb.setDescription(info);
        eb.setColor(DISCORD_COLOR);
        registerCommand(command, new InfoCommand(eb));
        return eb;
    }

    public void registerCommand(String command, Command c) {
        commands.put(command,c);
    }

    @Override
    public void onReady(ReadyEvent event) {
        for (Guild g:event.getJDA().getGuilds()) {
            GuildStorage gs = new GuildStorage(g.getIdLong());
            for (byte[] m_id:gs.getMembers()) {
                long member_id = GuildStorage.byteArrayToLong(m_id);
                if (g.getMemberById(member_id) == null) {
                    gs.removeMember(m_id);
                }
            }
        }
        event.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, false);
        event.getJDA().getPresence().setActivity(Activity.watching("You"));
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (GuildStorage.get(event.getGuild().getIdLong()) == null) {
            new GuildStorage(event.getGuild().getIdLong());
        }
    }
    
    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        GuildStorage.get(event.getGuild().getIdLong()).removeMember(GuildStorage.longToByteArray(event.getMember().getIdLong()));
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        String emoteName = event.getReactionEmote().getName();
        if (!event.getUser().isBot() && CONTROL_EMOTES.contains(emoteName)) {
            GuildStorage gs = GuildStorage.get(event.getGuild().getIdLong());
            byte[] a = gs.getEmbed(event.getChannel().getIdLong(), event.getMessageIdLong());
            if (a == null) {
                return;
            }
            // Information role embed
            if (a.length < 2) {
                if (emoteName.equals("\uD83D\uDD04")) {
                    EmbedBuilder eb = RoleCommand.getEmbedBuilder();
                    if (a.length == 0) {
                        RoleCommand.buildList(gs, eb);
                    } else {
                        RoleCommand.buildReward(gs, Byte.toUnsignedInt(a[0]), eb);
                    }
                    event.getChannel().editMessageById(event.getMessageIdLong(), eb.build()).queue(m -> {
                        m.clearReactions().queue();
                        m.addReaction("\uD83D\uDD04").queue();
                    });
                }
            } else {
                EmbedData data = new EmbedData(a);

                Long count = gs.count();

                if (count == null) {
                    return;
                }

                if (emoteName.equals("⬅")) {
                    data.data -= 10;
                    if (data.type == 1) {
                        return;
                    }
                } else if (emoteName.equals("➡")) {
                    data.data += 10;
                    if (data.type == 1) {
                        return;
                    }
                } else if (emoteName.equals("🏆")) {
                    if (data.type == 0) {
                        return;
                    }
                    data.data = (gs.getRank(data.data) / 10) * 10;
                    data.type = 0;
                } else if (!emoteName.equals("\uD83D\uDD04")) {
                    if (data.type == 1) {
                        return;
                    }
                    int num = emoteName.equals("🔟") ? 9 : (emoteName.charAt(0) - '1');
                    Iterator<Tuple> it = gs.getLeaderboard(data.data + num, data.data + num).iterator();
                    if (!it.hasNext()) {
                        return;
                    }
                    Tuple t = gs.getLeaderboard(data.data + num, data.data + num).iterator().next();
                    if (t == null) {
                        return;
                    }
                    data.type = 1;
                    data.data = GuildStorage.byteArrayToLong(t.getBinaryElement());
                }

                final byte[] de = data.toBytes();
                EmbedBuilder eb;
                if (data.type == 0) {
                    if (data.data >= count) {
                        data.data = 0;
                    } else if (data.data < 0) {
                        data.data = (count < 10) ? 0:(count/10)*10;
                    }
                    eb = LeaderboardCommand.buildLeaderboard(event.getGuild(), gs, data.data, (count+9)/10);
                } else {
                    eb = RankCommand.buildRank(event.getGuild(), gs, event.getGuild().getMemberById(data.data));
                }

                eb.setTimestamp(Instant.now());
                event.getChannel().editMessageById(event.getMessageIdLong(),
                    eb.build())
                    .queue(m -> {
                        gs.setEmbed(m.getChannel().getIdLong(), m.getIdLong(), de);
                        m.clearReactions().queue(x -> {
                            if (data.type == 0) {
                                m.addReaction("⬅").queue();
                                m.addReaction("\uD83D\uDD04").queue();
                                m.addReaction("➡").queue();
                            } else {
                                m.addReaction("\uD83D\uDD04").queue();
                                m.addReaction("🏆").queue();
                            }
                        });
                    });
            }
        }
    }

    public static void hasChatted(MessageReceivedEvent event) {
        GuildStorage gs = GuildStorage.get(event.getGuild().getIdLong());
        long memberID = event.getAuthor().getIdLong();
        double old_score = gs.getScore(memberID);
        int old_level = GuildStorage.getLevel(old_score);
        if (old_level == GuildStorage.LEVEL_AMOUNT - 1) {
            return;
        }
        double new_score = gs.hasChatted(memberID) + old_score;
        if (new_score == old_score) {
            return;
        }
        int new_level = GuildStorage.getLevel(new_score);
        if (new_level > old_level) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Level up !");
            eb.setColor(DISCORD_COLOR);
            eb.setThumbnail("https://twemoji.maxcdn.com/36x36/2b06.png");
            Long rew = gs.getRoleReward(new_level);
            if (rew != null) {
                Role reward = event.getGuild().getRoleById(rew);
                if (reward != null) {
                    setRankRole(event.getGuild(), gs, event.getMember(), reward);
                }
            }
            eb.setDescription("Congratulations, " + event.getMember().getAsMention() + " ! You've reached level " + new_level + " !");
            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            String cmd=event.getMessage().getContentRaw();
            if (cmd.startsWith(prefix)) {
                String command=cmd.substring(prefix.length());
                String firstPart=command.split("\\s|$",2)[0];
                Command c=commands.get(firstPart);
                if (c != null) {
                    if (c.getRequiredPermissions().length > 0) {
                        List<String> missing = new LinkedList();
                        Set<Permission> existing = event.getMember().getPermissions();
                        for (Permission p:c.getRequiredPermissions()) {
                            if (!existing.contains(p)) {
                                missing.add("`"+p.getName()+"`");
                            }
                        }
                        if (missing.size() > 0) {
                            errorReply(event, "Missing permissions : " + String.join(", ", (Iterable) () -> missing.iterator()));
                            return;
                        }
                    }
                    command=command.substring(firstPart.length());
                    String[] args=new String[] {};
                    if (command.length() > 0) {
                        command = command.substring(1);
                        int terminator = command.lastIndexOf("(\\s|\n|\t)");
                        if (terminator < 0) {
                            terminator = command.length();
                        }
                        command = command.substring(0, terminator);
                    }
                    if (command.length() > 0) {
                        args=command.split("\\s+",c.getMaxArgs());
                    }
                    if (args.length < c.getMinArgs()) {
                        errorReply(event, "Too few arguments supplied : at least " + c.getMinArgs() + " expected");
                    }
                    else if (args.length > c.getMaxArgs()) {
                        errorReply(event, "Too many arguments supplied : at most " + c.getMaxArgs() + " expected");
                    }
                    else {
                        c.execute(this, event, args);
                    }
                    return;
                }
            }
            hasChatted(event);
        }
    }
}
