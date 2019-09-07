package appguru.FREE6.db;

import static appguru.FREE6.db.GuildStorage.byteArrayToLong;
import static appguru.FREE6.db.GuildStorage.longToByteArray;

public class EmbedData {
    public byte type;
    public long data;

    public EmbedData(byte type, long data) {
        this.type = type;
        this.data = data;
    }

    public EmbedData(byte[] arr) {
        this.type = arr[8];
        this.data = byteArrayToLong(arr);
    }

    public byte[] toBytes() {
        byte[] res = new byte[9];
        longToByteArray(res, data);
        res[8] = type;
        return res;
    }
}
