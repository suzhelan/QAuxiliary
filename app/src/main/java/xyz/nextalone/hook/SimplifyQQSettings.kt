/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package xyz.nextalone.hook

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import cc.ioctl.util.Reflex
import com.hicore.ReflectUtil.MField
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.util.hookMethod
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hide
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyQQSettings : MultiItemDelayableHook("na_simplify_qq_settings_multi") {

    override val preferenceTitle = "精简设置菜单"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MISC

    override val allItems = setOf(
        "手机号码",
        "达人",
        "安全",
        "模式选择",
        "通知",
        "记录",
        "隐私",
        "通用",
        "辅助",
        "免流量",
        "关于",
        "收集清单",
        "共享清单",
        "保护设置",
        "隐私政策摘要"
    )
    override val defaultItems = setOf<String>()

    override fun initOnce() = throwOrTrue {
        val clazz = arrayOf(
            Initiator._QQSettingSettingActivity(),
            Initiator._QQSettingSettingFragment()
        ).filterNotNull()
        clazz.forEach { c ->
            val m = kotlin.runCatching {
                Reflex.findSingleMethod(c, Void.TYPE, false, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE)
            }.getOrNull() ?: return@forEach
            m.hookAfter(this) {
                val thisObject = it.thisObject
                val activity = if (thisObject is Activity) {
                    thisObject
                } else {
                    thisObject.invoke("getActivity") as Activity
                }
                val viewId: Int = it.args[0].toString().toInt()
                val strId: Int = it.args[1].toString().toInt()
                val view = thisObject.invoke("findViewById", viewId, Int::class.java) as View
                val str = activity.getString(strId)
                if (activeItems.any { string -> string.isNotEmpty() && string in str }) {
                    view.hide()
                }
            }
        }
        if (activeItems.contains("免流量")) {
            // if() CUOpenCardGuideMng guideEntry
            if (requireMinQQVersion(QQVersion.QQ_8_8_93)) {
                Initiator._QQSettingSettingActivity().method("doOnCreate")?.hookAfter(this) {
                    val getId = MField.GetStaticField<Int>("com.tencent.mobileqq.R\$id".clazz, "cu_open_card_guide_entry")
                    val cu = it.thisObject.invoke("findViewById", getId, Int::class.java) as RelativeLayout
                    (cu.parent as LinearLayout).removeView(cu)
                }!!.callback.let {
                    Initiator._QQSettingSettingFragment()?.method("doOnCreateView")?.hookMethod(it)
                }
            } else {
                try {
                    Initiator._QQSettingSettingActivity().method("a", 0, Void.TYPE)?.replace(
                        this, null
                    )
                } catch (e: Throwable) {
                    Initiator._QQSettingSettingActivity().method("b", 0, Void.TYPE)?.replace(
                        this, null
                    )
                }
            }
        }
    }
}
