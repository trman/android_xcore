package by.istin.android.xcore.test.db;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.ApplicationTestCase;
import by.istin.android.xcore.db.impl.DBHelper;
import by.istin.android.xcore.db.IDBConnector;
import by.istin.android.xcore.db.impl.sqlite.SQLiteSupport;
import by.istin.android.xcore.model.BigTestEntity;
import by.istin.android.xcore.model.BigTestSubEntity;
import by.istin.android.xcore.utils.CursorUtils;

public class TestDbHelper extends ApplicationTestCase<Application> {

    private DBHelper dbHelper;

    public TestDbHelper() {
		super(Application.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		createApplication();
        IDBConnector connector = new SQLiteSupport().createConnector(getApplication());
        dbHelper = new DBHelper(connector);
	}

    public void testInsert() throws Exception {
        ContentValues contentValues = MockStorage.generateSingleEntity(0);

        createAndClearTables();

        dbHelper.updateOrInsert(BigTestEntity.class, contentValues);

        checkResults(1);
    }

    public void testDelete() throws Exception {
        testBulkInsert();

        dbHelper.delete(BigTestEntity.class, null, null);
        dbHelper.delete(BigTestSubEntity.class, null, null);

        checkResults(0);
    }

    public void testDeleteWithCondition() throws Exception {
        testBulkInsert();

        dbHelper.delete(BigTestEntity.class, BigTestEntity.ID + "= ?", new String[]{"0"});
        dbHelper.delete(BigTestSubEntity.class, BigTestSubEntity.ID + "= ?", new String[]{"0"});

        checkResults(MockStorage.SIZE-1);
    }

	public void testBulkInsert() throws Exception {
        ContentValues[] contentValues = MockStorage.generateArray();
        createAndClearTables();

		dbHelper.updateOrInsert(BigTestEntity.class, contentValues);

        checkResults(MockStorage.SIZE);
	}

    private void checkResults(int count) {
        Cursor cursor = dbHelper.query(BigTestSubEntity.class, new String[]{BigTestSubEntity.ID}, null, null, null, null, null, null);
        if (count == 0) {
            assertTrue(CursorUtils.isEmpty(cursor));
        } else {
            assertEquals(count, cursor.getCount());
        }
        CursorUtils.close(cursor);

        cursor = dbHelper.query(BigTestEntity.class, new String[]{BigTestEntity.ID}, null, null, null, null, null, null);
        if (count == 0) {
            assertTrue(CursorUtils.isEmpty(cursor));
        } else {
            assertEquals(count, cursor.getCount());
        }
        CursorUtils.close(cursor);
    }

    private void createAndClearTables() {
        dbHelper.createTablesForModels(BigTestSubEntity.class, BigTestEntity.class);
        dbHelper.delete(BigTestSubEntity.class, null, null);
        dbHelper.delete(BigTestEntity.class, null, null);
    }

}
