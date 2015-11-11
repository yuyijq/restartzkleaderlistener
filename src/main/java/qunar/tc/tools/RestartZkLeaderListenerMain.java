package qunar.tc.tools;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;
import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by zhaohui.yu
 * 15/11/11
 */
public class RestartZkLeaderListenerMain {
    private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String type() {
            return null;
        }

        @Override
        public VirtualMachine attachVirtualMachine(String id) {
            return null;
        }

        @Override
        public List<VirtualMachineDescriptor> listVirtualMachines() {
            return null;
        }
    };

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {

        String agentPath = AgentMain.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String pid = args[0];
        System.out.println("agent=" + agentPath);
        System.out.println("pid=" + pid);

        VirtualMachine vm = null;

        if (AttachProvider.providers().isEmpty()) {
            String vmName = System.getProperty("java.vm.name");

            if (vmName.contains("HotSpot")) {
                vm = getVirtualMachineImplementationFromEmbeddedOnes(pid);
            } else {
                String helpMessage = getHelpMessageForNonHotSpotVM(vmName);
                throw new IllegalStateException(helpMessage);
            }
        } else {
            vm = attachToRunningVM(pid);
        }

        loadAgentAndDetachFromRunningVM(vm, agentPath);

        System.in.read();
    }

    private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes(String pid) {
        Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
        Class<?>[] parameterTypes = {AttachProvider.class, String.class};

        try {
            // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
            Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
            VirtualMachine newVM = vmConstructor.newInstance(ATTACH_PROVIDER, pid);
            return newVM;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        }
    }

    private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS() {
        if (File.separatorChar == '\\') {
            return WindowsVirtualMachine.class;
        }

        String osName = System.getProperty("os.name");

        if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
            return LinuxVirtualMachine.class;
        } else if (osName.startsWith("Mac OS X")) {
            return BsdVirtualMachine.class;
        } else if (osName.startsWith("Solaris")) {
            return SolarisVirtualMachine.class;
        }

        throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
    }

    private static String getHelpMessageForNonHotSpotVM(String vmName) {
        String helpMessage = "To run on " + vmName;

        if (vmName.contains("J9")) {
            helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
        }

        return helpMessage + " use -javaagent:";
    }

    private static VirtualMachine attachToRunningVM(String pid) {
        try {
            return VirtualMachine.attach(pid);
        } catch (AttachNotSupportedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadAgentAndDetachFromRunningVM(VirtualMachine vm, String jarFilePath) {
        if (vm == null) return;
        try {
            vm.loadAgent(jarFilePath, null);
        } catch (AgentLoadException e) {
            throw new IllegalStateException(e);
        } catch (AgentInitializationException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                vm.detach();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
