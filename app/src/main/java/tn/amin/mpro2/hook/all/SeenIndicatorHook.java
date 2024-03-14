package tn.amin.mpro2.hook.all;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tn.amin.mpro2.constants.OrcaClassNames;
import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.HookTime;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.hook.unobfuscation.OrcaUnobfuscator;
import tn.amin.mpro2.orca.OrcaGateway;

public class SeenIndicatorHook extends BaseHook {
    @Override
    public HookId getId() {
        return HookId.SEEN_INDICATOR_SEND;
    }

    @Override
    public HookTime getHookTime() {
        return HookTime.AFTER_DEOBFUSCATION;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(OrcaGateway gateway) {
        final Class<?> MailboxSDKJNI = XposedHelpers.findClass(OrcaClassNames.MAILBOX_SDK_JNI, gateway.classLoader);
        var wrapped = wrap(new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Integer apiCode = gateway.unobfuscator.getAPICode(OrcaUnobfuscator.API_MESSAGE_SEEN);

                // If api code is specified manually by user
                if (apiCode >= 0) {
                    if (apiCode == param.args[0]) {

                        // Inside seen indicator dispatch
                        notifyListenersWithResult((listener) -> ((SeenIndicatorListener) listener).onSeenIndicator());
                        boolean allowSeen = !getListenersReturnValue().isConsumed || (Boolean) getListenersReturnValue().value;
                        if (!allowSeen) {
                            param.setResult(null);
                        }
                    }
                }

                // Fallback method
                else if (
                        (param.args[1].getClass().getName().equals(OrcaClassNames.MAILBOX) ||
                                param.args[2].getClass().getName().equals(OrcaClassNames.MAILBOX)
                ) &&
                        (param.args[3] == null || param.args[3].getClass().getName().equals(Long.class.getName()))
                ) {

                    Logger.verbose("Inside seen indicator dispatch");
                    // Inside seen indicator dispatch
                    notifyListenersWithResult((listener) -> ((SeenIndicatorListener) listener).onSeenIndicator());
                    boolean allowSeen = !getListenersReturnValue().isConsumed || (Boolean) getListenersReturnValue().value;
                    Logger.verbose("AllowSeen: " + allowSeen);
                    if (!allowSeen) {
                        param.setResult(null);
                    }
                }
            }
        });

        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        unhooks.addAll(XposedBridge.hookAllMethods(MailboxSDKJNI, "dispatchVOOOOO", wrapped)); // old
        unhooks.addAll(XposedBridge.hookAllMethods(MailboxSDKJNI, "dispatchVJOOOO", wrapped));
        return unhooks;
    }

    public interface SeenIndicatorListener {
        HookListenerResult<Boolean> onSeenIndicator();
    }
}
