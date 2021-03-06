/**
 * 
 */
package by.istin.android.xcore.db.impl;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.provider.BaseColumns;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import by.istin.android.xcore.annotations.dbEntities;
import by.istin.android.xcore.annotations.dbEntity;
import by.istin.android.xcore.annotations.dbIndex;
import by.istin.android.xcore.db.IDBConnection;
import by.istin.android.xcore.db.IDBConnector;
import by.istin.android.xcore.db.entity.IBeforeArrayUpdate;
import by.istin.android.xcore.db.entity.IBeforeUpdate;
import by.istin.android.xcore.db.entity.IGenerateID;
import by.istin.android.xcore.db.entity.IMerge;
import by.istin.android.xcore.source.DataSourceRequest;
import by.istin.android.xcore.utils.BytesUtils;
import by.istin.android.xcore.utils.CursorUtils;
import by.istin.android.xcore.utils.Log;
import by.istin.android.xcore.utils.ReflectUtils;
import by.istin.android.xcore.utils.StringUtil;

/**
 * @author Uladzimir_Klyshevich
 *
 */
public class DBHelper {

    private static final String TAG = DBHelper.class.getSimpleName();

    private final IDBConnector mDbConnector;

    private final DBAssociationCache dbAssociationCache;

    public static final boolean IS_LOG_ENABLED = false;

    public DBHelper(IDBConnector dbConnector) {
        super();
        mDbConnector = dbConnector;
        dbAssociationCache = DBAssociationCache.get();
	}

	public static String getTableName(Class<?> clazz) {
        DBAssociationCache associationCache = DBAssociationCache.get();
        String tableName = associationCache.getTableName(clazz);
        if (tableName == null) {
            tableName = clazz.getCanonicalName().replace(".", "_");
            associationCache.setTableName(clazz, tableName);
        }
        return tableName;
	}

	public synchronized void createTablesForModels(Class<?>... models) {
		IDBConnection dbWriter = mDbConnector.getWritableConnection();
        dbWriter.beginTransaction();
        StringBuilder builder = new StringBuilder();
        List<String> foreignKeys = new ArrayList<String>();
        for (Class<?> classOfModel : models) {
			String table = getTableName(classOfModel);
            dbAssociationCache.setTableCreated(table, null);
			dbWriter.execSQL(mDbConnector.getCreateTableSQLTemplate(table));
            Cursor columns = null;
            try {
                columns = dbWriter.query(table, null, null, null, null, null, null, "0,1");
                List<ReflectUtils.XField> fields = ReflectUtils.getEntityKeys(classOfModel);
                for (ReflectUtils.XField field : fields) {
                    try {
                        String name = ReflectUtils.getStaticStringValue(field);
                        if (name.equals(BaseColumns._ID)) {
                            continue;
                        }
                        if (columns.getColumnIndex(name) != -1) {
                            continue;
                        }
                        Annotation[] annotations = field.getField().getAnnotations();
                        String type = null;
                        for (Annotation annotation : annotations) {
                            Class<? extends Annotation> classOfAnnotation = annotation.annotationType();
                            if (DBAssociationCache.TYPE_ASSOCIATION.containsKey(classOfAnnotation)) {
                                type = DBAssociationCache.TYPE_ASSOCIATION.get(classOfAnnotation);
                            } else if (classOfAnnotation.equals(dbEntity.class)) {
                                List<ReflectUtils.XField> list = dbAssociationCache.getEntityFields(classOfModel);
                                if (list == null) {
                                    list = new ArrayList<ReflectUtils.XField>();
                                }
                                list.add(field);
                                dbAssociationCache.putEntityFields(classOfModel, list);
                            } else if (classOfAnnotation.equals(dbEntities.class)) {
                                List<ReflectUtils.XField> list = dbAssociationCache.getEntitiesFields(classOfModel);
                                if (list == null) {
                                    list = new ArrayList<ReflectUtils.XField>();
                                }
                                list.add(field);
                                dbAssociationCache.putEntitiesFields(classOfModel, list);
                            } else if (classOfAnnotation.equals(dbIndex.class)) {
                                builder.append(mDbConnector.getCreateIndexSQLTemplate(table, name));
                            }
                        }
                        if (type == null) {
                            continue;
                        }
                        dbWriter.execSQL(mDbConnector.getCreateColumnSQLTemplate(table, name, type));
                    } catch (SQLException e) {
                        if (IS_LOG_ENABLED)
                        Log.w(TAG, e);
                    }
			    }
            } finally {
                CursorUtils.close(columns);
            }
            String sql = builder.toString();
            Log.xd(this, sql);
            if (!StringUtil.isEmpty(sql)) {
                try {
                    dbWriter.execSQL(sql);
                } catch (SQLException e) {
                    if (IS_LOG_ENABLED)
                    Log.w(TAG, e);
                }
            }
            builder.setLength(0);
		}
        setTransactionSuccessful(dbWriter);
        endTransaction(dbWriter);
    }

