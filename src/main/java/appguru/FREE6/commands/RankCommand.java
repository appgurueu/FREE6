package appguru.FREE6.commands;

import appguru.FREE6.chat.Bot;
import appguru.FREE6.db.EmbedData;
import appguru.FREE6.db.GuildStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static appguru.FREE6.chat.Bot.DISCORD_COLOR;

public class RankCommand extends Command {
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
        return 1;
    }

    public static EmbedBuilder buildRank(Guild g, GuildStorage gs, Member m) {
        User target = m.getUser();
        double score = gs.getScore(target.getIdLong());
        int level = GuildStorage.getLevel(score);
        double current_score = GuildStorage.LEVELS_INTEGRATED[level];
        score -= current_score;
        double score_to_next = GuildStorage.LEVELS[level];
        long rank = gs.getRank(target.getIdLong());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DISCORD_COLOR);
        eb.setThumbnail(target.getEffectiveAvatarUrl());
        eb.appendDescription(target.getAsMention() + "'s stats");
        eb.addField("Rank", "#" + (rank + 1), true);
        Long roleRewardID;
        Role role;
        if ((roleRewardID = gs.getRoleReward(level)) != null && (role = g.getRoleById(roleRewardID)) != null) {
            Bot.setRankRole(g, gs, m, role);
            eb.addField("Role", role.getAsMention(), true);
        } else {
            eb.addField("Role", "None", true);
        }
        eb.addField("Level", Integer.toString(level), true);
        eb.addField("XP", level == GuildStorage.LEVEL_AMOUNT - 1 ? "Max" : ((int)score) + "/" + ((int)score_to_next), true);
        return eb;
    }

    @Override
    public void execute(Bot b, MessageReceivedEvent e, String... args) {
        Member target;
        if (args.length == 1) {
            String id = args[0];
            if (args[0].startsWith("<@") && args[0].endsWith(">")) {
                id = args[0].substring(2, args[0].length() - 1);
            }
            if (!RoleCommand.isValidID(id) || (target = e.getGuild().getMemberById(id)) == null) {
                b.errorReply(e, "Invalid ID / mention.");
                return;
            }
        } else {
            target = e.getMember();
        }
        GuildStorage gs = GuildStorage.get(e.getGuild().getIdLong());
        EmbedBuilder eb = buildRank(e.getGuild(), gs, target);
        e.getChannel().sendMessage(eb.build()).queue(r -> {
            r.addReaction("\uD83D\uDD04").queue();
            r.addReaction("🏆").queue();
            gs.setEmbed(r.getChannel().getIdLong(), r.getIdLong(), new EmbedData((byte)1, target.getIdLong()).toBytes());
        });
    }
}
