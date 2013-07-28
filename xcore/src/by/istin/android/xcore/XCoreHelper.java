package by.istin.android.xcore;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import by.istin.android.xcore.plugin.IXListFragmentPlugin;
import by.istin.android.xcore.utils.AppUtils;
import by.istin.android.xcore.utils.Log;

public class XCoreHelper {

    public static final String SYSTEM_SERVICE_KEY = "core:xcorehelper";

    public static XCoreHelper get(Context context) {
        return (XCoreHelper) AppUtils.get(context, SYSTEM_SERVICE_KEY);
    }

	public static interface IAppServiceKey {
		
		String getAppServiceKey();
		
	}

	private Map<String, IAppServiceKey> mAppService = new HashMap<String, IAppServiceKey>();

    private List<IXListFragmentPlugin> mListFragmentPlugins;

    public List<IXListFragmentPlugin> getListFragmentPlugins() {
        return mListFragmentPlugins;
    }

    public void addPlugin(IXListFragmentPlugin listFragmentPlugin) {
        if (mListFragmentPlugins == null) {
            mListFragmentPlugins = new ArrayList<IXListFragmentPlugin>();
        }
        mListFragmentPlugins.add(listFragmentPlugin);
    }

	public void onCreate(Context ctx) {
		ContextHolder.getInstance().setContext(ctx);
        Log.init(ctx);
	}
	
	public void registerAppService(IAppServiceKey appService) {
		mAppService.put(appService.getAppServiceKey(), appService);
	}
	
	public Object getSystemService(String name) {
        if (name.equals(SYSTEM_SERVICE_KEY)) {
            return this;
        }
		if (mAppService.containsKey(name)) {
			return mAppService.get(name);
		}
		return null;
	}
}