    public int delete(Class<?> clazz, String where, String[] whereArgs) {
		return delete(null, getTableName(clazz), where, whereArgs);
	}
	
	public int delete(IDBConnection db, Class<?> clazz, String where, String[] whereArgs) {
		return delete(db, getTableName(clazz), where, whereArgs);
	}
	
	public int delete(String tableName, String where, String[] whereArgs) {
		return delete(null, tableName, where, whereArgs);
	}
	
	public int delete(IDBConnection db, String tableName, String where, String[] whereArgs) {
		if (isExists(tableName)) {
			if (db == null) {
				db = mDbConnector.getWritableConnection();
			}
            return db.delete(tableName, where, whereArgs);
		} else {
			return 0;
		}
	}

	public boolean isExists(String tableName) {
        Boolean isTableCreated = dbAssociationCache.isTableCreated(tableName);
        if (isTableCreated != null) {
            return isTableCreated;
        }
        IDBConnection readableDb = mDbConnector.getReadableConnection();
        boolean isExists = readableDb.isExists(tableName);
        dbAssociationCache.setTableCreated(tableName, isExists);
        return isExists;

	}
	
	public int updateOrInsert(Class<?> classOfModel, ContentValues... contentValues) {
		return updateOrInsert(null, classOfModel, contentValues);
	}
	
	public int updateOrInsert(DataSourceRequest dataSourceRequest, Class<?> classOfModel, ContentValues... contentValues) {
        if (contentValues == null) {
            return 0;
        }
		IDBConnection db = mDbConnector.getWritableConnection();
		try {
            beginTransaction(db);
            int count = updateOrInsert(dataSourceRequest, classOfModel, db, contentValues);
            setTransactionSuccessful(db);
			return count;
		} finally {
            endTransaction(db);
		}
	}

    public int updateOrInsert(DataSourceRequest dataSourceRequest, Class<?> classOfModel, IDBConnection db, ContentValues[] contentValues) {
        IBeforeArrayUpdate beforeListUpdate = ReflectUtils.getInstanceInterface(classOfModel, IBeforeArrayUpdate.class);
        int count = 0;
        for (int i = 0; i < contentValues.length; i++) {
            ContentValues contentValue = contentValues[i];
            if (contentValue == null) {
                continue;
            }
            if (beforeListUpdate != null) {
                beforeListUpdate.onBeforeListUpdate(this, db, dataSourceRequest, i, contentValue);
            }
            long id = updateOrInsert(dataSourceRequest, db, classOfModel, contentValue);
            if (id != -1l) {
                count++;
            }
        }
        return count;
    }

