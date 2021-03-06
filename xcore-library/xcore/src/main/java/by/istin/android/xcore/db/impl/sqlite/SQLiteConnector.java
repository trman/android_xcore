package by.istin.android.xcore.db.impl.sqlite;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.BaseColumns;

import by.istin.android.xcore.db.IDBConnection;
import by.istin.android.xcore.db.IDBConnector;
import by.istin.android.xcore.utils.StringUtil;

/**
 * Created with IntelliJ IDEA.
 * User: IstiN
 * Date: 18.10.13
 */
public class SQLiteConnector extends SQLiteOpenHelper implements IDBConnector {

    private static final String TAG = SQLiteConnector.class.getSimpleName();

    private static final String DATABASE_NAME_TEMPLATE = "%s.main.xcore.db";

    private static final int DATABASE_VERSION = 1;

    /** The Constant CREATE_TABLE_SQL. */
    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS  %1$s  ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY ASC)";


    public static final String CREATE_INDEX_SQL = "CREATE INDEX fk_%1$s_%2$s ON %1$s (%2$s ASC);";

    public static final String CREATE_COLUMN_SQL = "ALTER TABLE %1$s ADD %2$s %3$s;";

    public static final String FOREIGN_KEY_TEMPLATE = "ALTER TABLE %1$s ADD CONSTRAINT fk_%1$s_%2$s " +
            " FOREIGN KEY (%3$s_id) " +
            " REFERENCES %2$s(id);";

    public SQLiteConnector(Context context) {
        super(context, StringUtil.format(DATABASE_NAME_TEMPLATE, context.getPackageName()), null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    @TargetApi(value = Build.VERSION_CODES.HONEYCOMB)
    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase writableDatabase = super.getWritableDatabase();
        if (writableDatabase != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    writableDatabase.setLockingEnabled(false);
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                writableDatabase.enableWriteAheadLogging();
            }
        }
        return writableDatabase;
    }

    @Override
    @TargetApi(value = Build.VERSION_CODES.HONEYCOMB)
    public SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase readableDatabase = super.getReadableDatabase();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            readableDatabase.setLockingEnabled(false);
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            readableDatabase.enableWriteAheadLogging();
        }
        return readableDatabase;
    }

    @Override
    public IDBConnection getWritableConnection() {
        return new SQLiteConnection(getReadableDatabase());
    }

    @Override
    public IDBConnection getReadableConnection() {
        return new SQLiteConnection(getWritableDatabase());
    }

    @Override
    public String getCreateTableSQLTemplate(String table) {
        return StringUtil.format(CREATE_TABLE_SQL, table);
    }

    @Override
    public String getCreateIndexSQLTemplate(String table, String name) {
        return StringUtil.format(CREATE_INDEX_SQL, table, name);
    }

    @Override
    public String getCreateColumnSQLTemplate(String table, String name, String type) {
        return StringUtil.format(CREATE_COLUMN_SQL, table, name, type);
    }

}
