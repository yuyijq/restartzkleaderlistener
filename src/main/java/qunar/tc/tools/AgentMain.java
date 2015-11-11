package qunar.tc.tools;

import org.apache.zookeeper.jmx.MBeanRegistry;
import org.apache.zookeeper.jmx.ZKMBeanInfo;
import org.apache.zookeeper.server.quorum.QuorumBean;
import org.apache.zookeeper.server.quorum.QuorumCnxManager;
import org.apache.zookeeper.server.quorum.QuorumPeer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Created by zhaohui.yu
 * 15/11/11
 */
public class AgentMain {

    /**
     * 命令行启动
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
    }

    /**
     * 类加载调用
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("injecting...1");
        try {
            Field field = MBeanRegistry.class.getDeclaredField("mapBean2Path");
            field.setAccessible(true);
            Object o = field.get(MBeanRegistry.getInstance());
            Map<ZKMBeanInfo, String> map = (Map<ZKMBeanInfo, String>) o;
            for (ZKMBeanInfo info : map.keySet()) {
                if (info instanceof QuorumBean) {
                    process((QuorumBean) info);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void process(QuorumBean info) {
        try {
            Field field = QuorumBean.class.getDeclaredField("peer");
            field.setAccessible(true);
            Object o = field.get(info);
            if (o == null) return;
            if (o instanceof QuorumPeer) {
                process((QuorumPeer) o);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void process(QuorumPeer peer) {
        QuorumCnxManager quorumCnxManager = peer.getQuorumCnxManager();
        QuorumCnxManager.Listener listener = quorumCnxManager.listener;
        try {
            Field field = QuorumCnxManager.Listener.class.getDeclaredField("ss");
            field.setAccessible(true);
            Object o = field.get(listener);
            if (o == null) return;
            if (o instanceof ServerSocket) {
                process(quorumCnxManager, (ServerSocket) o);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private static void process(final QuorumCnxManager manager, final ServerSocket ss) {
        try {
            ss.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                manager.listener.run();
            }
        }, "listener-thread");
        thread.setDaemon(false);
        thread.start();

    }
}
