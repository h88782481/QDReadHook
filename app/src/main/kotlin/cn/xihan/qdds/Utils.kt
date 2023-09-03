package cn.xihan.qdds

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.alibaba.fastjson2.toJSONString
import com.highcapable.yukihookapi.hook.factory.MembersType
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FilenameFilter
import java.io.InputStreamReader
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Random
import kotlin.system.exitProcess


/**
 * 反射获取控件
 * @param [name] 字段名
 * @param [isSuperClass] 是超类
 * @return [T?]
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T : View> Any.getView(name: String, isSuperClass: Boolean = false): T? =
    getParam<T>(name, isSuperClass)

fun Any.getViews(vararg pairs: Pair<String, Boolean> = arrayOf("name" to false)): List<View> =
    if (pairs.isEmpty()) emptyList()
    else pairs.mapNotNull { (name, isSuperClass) -> getParam<View>(name, isSuperClass) }

/**
 * 获取控件集合
 * @param [isSuperClass] 是超类
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T : View> Any.getViews(isSuperClass: Boolean = false) =
    getParamList<T>(isSuperClass)

/**
 * 获取控件集合
 * @param [type] 类型
 * @param [isSuperClass] 是超类
 * @return [ArrayList<Any>]
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
fun Any.getViews(type: Class<*>, isSuperClass: Boolean = false): ArrayList<Any> {
    val results = arrayListOf<Any>()
    val classes =
        if (isSuperClass) generateSequence(javaClass) { it.superclass }.toList() else listOf(
            javaClass
        )
    for (clazz in classes) {
        clazz.declaredFields.filter { type.isAssignableFrom(it.type) }.forEach { field ->
            field.isAccessible = true
            val value = field.get(this)
            if (type.isInstance(value)) {
                results += value as Any
            }
        }
    }
    return results
}

/**
 * 反射获取任何类型
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T> Any.getParam(name: String, isSuperClass: Boolean = false): T? {
    val clazz = if (isSuperClass) javaClass.superclass else javaClass
    val field = clazz.getDeclaredField(name).apply { isAccessible = true }
    return field[this].safeCast<T>()
}

/**
 * 获取参数集合
 * @param [isSuperClass] 是超类
 * @return [ArrayList<T>]
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T> Any.getParamList(isSuperClass: Boolean = false): ArrayList<T> {
    val results = ArrayList<T>()
    val classes =
        if (isSuperClass) generateSequence(javaClass) { it.superclass }.toList() else listOf(
            javaClass
        )
    val type = T::class.java
    for (clazz in classes) {
        clazz.declaredFields.filter { type.isAssignableFrom(it.type) }.forEach { field ->
            field.isAccessible = true
            val value = field.get(this)
            if (type.isInstance(value)) {
                results += value as T
            }
        }
    }
    return results
}

/**
 * 获取参数 Map 集合
 */
@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T> Any.getParamMap(isSuperClass: Boolean = false): Map<String, T> {
    val results = mutableMapOf<String, T>()
    val classes =
        if (isSuperClass) generateSequence(javaClass) { it.superclass }.toList() else listOf(
            javaClass
        )
    val type = T::class.java
    for (clazz in classes) {
        clazz.declaredFields.filter { type.isAssignableFrom(it.type) }.forEach { field ->
            field.isAccessible = true
            val value = field.get(this)
            if (type.isInstance(value)) {
                results[field.name] = value as T
            }
        }
    }
    return results
}

@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T : View> Any.findViewById(id: Int): T? {
    return XposedHelpers.callMethod(this, "findViewById", id).safeCast<T>()
}

inline fun <reified T> Any?.safeCast(): T? = this as? T

fun View.setVisibilityIfNotEqual(status: Int = View.GONE) {
    this.takeIf { it.visibility != status }?.apply { visibility = status }
}

/**
 * Xposed 设置字段值
 */
fun Any.setParam(name: String, value: Any?) {
    when (value) {
        is Int -> XposedHelpers.setIntField(this, name, value)
        is Boolean -> XposedHelpers.setBooleanField(this, name, value)
        is String -> XposedHelpers.setObjectField(this, name, value)
        is Long -> XposedHelpers.setLongField(this, name, value)
        is Float -> XposedHelpers.setFloatField(this, name, value)
        is Double -> XposedHelpers.setDoubleField(this, name, value)
        is Short -> XposedHelpers.setShortField(this, name, value)
        is Byte -> XposedHelpers.setByteField(this, name, value)
        is Char -> XposedHelpers.setCharField(this, name, value)
        else -> XposedHelpers.setObjectField(this, name, value)
    }
}

