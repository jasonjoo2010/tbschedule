package com.taobao.pamirs.schedule;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;


/**
 * 调度处理工具类
 * @author xuannan
 *
 */
public class ScheduleUtil {
    private static final String OWN_SIGN_BASE = "BASE";

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

	public static int getFreeSocketPort() {
		try {
			ServerSocket ss = new ServerSocket(0);
			int freePort = ss.getLocalPort();
			ss.close();
			return freePort;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

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

    public static String transferDataToString(Date d) {
        FastDateFormat DATA_FORMAT_yyyyMMddHHmmss = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
        return DATA_FORMAT_yyyyMMddHHmmss.format(d);
    }

    public static Date transferStringToDate(String d) throws ParseException {
        return transferStringToDate(d, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date transferStringToDate(String d, String formatString) throws ParseException {
        FastDateFormat FORMAT = FastDateFormat.getInstance(formatString);
        return FORMAT.parse(d);
    }

    public static String getTaskTypeByBaseAndOwnSign(String baseType, String ownSign) {
        if (StringUtils.isEmpty(ownSign) || ownSign.equals(OWN_SIGN_BASE) == true) {
            return baseType;
        }
        return baseType + "$" + ownSign;
    }

    public static String splitBaseTaskTypeFromTaskType(String taskType) {
        if (taskType.indexOf("$") >= 0) {
            return taskType.substring(0, taskType.indexOf("$"));
        } else {
            return taskType;
        }

    }

    public static String splitOwnsignFromTaskType(String taskType) {
        if (taskType.indexOf("$") >= 0) {
            return taskType.substring(taskType.indexOf("$") + 1);
        } else {
            return OWN_SIGN_BASE;
        }
	}	
	
	/**
	 * 分配任务数量
	 * @param serverNum 总的服务器数量
	 * @param taskItemNum 任务项数量
	 * @param maxNumOfOneServer 每个server最大任务项数目
	 * @param maxNum 总的任务数量
	 * @return
	 */
    public static int[] assignTaskNumber(int serverNum, int taskItemNum, int maxNumOfOneServer) {
		int[] taskNums = new int[serverNum];
		int numOfSingle = taskItemNum / serverNum;
		int otherNum = taskItemNum % serverNum;
		//20150323 删除, 任务分片保证分配到所有的线程组数上。 开始
//		if (maxNumOfOneServer >0 && numOfSingle >= maxNumOfOneServer) {
//			numOfSingle = maxNumOfOneServer;
//			otherNum = 0;
//		}
		//20150323 删除, 任务分片保证分配到所有的线程组数上。 结束
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
	
    public static String getLeader(List<String> uuidList) {
        if (uuidList == null || uuidList.size() == 0) {
            return "";
        }
        long no = Long.MAX_VALUE;
        long tmpNo = -1;
        String leader = null;
        for (String server : uuidList) {
            tmpNo = NumberUtils.toLong(server.substring(server.lastIndexOf("$") + 1));
            if (no > tmpNo) {
                no = tmpNo;
                leader = server;
            }
        }
        return leader;
    }

    public static boolean isLeader(String uuid, List<String> uuidList) {
        return uuid.equals(getLeader(uuidList));
    }
}
