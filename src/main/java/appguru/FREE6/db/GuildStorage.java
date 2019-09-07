package appguru.FREE6.db;

import appguru.FREE6.Main;
import appguru.FREE6.commands.ResetCommand;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;
import java.util.Map.Entry;

public class GuildStorage {
    public static final int LEVEL_AMOUNT = 101;
    public static double[] LEVELS = new double[LEVEL_AMOUNT];
    public static double[] LEVELS_INTEGRATED = new double[LEVEL_AMOUNT];
    static {
        // score needed to next level : 100
        LEVELS[0] = 100;
        // score needed for level 0 : 0
        LEVELS_INTEGRATED[0] = 0;
        for (int i=1; i < LEVEL_AMOUNT; i++) {
            LEVELS[i] = 5 * i * i + 50 * i + 100;
            LEVELS_INTEGRATED[i] = LEVELS_INTEGRATED[i - 1] + LEVELS[i -1];
        }
    }
    private static HashMap<Long, GuildStorage> LOOKUP = new HashMap();
    private static Jedis JEDIS;
    private static Random RANDOM = new Random();
    public HashSet<Long> hasChatted;
    private SortedMap<Integer, Long> roleRewards;
    private HashSet<Long> roleRewardIDs;
    private byte[] guildID;
    private byte[] roleRewardsID;
    public Long awaitingConfirmation;

    public static void init(String host, int port, boolean ssl, String password) {
        JEDIS = new Jedis(host, port, ssl);
        JEDIS.auth(password);
    }

    public static GuildStorage get(long guildID) {
        return LOOKUP.get(guildID);
    }
    