	public long updateOrInsert(DataSourceRequest dataSourceRequest, IDBConnection db, Class<?> classOfModel, ContentValues contentValues) {
		boolean requestWithoutTransaction = false;
		if (db == null) {
			db = mDbConnector.getWritableConnection();
			requestWithoutTransaction = true;
            beginTransaction(db);
		}
		try {
			IBeforeUpdate beforeUpdate = ReflectUtils.getInstanceInterface(classOfModel, IBeforeUpdate.class);
			if (beforeUpdate != null) {
				beforeUpdate.onBeforeUpdate(this, db, dataSourceRequest, contentValues);
			}
			String idAsString = contentValues.getAsString(BaseColumns._ID);
            Long id = null;
			if (idAsString == null) {
                IGenerateID generateId = ReflectUtils.getInstanceInterface(classOfModel, IGenerateID.class);
                if (generateId != null) {
                    id = generateId.generateId(this, db, dataSourceRequest, contentValues);
                    contentValues.put(BaseColumns._ID, id);
                }
                if (id == null) {
                    Log.xd(this, "error to insert ContentValues["+classOfModel+"]: " + contentValues.toString());
                    throw new IllegalArgumentException("content values needs to contains _ID. Details: " +
                            "error to insert ContentValues["+classOfModel+"]: " + contentValues.toString());
                }
			} else {
                id = Long.valueOf(idAsString);
            }
			List<ReflectUtils.XField> listDbEntity = dbAssociationCache.getEntityFields(classOfModel);
			if (listDbEntity != null) {
				storeSubEntity(dataSourceRequest, id, classOfModel, db, contentValues, dbEntity.class, listDbEntity);
			}
			List<ReflectUtils.XField> listDbEntities = dbAssociationCache.getEntitiesFields(classOfModel);
			if (listDbEntities != null) {
				storeSubEntity(dataSourceRequest, id, classOfModel, db, contentValues, dbEntities.class, listDbEntities);
			}
			String tableName = getTableName(classOfModel);
			IMerge merge = ReflectUtils.getInstanceInterface(classOfModel, IMerge.class);
			long rowId = 0;
			if (merge == null) {
				int rowCount = db.update(tableName, contentValues, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)});
				if (rowCount == 0) {
					rowId = internalInsert(db, contentValues, tableName);
					if (rowId == -1l) {
						throw new IllegalArgumentException("can not insert content values:" + contentValues.toString() + " to table " + classOfModel+". Check keys in contentvalues and fields in model.");
					}
				} else {
					rowId = id;
				}
			} else {
                Cursor cursor = null;
                try {
				    cursor = query(classOfModel, null, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)}, null, null, null, null);
					if (cursor == null || !cursor.moveToFirst()) {
						rowId = internalInsert(db, contentValues, tableName);
						if (rowId == -1l) {
							throw new IllegalArgumentException("can not insert content values:" + contentValues.toString() + " to table " + classOfModel+". Check keys in contentvalues and fields in model.");
						}
					} else {
						ContentValues oldContentValues = new ContentValues();
                        CursorUtils.cursorRowToContentValues(classOfModel, cursor, oldContentValues);
						merge.merge(this, db, dataSourceRequest, oldContentValues, contentValues);
						if (!isContentValuesEquals(oldContentValues, contentValues)) {
							internalUpdate(db, contentValues, id, tableName);
							rowId = id;
						} else {
							rowId = -1l;
						}
					}
				} finally {
                    CursorUtils.close(cursor);
				}
			}
			if (requestWithoutTransaction) {
                setTransactionSuccessful(db);
			}
			return rowId;
		} finally {
			if (requestWithoutTransaction) {
                endTransaction(db);
			}
		}
	}

    private int internalUpdate(IDBConnection db, ContentValues contentValues, Long id, String tableName) {
        return db.update(tableName, contentValues, BaseColumns._ID + " = " + id, null);
    }

    public void endTransaction(IDBConnection dbWriter) {
        dbWriter.endTransaction();
    }

    public void setTransactionSuccessful(IDBConnection dbWriter) {
        dbWriter.setTransactionSuccessful();
    }


    public void beginTransaction(IDBConnection dbWriter) {
        dbWriter.beginTransaction();
    }

    private long internalInsert(IDBConnection db, ContentValues contentValues, String tableName) {
        return db.insert(tableName, contentValues);
    }

    public static boolean isContentValuesEquals(ContentValues oldContentValues, ContentValues contentValues) {
		Set<Entry<String, Object>> keySet = contentValues.valueSet();
        for (Entry<String, Object> entry : keySet) {
            Object newObject = entry.getValue();
            Object oldObject = oldContentValues.get(entry.getKey());
            if (newObject == null && oldObject == null) {
                continue;
            }
            if (newObject != null && newObject.equals(oldObject)) {
            } else {
                return false;
            }
        }
		return true;
	}

	private void storeSubEntity(DataSourceRequest dataSourceRequest, long id, Class<?> foreignEntity, IDBConnection db, ContentValues contentValues, Class<? extends Annotation> dbAnnotation, List<ReflectUtils.XField> listDbEntity) {
		for (ReflectUtils.XField field : listDbEntity) {
			String columnName = ReflectUtils.getStaticStringValue(field);
			byte[] entityAsByteArray = contentValues.getAsByteArray(columnName);
			if (entityAsByteArray == null) {
				continue;
			}
			Annotation annotation = ReflectUtils.getAnnotation(field, dbAnnotation);
			String contentValuesKey;
			String foreignId = getForeignKey(foreignEntity);
			try {
				contentValuesKey = (String) annotation.annotationType().getMethod("contentValuesKey").invoke(annotation);
			} catch (Exception e) {
				throw new IllegalArgumentException(e); 
			}
			String className = contentValues.getAsString(contentValuesKey);
			Class<?> modelClass;
			try {
				modelClass = Class.forName(className);
			} catch (ClassNotFoundException e1) {
				throw new IllegalArgumentException(e1);
			}
            if (annotation.annotationType().equals(dbEntity.class)) {
                ContentValues entityValues = BytesUtils.contentValuesFromByteArray(entityAsByteArray);
                putForeignIdAndClear(id, contentValuesKey, foreignId, entityValues);
                updateOrInsert(dataSourceRequest, db, modelClass, entityValues);
            } else {
                ContentValues[] entitiesValues = BytesUtils.arrayContentValuesFromByteArray(entityAsByteArray);
                for (ContentValues cv : entitiesValues) {
                    putForeignIdAndClear(id, contentValuesKey, foreignId, cv);
                }
                updateOrInsert(dataSourceRequest, modelClass, db, entitiesValues);
            }
			contentValues.remove(columnName);
			contentValues.remove(contentValuesKey);
		}
	}

    public static String getForeignKey(Class<?> foreignEntity) {
        DBAssociationCache associationCache = DBAssociationCache.get();
        String foreignKey = associationCache.getForeignKey(foreignEntity);
        if (foreignKey == null) {
            foreignKey = foreignEntity.getSimpleName().toLowerCase()+"_id";
            associationCache.putForeignKey(foreignEntity, foreignKey);
            return foreignKey;
        }
        return foreignKey;
    }

    private void putForeignIdAndClear(long id, String contentValuesKey, String foreignId, ContentValues entityValues) {
		entityValues.remove(contentValuesKey);
		entityValues.put(foreignId, id);
	}

	public Cursor query(Class<?> clazz, String[] projection,
			String selection, String[] selectionArgs, String groupBy,
			String having, String sortOrder, String limit) {
		return query(getTableName(clazz), projection, selection, selectionArgs, groupBy, having, sortOrder, limit);
	}
	
	public Cursor query(String tableName, String[] projection,
			String selection, String[] selectionArgs, String groupBy,
			String having, String sortOrder, String limit) {
		if (isExists(tableName)) {
            IDBConnection db = mDbConnector.getReadableConnection();
            return db.query(tableName, projection, selection, selectionArgs, groupBy, having, sortOrder, limit);
		} else {
			return null;
		}
	}

	public Cursor rawQuery(String sql, String[] selectionArgs) {
		IDBConnection db = mDbConnector.getReadableConnection();
        return db.rawQuery(sql, selectionArgs);
	}

    public static void moveFromOldValues(ContentValues oldValues, ContentValues newValues, String ... keys) {
        for (String key : keys) {
            Object value = oldValues.get(key);
            if (value != null && newValues.get(key) == null) {
                if (value instanceof Long) {
                    newValues.put(key, (Long)value);
                } else if (value instanceof Integer) {
                    newValues.put(key, (Integer)value);
                } else if (value instanceof String) {
                    newValues.put(key, (String)value);
                } else if (value instanceof Byte) {
                    newValues.put(key, (Byte)value);
                } else if (value instanceof byte[]) {
                    newValues.put(key, (byte[])value);
                } else if (value instanceof Boolean) {
                    newValues.put(key, (Boolean)value);
                } else if (value instanceof Double) {
                    newValues.put(key, (Double)value);
                } else if (value instanceof Float) {
                    newValues.put(key, (Float)value);
                } else if (value instanceof Short) {
                    newValues.put(key, (Short)value);
                }
            }
        }
    }

    public static ContentValues duplicateContentValues(ContentValues contentValues) {
        ContentValues values = new ContentValues();
        Set<Entry<String, Object>> entries = contentValues.valueSet();
        for (Entry<String, Object> keyValue : entries) {
            values.put(keyValue.getKey(), String.valueOf(keyValue.getValue()));
        }
        return values;
    }

    public IDBConnection getWritableDbConnection() {
        return mDbConnector.getWritableConnection();
    }
}