/**
 * 批量 setParam
 */
fun Any.setParams(vararg params: Pair<String, Any?>) {
    params.forEach {
        setParam(it.first, it.second)
    }
}

/**
 * 利用 Reflection 获取当前的系统 Context
 */
fun getSystemContext(): Context {
    val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
    val activityThread =
        XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
    val context = XposedHelpers.callMethod(activityThread, "getSystemContext").safeCast<Context>()
    return context ?: throw Error("Failed to get system context.")
}

/**
 * 获取指定应用的 APK 路径
 */
fun Context.getApplicationApkPath(packageName: String): String {
    val pm = this.packageManager
    val apkPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.getApplicationInfo(
            packageName, PackageManager.GET_SIGNING_CERTIFICATES
        ).publicSourceDir
    } else {
        pm.getApplicationInfo(packageName, 0).publicSourceDir
    }
    return apkPath ?: throw Error("Failed to get the APK path of $packageName")
}

/**
 * 重启当前应用
 */
fun Activity.restartApplication() = packageManager.getLaunchIntentForPackage(packageName)?.let {
    finishAffinity()
    startActivity(intent)
    exitProcess(0)
}

/**
 * 获取指定应用的版本号
 */
fun Context.getVersionCode(packageName: String): Int {
    val pm = packageManager
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            pm.getPackageInfo(
                packageName, PackageManager.GET_SIGNING_CERTIFICATES
            ).longVersionCode.toInt()
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
            pm.getPackageInfo(packageName, 0).longVersionCode.toInt()
        }

        else -> {
            pm.getPackageInfo(packageName, 0).versionCode
        }
    }

}

/**
 * 打印当前调用栈
 */
fun String.printCallStack() {
    val stringBuilder = StringBuilder()
    stringBuilder.appendLine("----className: $this ----")
    stringBuilder.appendLine("Dump Stack: ---------------start----------------")
    val ex = Throwable()
    val stackElements = ex.stackTrace
    stackElements.forEachIndexed { index, stackTraceElement ->
        stringBuilder.appendLine("Dump Stack: $index: $stackTraceElement")
    }
    stringBuilder.appendLine("Dump Stack: ---------------end----------------")
    stringBuilder.toString().loge()
}

fun Any.printCallStack() {
    this.javaClass.name.printCallStack()
}

/**
 * 容错安全运行方法
 */
inline fun safeRun(block: () -> Unit) = try {
    block()
} catch (e: Throwable) {
//    if (BuildConfig.DEBUG) {
//        loggerE(msg = "safeRun 报错: ${e.message}", e = e)
//    }
}

fun String.writeTextFile(fileName: String = "test") {
    var index = 0
    while (File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/QDReader",
            "$fileName-$index.txt"
        ).exists()
    ) {
        index++
    }
    File(
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/QDReader",
        "$fileName-$index.txt"
    ).apply {
        parentFile?.mkdirs()
        if (!exists()) {
            createNewFile()
        }
    }.also {
        it.writeText(this)
    }
}

/**
 * dp 转 px
 */
fun Context.dp2px(dp: Float): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * 计算当前密度需要的偏移量
 */
fun Context.dp2pxOffset(dp: Float): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * 检查模块更新
 */