    static {
        Thread hasChattedCleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60 * 1000); // one min
                } catch (InterruptedException e) {
                    Main.LOGGER.info("Sleeping interrupted : ", e);
                }
                for (Entry<Long, GuildStorage> entry:LOOKUP.entrySet()) {
                    GuildStorage gs = entry.getValue();
                    synchronized (gs) {
                        gs.hasChatted = new HashSet();
                        if (gs.awaitingConfirmation != null && System.currentTimeMillis() - gs.awaitingConfirmation > 30000) {
                            gs.awaitingConfirmation = null;
                        }
                    }
                }
                System.gc();
            }
        });
        hasChattedCleaner.setName("FREE6.Cleaner");
        hasChattedCleaner.start();
    }

    public GuildStorage(long guildID) {
        this.guildID = longToByteArray(guildID);
        this.roleRewardsID = new byte[9];
        System.arraycopy(this.guildID, 0, this.roleRewardsID, 0, this.guildID.length);
        this.hasChatted = new HashSet();
        this.roleRewards = new TreeMap();
        this.roleRewardIDs = new HashSet();
        for (Tuple t : JEDIS.zrangeWithScores(roleRewardsID, Long.MIN_VALUE, Long.MAX_VALUE)) {
            long id = byteArrayToLong(t.getBinaryElement());
            roleRewards.put((int) t.getScore(), id);
            roleRewardIDs.add(id);
        }

        LOOKUP.put(guildID, this);
    }

    public static byte[] longToByteArray(long l) {
        byte[] arr = new byte[Long.BYTES];
        for (byte b=Long.BYTES-1; b >= 0; b--) {
            arr[b] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return arr;
    }

    public static void longToByteArray(byte[] arr, long l) {
        for (byte b=Long.BYTES-1; b >= 0; b--) {
            arr[b] = (byte)(l & 0xFF);
            l >>= 8;
        }
    }

    public static long byteArrayToLong(byte[] arr) {
        long l = 0;
        for (byte b=0; b < Long.BYTES; b++) {
            l <<= 8;
            l |= (arr[b] & 0xFF);
        }
        return l;
    }

    public static int getLevel(double score) {
        int insertion_point = -(Arrays.binarySearch(LEVELS_INTEGRATED, score) + 1) - 1;
        if (insertion_point < 0) {
            insertion_point = 0;
        }
        return insertion_point;
    }

    public Long count() {
        return JEDIS.zcard(this.guildID);
    }

    public Long getPages() {
        Long total = count();
        if (total == null) {
            return null;
        }
        return (total + 9) / 10;
    }

    public boolean isRoleReward(long id) {
        return roleRewardIDs.contains(id);
    }

    public Long getRoleReward(int level) {
        Iterator<byte[]> id = JEDIS.zrevrangeByScore(roleRewardsID, level, 0, 0, 1).iterator();
        if (!id.hasNext()) {
            return null;
        }
        return byteArrayToLong(id.next());
    }

    public double getNextRoleRewardScore(int level) {
        Iterator<Tuple> id = JEDIS.zrangeByScoreWithScores(roleRewardsID, level, Long.MAX_VALUE, 1, 1).iterator();
        if (!id.hasNext()) {
            return Double.MAX_VALUE;
        }
        return LEVELS_INTEGRATED[(int)id.next().getScore()];
    }

    public Iterator<byte[]> getAffectedMembers(int level) {
        return JEDIS.zrangeByScore(guildID, LEVELS_INTEGRATED[level], getNextRoleRewardScore(level)).iterator();
    }

    public Set<Tuple> listRoleRewards() {
        return JEDIS.zrangeWithScores(roleRewardsID, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public void setRoleReward(int level, long role_id) {
        roleRewards.put(level, role_id);
        roleRewardIDs.add(role_id);
        JEDIS.zremrangeByScore(roleRewardsID, level, level);
        JEDIS.zadd(roleRewardsID, level, longToByteArray(role_id));
    }

    public byte[] getEmbed(long channel_id, long embed_id) {
        byte[] map_id = new byte[this.guildID.length * 2];
        System.arraycopy(this.guildID, 0, map_id, 0, this.guildID.length);
        System.arraycopy(longToByteArray(channel_id), 0, map_id, this.guildID.length, this.guildID.length);
        byte[] id = longToByteArray(embed_id);
        byte[] l = JEDIS.hget(map_id, id);
        return l;
    }

    public void setEmbed(long channel_id, long embed_id, byte[] data) {
        byte[] map_id = new byte[this.guildID.length * 2];
        System.arraycopy(this.guildID, 0, map_id, 0, this.guildID.length);
        System.arraycopy(longToByteArray(channel_id), 0, map_id, this.guildID.length, this.guildID.length);
        byte[] id = longToByteArray(embed_id);
        JEDIS.hset(map_id, id, data);
    }
    
    public Set<byte[]> getMembers() {
        return JEDIS.zrange(guildID, 0, -1);
    }

    public Set<Tuple> getLeaderboard(long rank1, long rank2) {
        return JEDIS.zrevrangeWithScores(guildID, rank1, rank2);
    }

    public void setScore(long memberID, double score) {
        byte[] id = longToByteArray(memberID);
        JEDIS.zrem(guildID, id);
        JEDIS.zadd(guildID, score, id);
    }

    public double getScore(byte[] id) {
        Double score = JEDIS.zscore(guildID, id);
        if (score == null) {
            return 0;
        }
        return score;
    }
    
    public void removeMember(byte[] memberID) {
        JEDIS.zrem(guildID, new byte[][] {memberID});
    }

    public double getScore(long memberID) {
        return getScore(longToByteArray(memberID));
    }

    public long getRank(long memberID) {
        byte[] id = longToByteArray(memberID);
        Long rank = JEDIS.zrevrank(guildID, id);
        if (rank == null) {
            return 0;
        }
        return rank;
    }

    public void changeScore(long memberID, double change) {
        byte[] id = longToByteArray(memberID);
        JEDIS.zincrby(guildID, change, id);
    }

    public double hasChatted(long memberID) {
        if (hasChatted.contains(memberID)) {
            return 0;
        }
        double change = 15 + RANDOM.nextInt(10);
        changeScore(memberID, change);
        hasChatted.add(memberID);
        return change;
    }

    public void reset() {
        hasChatted = new HashSet();
        awaitingConfirmation = null;
        JEDIS.del(guildID);
    }

    public void close() {
        JEDIS.close();
    }
}
