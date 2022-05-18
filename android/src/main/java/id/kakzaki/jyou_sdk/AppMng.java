package id.kakzaki.jyou_sdk;

import android.app.Activity;
import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class AppMng extends Application {

	private String TAG = this.getClass().getSimpleName();
	private static ArrayList<Activity> activities;
	private static AppMng instance = null;
	/*
	 * 在整个应用程序创建时执行
	 */
	@Override
	public void onCreate() {
		//CrashHandler.init(getApplicationContext(), this);
		activities = new ArrayList<Activity>();
		getInstance();
		super.onCreate();
	}

	public static AppMng getInstance() {
		if (null == instance) {
			instance = new AppMng();
		}
		return instance;
	}
	
	public void addActivity(Activity activity) {
		activities.add(activity);
    }

	public void removeActivity(Activity activity) {
		activities.remove(activity);
    }
	
	public void exitApplication() {
		List<Activity> lists = AppMng.activities;
		for (Activity a : lists) {
			a.finish();
		}
		System.exit(0);
	}
}