@Throws(Exception::class)
fun Context.checkModuleUpdate() {
    // 创建一个子线程
    Thread {
        Looper.prepare()
        // Java 原生网络请求
        val url = URL("https://api.github.com/repos/xihan123/QDReadHook/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            doInput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36"
            )
        }
        try {
            connection.connect()
            if (connection.responseCode == 200) {
                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                bufferedReader.forEachLine {
                    stringBuilder.append(it)
                }
                val jsonObject = JSONObject(stringBuilder.toString())
                val versionName = jsonObject.getString("tag_name")
                val downloadUrl = jsonObject.getJSONArray("assets").getJSONObject(0)
                    .getString("browser_download_url")
                val releaseNote = jsonObject.getString("body")
                if (versionName != BuildConfig.VERSION_NAME) {
                    alertDialog {
                        title = "发现新版本: $versionName"
                        message = "更新内容:\n$releaseNote"
                        positiveButton("下载更新") {
                            startActivity(Intent(Intent.ACTION_VIEW).also {
                                it.data = Uri.parse(downloadUrl)
                            })
                        }
                        negativeButton("返回") {
                            it.dismiss()
                        }
                        build()
                        show()
                    }
                } else {
                    Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            "checkModuleUpdate 报错: ${e.message}".loge()
        } finally {
            connection.disconnect()
        }
        Looper.loop()
    }.start()
}

@Throws(Exception::class)
fun Context.checkHideWelfareUpdate() {
    // 创建一个子线程
    Thread {
        Looper.prepare()
        val remoteHideWelfareList = HookEntry.optionEntity.hideBenefitsOption.remoteCHideWelfareList
        if (remoteHideWelfareList.isNotEmpty()) {
            val hideWelfareList = mutableListOf<OptionEntity.HideWelfareOption.HideWelfare>()
            remoteHideWelfareList.forEach { url ->
                // Java 原生网络请求
                val url = URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doInput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36"
                    )
                }
                try {
                    connection.connect()
                    if (connection.responseCode == 200) {
                        val inputStream = connection.inputStream
                        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                        val stringBuilder = StringBuilder()
                        bufferedReader.forEachLine {
                            stringBuilder.append(it)
                        }
                        val jsonArray = JSONArray(stringBuilder.toString())
                        if (jsonArray.length() == 0) {
                            Toast.makeText(this, "$url 当前已是最新版本", Toast.LENGTH_SHORT).show()
                        } else {
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.optJSONObject(i)
                                if (jsonObject != null) {
                                    val text = jsonObject.optString("text")
                                    val icon = jsonObject.getString("icon")
                                    val url = jsonObject.getString("url")
                                    hideWelfareList.add(
                                        OptionEntity.HideWelfareOption.HideWelfare(
                                            title = text, imageUrl = icon, actionUrl = url
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "检查隐藏福利列表失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    "checkModuleUpdate 报错: ${e.message}".loge()
                } finally {
                    connection.disconnect()
                }
            }

            if (hideWelfareList.isNotEmpty()) {
                HookEntry.optionEntity.hideBenefitsOption.hideWelfareList.addAll(hideWelfareList)
                updateOptionEntity()
                Toast.makeText(
                    this,
                    "检查隐藏福利列表成功 更新了: ${hideWelfareList.size} 个",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {
            Toast.makeText(this, "远程隐藏福利列表为空", Toast.LENGTH_SHORT).show()
        }
        Looper.loop()
    }.start()
}

/**
 * 容错的根据正则修改字符串返回字符串
 * @param enableRegex 启用正则表达式
 * @param regex 正则表达式
 * @param replacement 替换内容
 */
fun String.safeReplace(
    enableRegex: Boolean = false,
    regex: String = "",
    replacement: String = "",
): String {
    return try {
        if (enableRegex) {
            this.replace(Regex(regex), replacement)
        } else {
            this.replace(regex, replacement)
        }
    } catch (e: Exception) {
        "safeReplace 报错: ${e.message}".loge()
        this
    }
}

/**
 * 根据 ReplaceRuleOption 中 replaceRuleList 修改返回字符串
 * @param replaceRuleList 替换规则列表
 */
fun String.replaceByReplaceRuleList(replaceRuleList: List<OptionEntity.ReplaceRuleOption.ReplaceItem>): String =
    try {
        replaceRuleList.fold(this) { acc, item ->
            acc.safeReplace(item.enableRegularExpressions, item.replaceRuleRegex, item.replaceWith)
        }
    } catch (e: Exception) {
        "replaceByReplaceRuleList 报错: ${e.message}".loge()
        this
    }

/**
 *  判断输入的 十六进制颜色代码
 */
fun String.isHexColor(): Boolean {
    return try {
        Color.parseColor(this)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * 字符串复制到剪切板
 */
fun Context.copyToClipboard(text: String) {
    val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
}

/*
 *
 * 发起添加群流程。群号：模块交流群(727983520)
 * 调用 joinQQGroup 即可发起手Q客户端申请加群 模块交流群(727983520)
 *
 * @return 返回true表示呼起手Q成功，返回false表示呼起失败
 */
fun Context.joinQQGroup(key: String): Boolean {
    val intent = Intent()
    intent.data =
        Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")
    return try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        // 未安装手Q或安装的版本不支持
        Toast.makeText(this, "未安装手Q或安装的版本不支持", Toast.LENGTH_SHORT).show()
        false
    }
}

/**
 * 隐藏应用图标
 */
fun Context.hideAppIcon() {
    val componentName = ComponentName(this, MainActivity::class.java.name)
    if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/**
 * 显示应用图标
 */
fun Context.showAppIcon() {
    val componentName = ComponentName(this, MainActivity::class.java.name)
    if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/**
 * 请求权限
 */
fun Context.requestPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    if (this.applicationInfo.targetSdkVersion > 30) {
        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE, Permission.REQUEST_INSTALL_PACKAGES)
            .request { _, allGranted ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    } else {
        XXPermissions.with(this)
            .permission(Permission.Group.STORAGE.plus(Permission.REQUEST_INSTALL_PACKAGES))
            .request { _, allGranted ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }
}

/**
 * 跳转获取权限
 */
fun Context.jumpToPermission() {
    if (this.applicationInfo.targetSdkVersion > 30) {
        XXPermissions.startPermissionActivity(this, Permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        XXPermissions.startPermissionActivity(this, Permission.Group.STORAGE)
    }
}

/**
 * 判断字符串是否为数字
 */
fun String.isNumber(): Boolean = try {
    this.toDouble()
    true
} catch (e: Exception) {
    false
}

/**
 * 默认浏览器打开url
 */
fun Context.openUrl(url: String) = safeRun {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

/**
 * toast
 * @param msg String
 */
fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/**
 * 打印不支持的版本号
 */
fun String.printlnNotSupportVersion(versionCode: Int = 0) =
    "${this}不支持的版本号为: $versionCode".loge()

/**
 * 更新列表选项实体
 */
fun MutableList<OptionEntity.SelectedModel>.updateSelectedListOptionEntity(newConfigurations: List<OptionEntity.SelectedModel>): MutableList<OptionEntity.SelectedModel> {
    removeIf { oldConfig -> !newConfigurations.any { it.title == oldConfig.title } }
    addAll(newConfigurations.filter { newConfig -> !any { it.title == newConfig.title } })
    return this.sortedBy { it.title }.toMutableList()
}

fun List<OptionEntity.SelectedModel>.isSelectedByTitle(title: String): Boolean =
    firstOrNull { it.title == title }?.selected ?: false

/**
 * 解析关键词组
 */
fun parseKeyWordOption(it: String = ""): MutableSet<String> =
    it.split(";").filter(String::isNotBlank).map(String::trim).toMutableSet()

fun String.loge() {
    if (BuildConfig.DEBUG) {
        loggerE(msg = this)
    }
}

/**
 * 查找或添加列表中的数据
 * @param title 标题
 * @param iterator 迭代器
 */
fun MutableList<OptionEntity.SelectedModel>.findOrPlus(
    title: String,
    iterator: MutableIterator<Any?>,
) = safeRun {
    firstOrNull { it.title == title }?.let { config ->
        if (config.selected) {
            iterator.remove()
        }
    } ?: OptionEntity.SelectedModel(title = title).also { plusAssign(it) }
    updateOptionEntity()
}

/**
 * 查找或添加列表中的数据
 * @param title 标题
 */
fun MutableList<OptionEntity.SelectedModel>.findOrPlus(
    title: String,
    actionUnit: () -> Unit = {},
) = safeRun {
    firstOrNull { it.title == title }?.let { config ->
        if (config.selected) {
            actionUnit()
        }
    } ?: OptionEntity.SelectedModel(title = title).also { plusAssign(it) }
    updateOptionEntity()
}

/**
 * 多选项对话框
 * @param list 列表
 */
fun Context.multiChoiceSelector(
    list: List<OptionEntity.SelectedModel>,
) = safeRun {
    if (list.isEmpty()) {
        toast("没有可用的选项")
        return
    }

    val checkedItems = list.map { it.selected }.toBooleanArray()
    multiChoiceSelector(
        list.map { it.title }, checkedItems, "选项列表"
    ) { _, i, isChecked ->
        checkedItems[i] = isChecked
    }.doOnDismiss {
        checkedItems.forEachIndexed { index, b ->
            list[index].selected = b
        }
        updateOptionEntity()
    }
}

@Composable
fun <T> rememberMutableStateOf(value: T): MutableState<T> = remember { mutableStateOf(value) }

@Composable
fun Insert(list: MutableState<String>) {
    if (list.value.isNotBlank()) {
        IconButton(onClick = {
            list.value = list.value.plus(";")
        }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}

/**
 * 传入类名 打印所有方法以及参数
 * @param className 类名
 * @param printCallStack 是否打印调用栈
 * @param printResult 是否打印返回值
 * @param printType 打印参数类型
 */
fun PackageParam.findMethodAndPrint(
    className: String, printCallStack: Boolean = false, printType: MembersType = MembersType.METHOD
) {
    findClass(className).hook {
        injectMember {
            allMembers(printType)
            afterHook {
                val stringBuilder = StringBuilder().apply {
                    append("---类名: ${instanceClass.name} 方法名: ${method.name}\n")
                    if (args.isEmpty()) {
                        append("无参数\n")
                    } else {
                        args.forEachIndexed { index, any ->
                            append("参数${any?.javaClass?.simpleName} ${index}: ${any.mToString()}\n")
                        }
                    }
                    result?.let { append("---返回值: ${it.mToString()}") }
                }
                stringBuilder.toString().loge()
                if (printCallStack) {
                    instance.printCallStack()
                }
            }
        }
    }
}

fun Array<Any?>.printArgs(): String {
    val stringBuilder = StringBuilder()
    this.forEachIndexed { index, any ->
        stringBuilder.append("args[$index]: ${any.mToString()}\n")
    }
    return stringBuilder.toString()
}

/**
 * Any 基础类型转为 String
 */
fun Any?.mToString(): String = when (this) {
    is String, is Int, is Long, is Float, is Double, is Boolean -> "$this"
    is Array<*> -> this.joinToString(",")
    is ByteArray -> this.toString(Charsets.UTF_8)
    is Serializable, is Parcelable -> this.toJSONString()
    else -> {
        val list = listOf(
            "Entity",
            "Model",
            "Bean",
            "Result",
        )
        if (list.any { this?.javaClass?.name?.contains(it) == true }) this.toJSONString()
        else if (this?.javaClass?.name?.contains("QDHttpResp") == true) this.getParamList<String>()
            .toString()
        else this?.toString() ?: "null"
    }
}

/**
 * 重定向启动图路径
 */
val splashPath
    get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/QDReader/Splash/"

/**
 * 重定向主题路径
 */
val themePath get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/QDReader/ReaderTheme/"

/**
 * 随机返回一个 bitmap
 */
fun randomBitmap(): Bitmap? {
    val filter = FilenameFilter { _, name ->
        name.endsWith(".jpg", true) || name.endsWith(".png", true)
    }
    val files = File(splashPath).apply { if (!exists()) mkdirs() }
    val list = files.listFiles(filter) ?: return null
    val index = (list.indices).random()
    return try {
        BitmapFactory.decodeFile(list[index].absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 找到视图类型
 * @param [viewClass] 视图类
 * @return [List<View>]
 */
fun ViewGroup.findViewsByType(viewClass: Class<*>): ArrayList<View> {
    val result = arrayListOf<View>()
    val queue = ArrayDeque<View>()
    queue.add(this)

    while (queue.isNotEmpty()) {
        val view = queue.removeFirst()
        if (viewClass.isInstance(view)) {
            result.add(view)
        }

        if (view is ViewGroup) {
            for (i in 0..<view.childCount) {
                queue.add(view.getChildAt(i))
            }
        }
    }

    return result
}

/**
 * 生成一个随机的延迟时间。
 * @return 一个长整型值，表示延迟的毫秒数，范围在[50, 550)之间。
 */
fun randomTime(): Long = Random().nextInt(500) + 50L

/**
 * 在一个视图上延迟执行一个动作。
 * @param delayMillis 一个长整型值，表示延迟的毫秒数，默认为[randomTime]函数的返回值。
 * @param action 一个视图的扩展函数，表示要执行的动作。
 * @return 一个布尔值，表示是否成功地将动作加入到消息队列中。
 */
fun View.postRandomDelay(delayMillis: Long = randomTime(), action: View.() -> Unit) =
    postDelayed({ this.action() }, delayMillis)

/**
 * 将一个字符串数组转换为一个由字符串和布尔值组成的对数组。
 * @param default 一个布尔值，表示每个对的第二个元素的默认值，默认为false。
 * @return 一个对数组，每个对的第一个元素是原数组中的一个字符串，第二个元素是default参数的值。
 */
fun Array<String>.toPairs(default: Boolean = false): Array<Pair<String, Boolean>> =
    this.map { it to default }.toTypedArray()

/**
 * 将一个视图列表中的所有视图都设置为不可见。
 * 这个函数是一个扩展函数，可以在任何视图列表上调用。
 * 它使用了[setVisibilityIfNotEqual]函数，如果视图的可见性不等于给定的值，就设置为该值。
 */
fun List<View>.hideViews() = forEach { it.setVisibilityIfNotEqual() }

fun com.alibaba.fastjson2.JSONObject.getStringWithFallback(key: String): String? =
    this.getString(key)
        ?: this.getString(key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })

fun com.alibaba.fastjson2.JSONObject.getJSONArrayWithFallback(key: String): com.alibaba.fastjson2.JSONArray? =
    this.getJSONArray(key)
        ?: this.getJSONArray(key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
