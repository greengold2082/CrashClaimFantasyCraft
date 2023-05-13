package net.crashcraft.crashclaim.data.providers.sqlite.versions;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import net.crashcraft.crashclaim.data.providers.sqlite.DataType;
import net.crashcraft.crashclaim.data.providers.sqlite.DataVersion;

import java.sql.SQLException;
import java.util.List;

public class DataRev5 implements DataVersion {
    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public void executeUpgrade(int fromRevision) throws SQLException {
        DB.executeUpdate("PRAGMA foreign_keys = OFF"); // Turn foreign keys off

        DB.executeUpdate("ALTER TABLE \"claim_data\" ADD COLUMN\n" +
                "\t\"teleportLocation\" TEXT NULL"
        );

        DB.executeUpdate("ALTER TABLE \"claim_data\"\n" +
                "\tADD COLUMN \"createdAt\" TEXT NULL"
        );

        // RIP DEFAULT (DATETIME('now', 'localtime')), trigger then...
        DB.executeUpdate( "CREATE TRIGGER claim_data_set_createdAt AFTER INSERT\n" +
                "\tON claim_data\n" +
                "\tFOR EACH ROW BEGIN\n" +
                    "\tUPDATE claim_data\n" +
                    "\tSET createdAt = DATETIME('now', 'localtime')\n" +
                    "\tWHERE id = NEW.id;\n" +
                "\tEND;"
        );



        DB.executeUpdate("PRAGMA foreign_keys = ON");  // Undo
    }
}
