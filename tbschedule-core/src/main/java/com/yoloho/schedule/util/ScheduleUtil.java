package com.yoloho.schedule.util;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Schedule utilities
 * 
 * @author xuannan
 * @author jason
 *
 */
public class ScheduleUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleUtil.class.getSimpleName());
    private static final String OWN_SIGN_BASE = "BASE";

	/**
	 * Get hostname
	 * 
	 * @return
	 */
	public static String getLocalHostName() {
		try {
		    InetAddress addr = getRealAddr();
		    if (addr != null && !addr.getHostAddress().equals(addr.getHostName())) {
		        return addr.getHostName();
		    }
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Get local address
	 * 
	 * @return
	 */
	public static String getLocalIP() {
		try {
		    InetAddress addr = getRealAddr();
		    if (addr != null) {
		        return addr.getHostAddress();
		    }
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "";
		}
    }

    /**
     * Format date into "yyyy-MM-dd HH:mm:ss"
     * 
     * @param d
     * @return
     */
    public static String dataToString(Date d) {
        FastDateFormat yyyyMMddHHmmss = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
        return yyyyMMddHHmmss.format(d);
    }

    /**
     * Generate running entry name from task name and ownsign
     * 
     * @param taskName
     * @param ownSign
     * @return
     */
    public static String runningEntryFromTaskName(String taskName, String ownSign) {
        if (StringUtils.isEmpty(ownSign) || ownSign.equals(OWN_SIGN_BASE) == true) {
            return taskName;
        }
        return taskName + "$" + ownSign;
    }

    /**
     * Fetch task name from running entry name
     * 
     * @param runningEntry
     * @return
     */
    public static String taskNameFromRunningEntry(String runningEntry) {
        if (runningEntry.indexOf("$") >= 0) {
            return runningEntry.substring(0, runningEntry.indexOf("$"));
        } else {
            return runningEntry;
        }

    }

    /**
     * Fetch ownsign from running entry name
     * 
     * @param runningEntry
     * @return
     */
    public static String ownsignFromRunningEntry(String runningEntry) {
        if (runningEntry.indexOf("$") >= 0) {
            return runningEntry.substring(runningEntry.indexOf("$") + 1);
        } else {
            return OWN_SIGN_BASE;
        }
	}	
	
	/**
	 * Generate a items count distribution sequence
	 * 
	 * @param nodeCount Node count
	 * @param jobCount Job count (eg. task item's count)
	 * @param nodeCapacity 0 for no limit on single node
	 * @return
	 */
    public static int[] generateSequence(int nodeCount, int jobCount, int nodeCapacity) {
        int[] taskNums = new int[nodeCount];
        int numOfSingle = jobCount / nodeCount;
        int otherNum = jobCount % nodeCount;
        if (nodeCapacity > 0 && numOfSingle >= nodeCapacity) {
            logger.error("Generation exceeds limit, please check alived nodes, current nodes {}", nodeCount);
            numOfSingle = nodeCapacity;
            otherNum = 0;
        }
        for (int i = 0; i < taskNums.length; i++) {
            if (i < otherNum) {
                taskNums[i] = numOfSingle + 1;
            } else {
                taskNums[i] = numOfSingle;
            }
        }
		return taskNums;
	}
    
	/**
	 * Get the local ip address, needed by uuid generating
	 * 
	 * XXX 这里的实现依然有问题，多有效端口时会有问题，暂先排个序，优先取有效的lan地址
	 * XXX 另外考虑这里是否加入缓存
	 * @return
	 */
	private static InetAddress getRealAddr() {
	    try {
	        //备选地址
	        List<InetAddress> addrs = new ArrayList<InetAddress>();
	        // 遍历所有的网络接口
	        for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
	            NetworkInterface interFace = (NetworkInterface) interfaces.nextElement();
	            // 在所有的接口下再遍历IP
	            for (InterfaceAddress interfaceAddr : interFace.getInterfaceAddresses()) {
	                InetAddress addr = interfaceAddr.getAddress();
	                if (addr.isLoopbackAddress()) continue;
	                if (addr.isMulticastAddress()) continue;
	                if (addr.isLinkLocalAddress()) continue;
	                if (addr.getHostAddress().indexOf(":") >= 0) {
	                    //ipv6
	                    if (interfaceAddr.getNetworkPrefixLength() == 64) {
                            continue;
                        }
	                } else {
	                    //ipv4
	                    if (interfaceAddr.getNetworkPrefixLength() == 32) {
	                        continue;
	                    }
	                }
	                if (addr.getHostAddress().startsWith("192.168.") || addr.getHostAddress().startsWith("172.16.") || addr.getHostAddress().startsWith("10.")) {
	                    // 暂优先lan地址
	                    return addr;
	                }
	                addrs.add(addr);
	            }
	        }
	        if (addrs.size() > 0) {
	            return addrs.get(0);
	        }
	        // 如果没有发现 non-loopback地址.只能用最次选的方案
	        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
	        return jdkSuppliedAddress;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
    /**
     * Get the leader's uuid from a set of server's uuid
     * 
     * @param serverUuidList
     * @return
     */
    public static String getLeader(Collection<String> serverUuidList) {
        if (serverUuidList == null || serverUuidList.size() == 0) {
            return "";
        }
        long no = Long.MAX_VALUE;
        long tmpNo = -1;
        String leader = null;
        for (String server : serverUuidList) {
            tmpNo = NumberUtils.toLong(server.substring(server.lastIndexOf("$") + 1));
            if (no > tmpNo) {
                no = tmpNo;
                leader = server;
            }
        }
        return leader;
    }

    /**
     * Whether the specified server is the leader among the servers
     * 
     * @param serverUuid
     * @param serverUuidList
     * @return
     */
    public static boolean isLeader(String serverUuid, List<String> serverUuidList) {
        return serverUuid.equals(getLeader(serverUuidList));
    }
}
