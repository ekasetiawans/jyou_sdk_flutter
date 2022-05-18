//package id.kakzaki.jyou_sdk;
//
//import android.app.Application;
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.Environment;
//import android.util.Log;
//
//import org.apache.http.HttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.io.UnsupportedEncodingException;
//import java.io.Writer;
//import java.lang.Thread.UncaughtExceptionHandler;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Locale;
//
///**
// * @ClassName: CrashHandler
// * @author victor_freedom (x_freedom_reddevil@126.com)
// * @createddate 2014-12-25 下午11:41:12
// * @Description: UncaughtException处理类, 当程序发生Uncaught异常的时候, 有该类来接管程序, 并记录发送错误报告.
// */
//public class CrashHandler implements UncaughtExceptionHandler {
//
//    private final String TAG = getClass().getSimpleName();
//    private static final int SYSTEM_ERROR_LEVEL_CRITICAL = 0;
//    private static final int SYSTEM_ERROR_LEVEL_WARNING = 1;
//    private static final String SYSTEM_ERROR_URI = "http://dev.keeprapid.com:8181/manage/adderrorlog";
//    // CrashHandler 实例
//    private static CrashHandler INSTANCE = new CrashHandler();
//    // 程序的 Context 对象
//    private static Context mContext;
//    private static Application app;
//
//    // 系统默认的 UncaughtException 处理类
//    private static UncaughtExceptionHandler mDefaultHandler;
//
//    // 用来存储设备信息和异常信息
//    private JSONObject infos = new JSONObject();
//
//    // 用于格式化日期,作为日志文件名的一部分
//    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
//
//    private static List<String> errorMap = new ArrayList<String>();
//
//    public static String ACTION_PREFIX = "jyClient";
//    public static String P_LOG_PATH = "/" + ACTION_PREFIX + "/log/";
//
//    /** 获取 CrashHandler 实例 ,单例模式 */
//    public static CrashHandler getInstance() {
//        return INSTANCE;
//    }
//
//    /**
//     * @Title: init
//     * @Description: 初始化
//     * @param context
//     * @param application
//     *            传入的app
//     * @throws
//     */
//    public static void init(Context context, Application application) {
//        // 传入app对象，为完美终止app
//        app = application;
//        mContext = context;
//        // 获取系统默认的 UncaughtException 处理器
//        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
//        // 设置该 CrashHandler 为程序的默认处理器
//        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
//    }
//
//    /**
//     * 当 UncaughtException 发生时会转入该函数来处理
//     */
//    @Override
//    public void uncaughtException(Thread thread, Throwable ex) {
//        if (!handleException(ex) && mDefaultHandler != null) {
//            // 如果用户没有处理则让系统默认的异常处理器来处理
//            mDefaultHandler.uncaughtException(thread, ex);
//        } else {
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            Log.i(TAG, "exit");
//            // 退出程序,注释下面的重启启动程序代码
//            app.onTerminate();
//        }
//    }
//
//
//    /**
//     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
//     *
//     * @param ex
//     * @return true：如果处理了该异常信息；否则返回 false
//     */
//    private boolean handleException(Throwable ex) {
//        if (ex == null) {
//            return false;
//        }
//        ex.printStackTrace();
//        // 收集设备参数信息
//        collectDeviceInfo(mContext, ex, SYSTEM_ERROR_LEVEL_CRITICAL);
//        // 保存日志文件
//        saveCrashInfo2File();
//        saveCrashInfo2Server();
//        return true;
//    }
//
//    /**
//     * 收集设备参数信息
//     *
//     * @param ctx
//     */
//    public void collectDeviceInfo(Context ctx, Throwable ex, int level) {
//        try {
//            PackageManager pm = ctx.getPackageManager();
//            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
//                    PackageManager.GET_ACTIVITIES);
//            if (pi != null) {
//
//              //  AuthrizeDataXmlPullParser authrizeDataXmlPullParser = new AuthrizeDataXmlPullParser(mContext);
////                HashMap<String, String> maps = authrizeDataXmlPullParser.getPersons("JySDK.xml");
////                String vid = maps.get("vid");
////                String appId = maps.get("appid");
////                String appSecret = maps.get("secret");
////
////                String versionName = pi.versionName == null ? "null" : pi.versionName;
////                String versionCode = pi.versionCode + "";
////                infos.put("app_name", vid);
////                infos.put("app_vname", versionName);
////                infos.put("app_vcode", versionCode);
//            }
//
//            String username = "";
//            infos.put("username", username);
//
//            String phone_id = "demophone";
//            infos.put("phone_id", phone_id);
//
//            String phone_os = android.os.Build.VERSION.RELEASE;
//            infos.put("phone_os", phone_os);
//            String phone_name = android.os.Build.BRAND + "-" + android.os.Build.PRODUCT;
//            infos.put("phone_name", phone_name);
//            infos.put("error_level", level);
//
//            Writer writer = new StringWriter();
//            PrintWriter printWriter = new PrintWriter(writer);
//            ex.printStackTrace(printWriter);
//            printWriter.close();
//            String detail = writer.toString();
//            infos.put("error_detail", detail);
//            infos.put("error_type",detail.split(":")[2]);
//            Log.i(TAG, "error_detail " + detail);
//        } catch (Exception e) {
//			e.printStackTrace();
//		}
//    }
//    /**
//     * 保存错误信息到文件中 *
//     *
//     * @return 返回文件名称,便于将文件传送到服务器
//     */
//    private void saveCrashInfo2File() {
//        try {
//        	StringBuffer sb = new StringBuffer();
//            Iterator<String> it = infos.keys();
//            String version = infos.getString("app_vcode");
//            while(it.hasNext()){
//            	String key = it.next();
//            	Log.e(TAG, infos.getString(key));
//                sb.append(key + "=" + infos.getString(key) + "\n");
//            }
//            String time = formatter.format(new Date());
//            String fileName =  "crash-" + version + "-error-" + time + ".txt";
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + P_LOG_PATH;
//                File dir = new File(path);
//                if (!dir.exists()) {
//                    dir.mkdirs();
//                }else{
//                	String[] files = dir.list();
//                	if(files != null){
//                        for(int i = 0; i < files.length; i++){
//                            if(files[i].matches(".*error-.*txt") && !files[i].split("-")[0].equals(version)){
//                                File file = new File(path + files[i]);
//                                Log.i(TAG,"delete:" + files[i]);
//                                file.delete();
//                            }
//                        }
//                    }
//
//                }
//                FileOutputStream fos = new FileOutputStream(path + fileName);
//                fos.write(sb.toString().getBytes());
//                fos.flush();
//                fos.close();
//            }
//        } catch (Exception e) {
//        	e.printStackTrace();
//        }
//    }
//
//    //保存错误信息到服务器
//    private void saveCrashInfo2Server(){
//    	try {
//			if(errorMap != null && errorMap.size() > 0 && errorMap.contains(stringToMD5(infos.getString("error_detail")))){
//				Log.i(TAG, "same error , not push to service");
//				return;
//			}else{
//				Log.i(TAG, "saveCrashInfo2Server else");
//			    Thread t = new Thread(runnable);
//			    t.start();
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//    }
//
//    private Runnable runnable = new Runnable(){
//        @Override
//        public void run() {
//        	String url = SYSTEM_ERROR_URI;
//        	Log.i(TAG,"save");
//        	HttpPost request = new HttpPost(url);
//        	try {
//        		Log.i(TAG,"infos=" + infos.toString());
//    	    	StringEntity se = new StringEntity(infos.toString());
//    	        errorMap.add(stringToMD5(infos.getString("error_detail")));
//    	    	se.setContentType("application/json");
//    	    	request.setEntity(se);
//                Log.i(TAG,"post to server");
//                HttpResponse rsp = new DefaultHttpClient().execute(request);
//                Log.i(TAG, rsp.getStatusLine().toString());
//    		} catch (Exception e) {
//    			e.printStackTrace();
//    		}
//        }
//    };
//
//    private static String stringToMD5(String string) {
//        byte[] hash;
//
//        try {
//            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//            return null;
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//            return null;
//        }
//
//        StringBuilder hex = new StringBuilder(hash.length * 2);
//        for (byte b : hash) {
//            if ((b & 0xFF) < 0x10)
//                hex.append("0");
//            hex.append(Integer.toHexString(b & 0xFF));
//        }
//
//        return hex.toString();
//    }
//
//
//    public void reportError(Context context, String detail, int level){
//        Throwable throwable = new Throwable(detail);
//        collectDeviceInfo(context, throwable, level);
//        saveCrashInfo2Server();
//    }
//
//}